# Inline Workflow Runs

**Author:** Eduwer Camacaro

## Context

Today, executing a task method through LittleHorse requires a registered `TaskDef` and a registered `WfSpec` that invokes it. 
This is foundational to LittleHorse and works well for reusable business processes.
However, some executions require orchestration without requiring clients to manage, distribute, and version a separate workflow definition. 
Making `WfSpec` registration mandatory adds friction in cases where developers want to invoke a task method with specific inputs and inspect its results while debugging.

This proposal aims to allow clients to define and run workflows without registering a WfSpec.

## Server API Changes


### `WfRun`

The key change is to add a spec source for WfRuns. The source could be either a registered `WfSpec` or an inline `WfSpec` that was provided at invocation time.

```protobuf
message WfRun {
  WfRunId id = 1;

  oneof wf_spec_source {
    WfSpecId wf_spec_id = 2;

    InlineWfSpecId inline_wf_spec_id = 15;
  }

  // Other existing fields are unchanged.
}
```

### `InlineWfSpec`


```protobuf
message InlineWfSpec {
  // Identity for the one-off workflow
  InlineWfSpecId id = 1;

  google.protobuf.Timestamp created_at = 2;

  map<string, ThreadSpec> thread_specs = 3;
  string entrypoint_thread_name = 4;
  // Optional retention policy to clean up the resuling WfRun
  optional WorkflowRetentionPolicy retention_policy = 5;
}
```

The `InlineWfSpec` has a similar structure to a `WfSpec` and uses the same thread graph, entrypoint, and metadata references

The server validates the thread graph, entrypoint, metadata references, and input variables before accepting the run just like any other `WfSpec`

### `InlineWfSpecId`

Defines both identity and partition key for the inline-spec. The `InlineWfSpec` is co-partitioned with the owning `WfRun` record.

```protobuf
message InlineWfSpecId {
  WfRunId wf_run_id = 1;
}
```

### `rpc RunInlineWf`

Creates a new `WfRun` from an InlineWfSpec. Clients could also provide an ID for the run similar to the `rpc RunWf`.

```protobuf
message RunInlineWfRequest {
  InlineWfSpec wf_spec = 1;

  // Inputs to the entrypoint ThreadRun.
  map<string, VariableValue> variables = 2;

  // Optional caller-provided WfRun ID.
  optional string id = 3;
}

service LittleHorse {
  
  rpc RunInlineWf(RunInlineWfRequest) returns (WfRun) {}
  
}
```

Clients can optionally provide variables to the entrypoint thread, or alternatively, they can use literal value on the inline spec.

### `GetInlineWfSpec`

Clients can retrive the InlineWfSpec for a given WfRun.
```protobuf
service LittleHorse {
  // Existing RPCs omitted.
  rpc GetInlineWfSpec(InlineWfSpecId) returns (InlineWfSpec) {}
}
```
