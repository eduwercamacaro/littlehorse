# Inline workflow prototype

`RunInlineWf` validates an inline definition and starts a normal durable `WfRun`
without registering a `WfSpec`. Task execution still uses `ThreadRun`, `NodeRun`,
and `TaskRun`; this is ephemeral orchestration, not standalone task invocation.
Only server behavior is implemented. Generated protocol bindings expose the RPC,
but there are no new SDK builders, CLI commands, or dashboard features.

## API

`RunInlineWfRequest` contains `wf_spec` (`InlineWfSpecDefinition`), `variables`, and
an optional `id`. A definition contains `thread_specs`, `entrypoint_thread_name`,
and an optional `retention_policy`. Existing TaskDefs and other referenced metadata
must already be registered. The caller needs both workflow `RUN` and
`WRITE_METADATA` permissions.

The returned `WfRun` has `inline_wf_spec` instead of `wf_spec_id`. Its embedded
snapshot includes the validated definition and a server-computed SHA-256 checksum.
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
  the resulting definition is what is persisted and fingerprinted.
- The definition lives with its WfRun, in the same partition. A transient
  `WfSpecModel` view lets the existing execution engine use it without inventing
  or registering a WfSpec ID. Subsequent commands resolve it from the stored run.
- Inline ThreadRuns, NodeRuns, and Variables omit `wf_spec_id`. Consumers must
  resolve their definition through the owning WfRun instead of assuming that
  every run has a registered spec.
- Existing task retries, external events, child threads, failure handlers, sleep
  timers, run deletion, and workflow retention use the normal execution paths.
  Retention deletes the embedded definition with the run.
- The checksum uses deterministic protobuf serialization. It is a prototype
  fingerprint, not a cross-version canonical-format guarantee or a deduplication
  key. Task execution retains its existing delivery semantics.

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
- The definition is copied into every run rather than stored in a shared cache.
  Large definitions amplify run storage/output updates, and every invocation
  repeats validation. There is no metadata-level definition deduplication.

`InlineWfRunTest` exercises execution without registered workflow metadata,
definition persistence across commands, task retries, events, variables, child
threads, sleep, failure handlers, deletion, retention, duplicate IDs, migration
rejection, and invalid definitions/inputs. Full server-restart recovery and broad
compatibility with existing API consumers are not established by these tests.
