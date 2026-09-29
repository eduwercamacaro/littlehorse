# Put Variables

**Author:** Eduwer Camacaro

**Status:** Draft

**GitHub Issue:** TODO

## Motivation

Applications and operators sometimes need to modify a variable of an existing `WfRun` without changing its `WfSpec`. Examples include correcting input data, adjusting a pending sleep, and repairing state before rescuing a failed thread.

Today, a workflow can model these changes through an `ExternalEvent` and a handler that mutates the variable. This requires the workflow author to anticipate each update and express the corresponding behavior in the specification.

This proposal introduces `PutVariable`, an RPC that replaces the value of an existing `Variable` and attempts to advance the owning workflow. It returns the previous value so that the caller can observe what was replaced. External events remain appropriate when an update needs business validation or a reaction explicitly modeled in the `WfSpec`.

## Scope

The initial implementation supports whole-value replacement of one existing variable, identified by its exact `VariableId`. It does not create variables, change their declarations, or modify the `WfSpec`.

The prototype deliberately omits declared-type validation, lifecycle restrictions, conditional writes, and request deduplication. These limitations are recorded under [Open Questions](#open-questions); they should not be interpreted as settled production policy.

Batch updates, nested-field patches, arithmetic mutation operations, a variable revision field, and a dedicated mutation-history API are outside the initial scope.

## Public Contract

### Protobuf

Add the following request and RPC to `service.proto`:

```protobuf
message PutVariableRequest {
  // Exact ID of the existing variable to modify.
  VariableId id = 1;

  // Replacement value. An explicitly present, empty VariableValue represents null.
  VariableValue value = 2;
}

// Replaces the value of an existing Variable and attempts to advance its workflow.
// Returns the previous value, respecting the variable's masking configuration.
rpc PutVariable(PutVariableRequest) returns (VariableValue) {}
```

The existing `VariableId` identifies the owning `WfRun`, thread run number, and variable name. The RPC performs an exact lookup; it does not resolve the name through parent-thread or inherited-variable lookup. To update shared state, callers must address the variable where it is stored.

Both `id` and `value` must be present. The workflow ID and variable name must be nonempty, and the thread run number must be nonnegative. Omitting `value` is an error; sending an empty `VariableValue` explicitly assigns null.

The response is a snapshot of the previous value captured before replacement and before workflow advancement. For a masked variable, it contains the existing string mask rather than the previous secret. A previous null value is returned as an empty `VariableValue`.

### Errors and Authorization

| Condition | Result |
| --- | --- |
| Missing required fields or malformed identifiers described above | `INVALID_ARGUMENT` |
| No variable exists for the exact ID | `NOT_FOUND` |
| Caller lacks authorization | Rejected by the existing authorization layer |
| Replacement succeeds | Previous `VariableValue` |

The prototype uses `ACL_WORKFLOW` with the `RUN` action, consistent with other workflow execution commands, and the existing tenant context. Whether external variable writes require a separate permission remains open. `PUBLIC_VAR` and `PRIVATE_VAR` describe access from child workflows; they do not establish an external-write permission.

### SDKs and CLI

Regenerate the protobuf clients to expose `PutVariable`. No workflow-builder API changes are required. For example, a Java caller can use:

```java
VariableValue previousValue = client.putVariable(PutVariableRequest.newBuilder()
        .setId(VariableId.newBuilder()
                .setWfRunId(wfRunId)
                .setThreadRunNumber(0)
                .setName("duration"))
        .setValue(VariableValue.newBuilder().setInt(120))
        .build());
```

Add `lhctl put variable` using the existing CLI value-parsing conventions:

```bash
lhctl put variable <wfRunId> <threadRunNumber> <varName> [(<varType> <payload>)]

lhctl put variable <wfRunId> 0 duration INT 120
lhctl put variable <wfRunId> 0 message STR "updated"
lhctl put variable <wfRunId> 0 data JSON_OBJ '{"enabled":true}'

# Omit both type and payload to assign null.
lhctl put variable <wfRunId> 0 message
```

The command prints the previous value returned by the server. It supports the existing primitive value parser, including `WF_RUN_ID` and `TIMESTAMP`; it does not introduce CLI builders for complex typed values.

## Server Implementation

### Command Processing

Add a subcommand to the internal `Command.command` oneof:

```protobuf
PutVariableRequest put_variable = 39;
```

`LHServerListener` routes the request through the normal command pipeline. `PutVariableRequestModel` uses the variable's workflow partition key so the update is processed with the workflow's other commands.

The subcommand:

1. Loads the variable using its exact ID and rejects a missing variable.
2. Captures an immutable protobuf snapshot of its previous value, applying masking for the response.
3. Replaces its value and saves it through `GetableManager`.
4. Loads the owning `WfRun` and calls `advance()` with the current command's timestamp.
5. Returns the captured previous value through the normal command-response path.

The variable keeps its ID, creation time, specification reference, and masking configuration. Using the existing persistence path preserves the mechanisms for updating indexes and publishing eligible changes to the output topic.

### Execution Semantics

The command's processing order determines whether a competing task completion, event, or variable update takes effect before or after this replacement. This orders server-side operations, but does not provide protection against a caller writing from stale information. A subsequent workflow mutation may overwrite the value set by the RPC.

Advancement lets active nodes reevaluate their existing conditions and variable-dependent behavior. It does not clear halt reasons, rescue a failed thread, or reopen completed nodes. A successful RPC does not guarantee that the workflow progresses or completes successfully: evaluating the updated state may leave it waiting or produce a workflow failure.

Previously executed branches and external side effects are not undone. Inputs already stored in a `TaskRun`, including inputs reused by its retries, are not rewritten. Future evaluations can read the new value. For example, a task may finish using an input of `100`, while the next branch reads the updated variable value of `200`.

Only the owning `WfRun` is explicitly advanced. Other workflows that inherit the variable do not receive an automatic advancement notification in this prototype.

### Rescheduling Sleep Nodes

Relative sleeps must use a stable reference time when reevaluated. Calculate their deadline as:

```text
maturationTime = NodeRun.arrivalTime + durationSeconds * 1000
```

Use this calculation both when the sleep node is entered and whenever it is checked during advancement. Remove the exclusion of `RAW_SECONDS` from `maybeRescheduleMaturation()`. Absolute timestamp and ISO date sleeps continue to resolve their deadlines from their assignments.

If the newly evaluated deadline equals the stored deadline, no timer is added. Otherwise, update the stored maturation time and schedule a new timer. This supports expressions depending on multiple variables without tracking individual variable revisions.

For a sleep entered at `12:00:00` with an initial duration of 60 seconds, an update at `12:00:20` has these effects:

| New duration | Deadline | Effect |
| --- | --- | --- |
| 120 seconds | `12:02:00` | Extends the total sleep duration |
| 10 seconds | `12:00:10` | Schedules a timer already eligible to fire |
| 60 seconds | `12:01:00` | Leaves the deadline unchanged |

Changing the duration adjusts the total sleep length measured from node arrival; it does not restart the countdown. Repeated advancement or assigning the same duration does not move the deadline forward. A node already marked as matured is not rescheduled.

Rescheduling adds a timer rather than cancelling the old one. Existing handling ignores timer commands for previous node positions and timer commands whose timestamp precedes the updated maturation time. A deadline in the past still completes through timer-command processing, rather than immediately setting the node's matured flag in `PutVariable`.

### Operational Impact

Each update adds a core command, a variable write, and a workflow advancement attempt. Advancement may evaluate several active threads or execute additional workflow transitions. Updating searchable or published variables also uses the existing index and output-topic mechanisms.

Repeated deadline changes can leave multiple timers pending until they fire and are ignored. Load testing should cover frequent updates to long-lived workflows and sleeping nodes, including the cost of retaining obsolete timers.

## Compatibility

The RPC and request are additive public API changes. Existing clients remain usable; callers need an updated client and server to use `PutVariable`. An older server does not implement the new RPC. All servers that may consume the new internal command must understand it before clients begin sending updates.

No variable revision or new persisted sleep field is required. Existing `NodeRun.arrivalTime` and `SleepNodeRun.maturationTime` supply the necessary state.

There is a behavioral change for relative sleeps: a variable mutation followed by advancement can now adjust an active sleep's deadline. This also applies to mutations performed by the workflow itself, such as an interrupt handler, not just `PutVariable`. Existing sleeps are reevaluated against their persisted arrival time. Rollout must account for mixed server versions applying different relative-sleep behavior.

## Test Plan

The prototype includes unit and E2E coverage for replacement, previous-value responses, masking, null assignment, missing variables, malformed requests, subsequent workflow reads, and relative-sleep shortening and extension. Unit coverage checks the relative deadline against a fixed arrival time.

Before completing the feature, verify:

* Repeated advancement and same-value assignments preserve the deadline and do not schedule duplicate timers.
* Absolute sleeps continue to reschedule, and stale timers cannot complete an extended sleep early.
* Concurrent task completions and RPC writes follow command order; existing task inputs remain unchanged.
* Exact ownership works for child threads and parent/child workflows, including tenant and authorization boundaries.
* Search indexes remove old values and include new values, and output-topic behavior preserves masking and visibility rules.
* Restart and state restoration preserve the updated value and sleep deadline.
* The lifecycle and validation policies selected below are enforced consistently.

## Alternatives Considered

### External Events

An external event handler can validate incoming data and apply mutations defined by the workflow author. This remains useful for business operations, but requires a matching handler in the specification. `PutVariable` provides a direct update path for an existing variable without adding that handler.

### Variable Revisions

A revision incremented on each assignment could let consumers detect writes and could later support conditional updates. For relative sleep reevaluation, a stable node arrival time and comparison of the resulting deadline are sufficient. A revision would also require all mutation paths to maintain it and consumers to track the revisions of their dependencies. This proposal does not add one.

## Open Questions

* **Type validation:** The prototype replaces values without checking the declared type. Should the production RPC reuse ingress validation for primitives, structs, collections, and nulls? Should it permit coercion? Invalid state can otherwise fail later during workflow execution or indexing.
* **Lifecycle:** The prototype has no status restriction. Which combinations of workflow and owning-thread status should permit updates, including halted, failed, completed, and archived threads? How should migration affect validation against declarations?
* **Authorization and opt-in:** Is `ACL_WORKFLOW/RUN` sufficient, or should this operation require a distinct permission or an externally writable declaration? Returning a previous value also grants visibility into unmasked state.
* **Concurrency and retries:** Should callers supply an expected revision and a request ID? The prototype has neither. Retrying after an ambiguous timeout can overwrite an intervening mutation and return a different previous value.
* **Business invariants:** How should callers update related values atomically when they must change together? The initial RPC updates only one variable and immediately attempts advancement.
* **Auditability:** Should a durable, user-accessible record capture the actor, reason, target, and outcome of each update? The command pipeline and output topic do not by themselves define such an API or its retention policy.
* **Inherited consumers:** Should updating a shared variable explicitly advance child workflows that inherit it, or should they observe it only on their next existing advancement trigger?
