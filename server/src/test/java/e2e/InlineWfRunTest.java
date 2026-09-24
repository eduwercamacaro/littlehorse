package e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.sdk.common.proto.*;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.wfsdk.SpawnedThreads;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.WorkerContext;
import io.littlehorse.test.LHTest;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

@LHTest(externalEventNames = "inline-prototype-release")
public class InlineWfRunTest {
    private LittleHorseBlockingStub client;

    private InlineWfSpec definition(String taskName, String[] names) {
        PutWfSpecRequest compiled = Workflow.newWorkflow("unregistered-inline-prototype", thread -> {
                    thread.execute("greet");
                })
                .compileWorkflow();
        for (int i = 0; i < 1000; i++) {
            client.runWf(Workflow.inlineWorkflow(thread ->{
                thread.execute(taskName, i);
            }).compile());
        }


        return inlineDefinition(compiled);
    }

    private InlineWfSpec inlineDefinition(PutWfSpecRequest compiled) {
        return InlineWfSpec.newBuilder()
                .putAllThreadSpecs(compiled.getThreadSpecsMap())
                .setEntrypointThreadName(compiled.getEntrypointThreadName())
                .build();
    }

    private RunInlineWfRequest request(String id) {
        return RunInlineWfRequest.newBuilder()
                .setId(id)
                .setWfSpec(definition())
                .putVariables(
                        "input", VariableValue.newBuilder().setStr("hello").build())
                .build();
    }

    @Test
    void executesWithoutRegisteredMetadataAcrossEventsAndTaskRetries() {
        WfRun run = client.runInlineWf(request(UUID.randomUUID().toString()));
        assertThat(run.hasInlineWfSpec()).isFalse();
        assertThat(run.hasInlineWfSpecId()).isTrue();
        assertThat(run.hasWfSpecId()).isFalse();
        InlineWfSpec snapshot = client.getInlineWfSpec(run.getInlineWfSpecId());
        assertThat(snapshot.getId().getWfRunId()).isEqualTo(run.getId());
        assertThat(snapshot.hasCreatedAt()).isTrue();
        assertThat(snapshot.getThreadSpecsCount()).isGreaterThan(0);
        assertThat(snapshot.getEntrypointThreadName()).isEqualTo(definition().getEntrypointThreadName());
        assertThatThrownBy(() -> client.getLatestWfSpec(GetLatestWfSpecRequest.newBuilder()
                        .setName("unregistered-inline-prototype")
                        .build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));

        // A later command must reload the separate definition through the run's reference.
        client.putExternalEvent(PutExternalEventRequest.newBuilder()
                .setWfRunId(run.getId())
                .setExternalEventDefId(ExternalEventDefId.newBuilder().setName("inline-prototype-release"))
                .build());
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(
                        client.getWfRun(run.getId()).getStatus())
                .isEqualTo(LHStatus.COMPLETED));

        WfRun completed = client.getWfRun(run.getId());
        assertThat(completed.getThreadRuns(0).getOutput().getStr()).isEqualTo("hello world");
        assertThat(completed.getInlineWfSpecId()).isEqualTo(run.getInlineWfSpecId());
        assertThat(completed.hasInlineWfSpec()).isFalse();
        assertThat(client.getInlineWfSpec(completed.getInlineWfSpecId())).isEqualTo(snapshot);
        assertThat(completed.getThreadRuns(0).hasWfSpecId()).isFalse();
        var tasks = client.listTaskRuns(
                ListTaskRunsRequest.newBuilder().setWfRunId(run.getId()).build());
        assertThat(tasks.getResultsCount()).isEqualTo(1);
        assertThat(tasks.getResults(0).getAttemptsCount()).isEqualTo(2);
        assertThat(tasks.getResults(0).getStatus()).isEqualTo(TaskStatus.TASK_SUCCESS);
        var nodes = client.listNodeRuns(
                ListNodeRunsRequest.newBuilder().setWfRunId(run.getId()).build());
        assertThat(nodes.getResultsList())
                .allSatisfy(node -> assertThat(node.hasWfSpecId()).isFalse());
        Variable variable = client.getVariable(VariableId.newBuilder()
                .setWfRunId(run.getId())
                .setThreadRunNumber(0)
                .setName("input")
                .build());
        assertThat(variable.getValue().getStr()).isEqualTo("hello");
        assertThat(variable.hasWfSpecId()).isFalse();

