# Inline workflow prototype

`RunInlineWf` validates an inline definition and starts a normal durable `WfRun`
without registering a `WfSpec`. Task execution still uses `ThreadRun`, `NodeRun`,
and `TaskRun`; this is ephemeral orchestration, not standalone task invocation.
The server exposes the inline RPCs; `lhctl run tasks` builds a single-task inline
workflow. There are no dedicated inline SDK builders or dashboard features.

## API

`RunInlineWfRequest` contains `wf_spec` (`InlineWfSpec`), `variables`, and
an optional `id`. A definition contains `thread_specs`, `entrypoint_thread_name`,
and an optional `retention_policy`. Existing TaskDefs and other referenced metadata
must already be registered. The caller needs both workflow `RUN` and
`WRITE_METADATA` permissions.

`InlineWfSpec` is used for both submission and retrieval. Its `thread_specs`,
`entrypoint_thread_name`, and `retention_policy` fields are directly on the message.
The server rejects caller-supplied `id` and `created_at`; it generates them after
validation. There is no checksum or definition-level comparison.

The returned `WfRun` has `inline_wf_spec_id` instead of `wf_spec_id`. The ID contains
the owning `wf_run_id`, which determines routing and lifecycle. Fetch the snapshot
with `GetInlineWfSpec(InlineWfSpecId)` (workflow `READ` permission). It contains the
validated definition, its ID, and creation time.
There is no independent create/update/delete API for inline definitions.
Reusing a run ID returns `ALREADY_EXISTS`, matching `RunWf`; it does not compare
definitions or return an earlier response.

For example, from the repository root, an empty workflow can be submitted with:

```sh
grpcurl -plaintext -import-path schemas/littlehorse -proto service.proto \
  -d '{"id":"inline-example","wfSpec":{"entrypointThreadName":"main","threadSpecs":{"main":{"nodes":{"start":{"entrypoint":{},"outgoingEdges":[{"sinkNodeName":"done"}]},"done":{"exit":{}}}}}}}' \
  localhost:2023 littlehorse.LittleHorse/RunInlineWf
```

## Execution and storage

- Validation runs on the core command path using the existing WfSpec validators
  and read-only metadata access. Validation can normalize metadata references;
  the resulting definition is what is persisted.
- The definition is a separate core-store record in the same partition as its
  WfRun, staged in the same core transaction when the run is created. A transient
  `WfSpecModel` view lets the existing execution engine use it without inventing
  or registering a WfSpec ID. Subsequent commands resolve it through the stored
  run's ID reference. WfRun updates no longer serialize the full definition.
- Inline ThreadRuns, NodeRuns, and Variables omit `wf_spec_id`. Consumers must
  resolve their definition through the owning WfRun instead of assuming that
  every run has a registered spec.
- Existing task retries, external events, child threads, failure handlers, sleep
  timers, run deletion, and workflow retention use the normal execution paths.
  Retention deletes the definition with the run. Resumable deletion keeps the
  definition until node, variable, and event cleanup finishes, then deletes both
  the definition and WfRun in the final cleanup command.
- Task execution retains its existing delivery semantics.

## Deliberate prototype boundaries

- Definitions are limited to 256 KiB, 256 total nodes, and 256 threads. The byte
  limit is checked both before and after validation.
- Workflow migration is rejected. Inline workflows cannot invoke/wait for child
  workflows, or act as parents of separately invoked registered workflows. Child
  **threads** inside the inline workflow are supported.
- Scheduled inline invocation is not implemented.
- Inline runs do not participate in WfSpec-name/version indexes, WfSpec metrics,
  or variable search indexes. Direct run/variable lookup, node listing, and
  external-event processing remain available.
- Each run owns its own definition record; records are not compared or shared.
  Every invocation repeats validation. WfRun output events contain
  only the reference, not a self-contained definition; consumers must fetch it
  before retention removes it. The definition does not emit a separate output event.

## Compatibility

The current prototype assigns fields 1–5 to `id`, `created_at`, `thread_specs`,
`entrypoint_thread_name`, and `retention_policy`. This renumbering is incompatible
with earlier nested and flattened formats. Previously persisted inline definitions
and old binary requests require migration before use; no automatic migration or
data reset is performed. Field numbers must remain stable before production use.
Clients must regenerate bindings. Embedded WfRun field 15 is still understood for
the unified message shape, while newly created runs use the ID in field 16.
`GetInlineWfSpec` reads separate records only.

`InlineWfRunTest` exercises execution without registered workflow metadata,
definition persistence across commands, task retries, events, variables, child
threads, sleep, failure handlers, deletion, retention, duplicate IDs, migration
rejection, and invalid definitions/inputs. Full server-restart recovery and broad
compatibility with existing API consumers are not established by these tests.
