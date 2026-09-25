package e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.PutVariableRequest;
import io.littlehorse.sdk.common.proto.Variable;
import io.littlehorse.sdk.common.proto.VariableId;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.test.LHTest;
import io.littlehorse.test.LHWorkflow;
import io.littlehorse.test.WorkflowVerifier;
import org.junit.jupiter.api.Test;

@LHTest
public class PutVariableTest {
    private LittleHorseBlockingStub client;
    private WorkflowVerifier verifier;

    @LHWorkflow("put-variable")
    private Workflow workflow;

    @LHWorkflow("put-variable-sleep")
    private Workflow sleepWorkflow;

    @LHWorkflow("put-variable-sleep")
    public Workflow buildSleepWorkflow() {
        return Workflow.newWorkflow("put-variable-sleep", thread -> {
            var deadline = thread.declareInt("deadline").withDefault(4102444800000L);
            thread.sleepUntil(deadline);
        });
    }

    @Test
    void shouldAdvanceWorkflowAfterReplacingVariable() {
        verifier.prepareRun(sleepWorkflow)
                .waitForNodeRunStatus(0, 1, LHStatus.RUNNING)
                .thenVerifyWfRun(wfRun -> client.putVariable(PutVariableRequest.newBuilder()
                        .setId(VariableId.newBuilder()
                                .setWfRunId(wfRun.getId())
                                .setThreadRunNumber(0)
                                .setName("deadline"))
                        .setValue(VariableValue.newBuilder().setInt(0))
                        .build()))
                .waitForStatus(LHStatus.COMPLETED)
                .start();
    }

    @LHWorkflow("put-variable")
    public Workflow buildWorkflow() {
        return Workflow.newWorkflow("put-variable", thread -> {
            var value = thread.declareStr("value").withDefault("original");
            var observed = thread.declareStr("observed");
            thread.waitForEvent("put-variable-continue").registeredAs(String.class);
            observed.assign(value);
        });
    }

    @Test
    void shouldReplaceExistingVariableAndUseItAfterEvent() {
        verifier.prepareRun(workflow)
                .waitForNodeRunStatus(0, 1, LHStatus.RUNNING)
                .thenVerifyWfRun(wfRun -> {
                    VariableId id = VariableId.newBuilder()
                            .setWfRunId(wfRun.getId())
                            .setThreadRunNumber(0)
                            .setName("value")
                            .build();
                    Variable before = client.getVariable(id);
                    VariableValue replacement =
                            VariableValue.newBuilder().setStr("replacement").build();
                    client.putVariable(PutVariableRequest.newBuilder()
                            .setId(id)
                            .setValue(replacement)
                            .build());
                    assertThat(client.getVariable(id))
                            .isEqualTo(before.toBuilder().setValue(replacement).build());
                    assertThat(client.getWfRun(wfRun.getId()).getStatus()).isEqualTo(LHStatus.RUNNING);

                    StatusRuntimeException missing = assertThrows(
                            StatusRuntimeException.class,
                            () -> client.putVariable(PutVariableRequest.newBuilder()
                                    .setId(id.toBuilder().setName("missing"))
                                    .setValue(replacement)
                                    .build()));
                    assertThat(missing.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    StatusRuntimeException malformed = assertThrows(
                            StatusRuntimeException.class,
                            () -> client.putVariable(
                                    PutVariableRequest.newBuilder().setId(id).build()));
                    assertThat(malformed.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                })
                .thenSendExternalEventWithContent("put-variable-continue", "continue")
                .waitForStatus(LHStatus.COMPLETED)
                .thenVerifyVariable(
                        0, "observed", value -> assertThat(value.getStr()).isEqualTo("replacement"))
                .start();
    }

    @Test
    void shouldAllowExplicitNull() {
        verifier.prepareRun(workflow)
                .waitForNodeRunStatus(0, 1, LHStatus.RUNNING)
                .thenVerifyWfRun(wfRun -> {
                    VariableId id = VariableId.newBuilder()
                            .setWfRunId(wfRun.getId())
                            .setThreadRunNumber(0)
                            .setName("value")
                            .build();
                    client.putVariable(PutVariableRequest.newBuilder()
                            .setId(id)
                            .setValue(VariableValue.getDefaultInstance())
                            .build());
                    assertThat(client.getVariable(id).getValue()).isEqualTo(VariableValue.getDefaultInstance());
                })
                .thenSendExternalEventWithContent("put-variable-continue", "continue")
                .waitForStatus(LHStatus.COMPLETED)
                .start();
    }
}