        client.deleteWfRun(DeleteWfRunRequest.newBuilder().setId(run.getId()).build());
        assertDefinitionDeleted(run.getInlineWfSpecId());
        assertThatThrownBy(() -> client.getWfRun(run.getId()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void rejectsDuplicatesAndMigrationAndPreservesExistingDefinition() {
        RunInlineWfRequest request = request(UUID.randomUUID().toString());
        WfRun run = client.runInlineWf(request);
        InlineWfSpec snapshot = client.getInlineWfSpec(run.getInlineWfSpecId());
        assertThatThrownBy(() -> client.runInlineWf(request))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS));
        assertThatThrownBy(() -> client.applyWorkflowMigrationPlan(ApplyWorkflowMigrationPlanRequest.newBuilder()
                        .setWfRunId(run.getId())
                        .build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION));
        assertThat(client.getInlineWfSpec(run.getInlineWfSpecId())).isEqualTo(snapshot);
        WfRun second = client.runInlineWf(
                request.toBuilder().setId(UUID.randomUUID().toString()).build());
        InlineWfSpec secondSnapshot = client.getInlineWfSpec(second.getInlineWfSpecId());
        assertThat(secondSnapshot.getId()).isNotEqualTo(snapshot.getId());
        client.deleteWfRun(DeleteWfRunRequest.newBuilder().setId(run.getId()).build());
        assertDefinitionDeleted(run.getInlineWfSpecId());
        assertThat(client.getInlineWfSpec(second.getInlineWfSpecId())).isEqualTo(secondSnapshot);
        client.deleteWfRun(DeleteWfRunRequest.newBuilder().setId(second.getId()).build());
    }

    @Test
    void resolvesInlineDefinitionForChildThreadsSleepAndFailureHandlers() {
        var compiled = Workflow.newWorkflow("unregistered-inline-threads", thread -> {
                    var result = thread.declareStr("result");
                    var child = thread.spawnThread(
                            childThread -> {
                                childThread.sleepSeconds(0);
                                childThread.mutate(
                                        result,
                                        VariableMutationType.ASSIGN,
                                        childThread
                                                .execute("inline-prototype-task", "child")
                                                .withRetries(1));
                            },
                            "child",
                            null);
                    thread.waitForThreads(SpawnedThreads.of(child));
                    var failingTask = thread.execute("inline-prototype-task", "fail");
                    thread.handleError(
                            failingTask, handler -> handler.mutate(result, VariableMutationType.ASSIGN, "handled"));
                    thread.complete(result);
                })
                .compileWorkflow();
        WfRun run = client.runInlineWf(RunInlineWfRequest.newBuilder()
                .setWfSpec(inlineDefinition(compiled))
                .build());
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(
                        client.getWfRun(run.getId()).getStatus())
                .isEqualTo(LHStatus.COMPLETED));
        WfRun completed = client.getWfRun(run.getId());
        assertThat(completed.getThreadRuns(0).getOutput().getStr()).isEqualTo("handled");
        assertThat(completed.getThreadRunsCount()).isEqualTo(3);
        client.deleteWfRun(DeleteWfRunRequest.newBuilder().setId(run.getId()).build());
    }

    @Test
    void deletesInlineRunWithItsRetentionPolicy() {
        var compiled = Workflow.newWorkflow("unregistered-inline-retention", thread -> {})
                .compileWorkflow();
        WfRun run = client.runInlineWf(RunInlineWfRequest.newBuilder()
                .setWfSpec(inlineDefinition(compiled).toBuilder()
                        .setRetentionPolicy(WorkflowRetentionPolicy.newBuilder().setSecondsAfterWfTermination(0)))
                .build());
        assertThat(run.getStatus()).isEqualTo(LHStatus.COMPLETED);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThatThrownBy(
                        () -> client.getWfRun(run.getId()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND)));
        assertDefinitionDeleted(run.getInlineWfSpecId());
    }

    @Test
    void rejectsInvalidDefinitionAndInputsBeforeCreatingRun() {
        String id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> client.runInlineWf(request(id).toBuilder()
                        .setWfSpec(definition().toBuilder().setEntrypointThreadName("missing"))
                        .build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
        assertThatThrownBy(() -> client.runInlineWf(
                        request(id).toBuilder().clearVariables().build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
        assertThatThrownBy(() -> client.getWfRun(WfRunId.newBuilder().setId(id).build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
        assertDefinitionDeleted(InlineWfSpecId.newBuilder()
                .setWfRunId(WfRunId.newBuilder().setId(id))
                .build());
    }

    private void assertDefinitionDeleted(InlineWfSpecId id) {
        assertThatThrownBy(() -> client.getInlineWfSpec(id))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void rejectsServerManagedFieldsInSubmittedSpec() {
        String id = UUID.randomUUID().toString();
        for (InlineWfSpec supplied : java.util.List.of(
                definition().toBuilder()
                        .setId(InlineWfSpecId.getDefaultInstance())
                        .build(),
                definition().toBuilder()
                        .setCreatedAt(com.google.protobuf.Timestamp.getDefaultInstance())
                        .build())) {
            assertThatThrownBy(() -> client.runInlineWf(
                            request(id).toBuilder().setWfSpec(supplied).build()))
                    .isInstanceOfSatisfying(
                            StatusRuntimeException.class,
                            ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
        }
        assertDefinitionDeleted(InlineWfSpecId.newBuilder()
                .setWfRunId(WfRunId.newBuilder().setId(id))
                .build());
    }

    @Test
    void rejectsLookupWithoutOwner() {
        assertThatThrownBy(() -> client.getInlineWfSpec(InlineWfSpecId.getDefaultInstance()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        ex -> assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @LHTaskMethod("inline-prototype-task")
    public String task(String input, WorkerContext context) {
        if (context.getAttemptNumber() == 0) throw new IllegalStateException("Retry this task");
        return input + " world";
    }
}
