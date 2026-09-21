package io.littlehorse.common.model.corecommand.subcommand;

import io.littlehorse.common.LHConstants;
import io.littlehorse.common.model.getable.core.usertaskrun.UserTaskRunModel;
import io.littlehorse.common.model.getable.global.structdef.StructDefModel;
import io.littlehorse.common.model.getable.global.wfspec.node.subnode.usertasks.UserTaskDefModel;
import io.littlehorse.common.model.getable.objectId.NodeRunIdModel;
import io.littlehorse.common.model.getable.objectId.StructDefIdModel;
import io.littlehorse.common.model.getable.objectId.UserTaskDefIdModel;
import io.littlehorse.common.model.getable.objectId.UserTaskRunIdModel;
import io.littlehorse.common.proto.Command;
import io.littlehorse.common.util.LHUtil;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.SaveUserTaskRunProgressRequest;
import io.littlehorse.sdk.common.proto.SaveUserTaskRunProgressRequest.SaveUserTaskRunAssignmentPolicy;
import io.littlehorse.sdk.common.proto.Struct;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.UserTaskEvent.EventCase;
import io.littlehorse.sdk.common.proto.UserTaskRunStatus;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.server.TestCoreProcessorContext;
import io.littlehorse.server.streams.topology.core.CommandProcessorOutput;
import io.littlehorse.server.streams.util.HeadersUtil;
import java.util.Date;
import org.apache.kafka.streams.processor.api.MockProcessorContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SaveUserTaskRunProgressRequestModelTest {

    @Test
    void shouldSavePartialStructProgress() {
        MockProcessorContext<String, CommandProcessorOutput> mockProcessorContext = new MockProcessorContext<>();
        Date savedAt = new Date(1_000L);
        NodeRunIdModel nodeRunId = new NodeRunIdModel("wf-run", 0, 1);
        UserTaskRunIdModel taskId = new UserTaskRunIdModel(nodeRunId);
        StructDefIdModel structDefId = new StructDefIdModel("item-request-form", 0);
        VariableValue requestedItem =
                VariableValue.newBuilder().setStr("a laptop").build();

        SaveUserTaskRunProgressRequest progress = createProgressRequest(taskId, structDefId, requestedItem);
        // The shared harness registers Kafka Streams in-memory stores with this
        // MockProcessorContext and uses real metadata and Getable managers.
        TestCoreProcessorContext freshContext = freshContext(
                Command.newBuilder()
                        .setTime(LHUtil.fromDate(savedAt))
                        .setSaveUserTaskRunProgress(progress)
                        .build(),
                mockProcessorContext);

        UserTaskDefModel taskDef = createUserTaskDef(structDefId);
        StructDefModel structDef = createStructDef(structDefId, freshContext);
        freshContext.metadataManager().put(taskDef);
        freshContext.metadataManager().put(structDef);

        UserTaskRunModel task = createUserTaskRun(nodeRunId, taskDef.getObjectId(), freshContext);

        freshContext.getableManager().put(task);
        freshContext.endExecution();
        UserTaskRunModel originalTask = freshContext.getableManager().get(taskId);
        Assertions.assertThat(originalTask.getResults()).isEmpty();
        Assertions.assertThat(originalTask.getOutput()).isNull();
        freshContext.endExecution();

        SaveUserTaskRunProgressRequestModel request =
                freshContext.currentCommand().getSaveUserTaskRunProgress();

        request.process(freshContext, freshContext.getLhConfig());
        freshContext.endExecution();

        UserTaskRunModel storedTask = freshContext.getableManager().get(taskId);
        Assertions.assertThat(storedTask.getId()).isEqualTo(taskId);
        Assertions.assertThat(storedTask.getResults()).isEmpty();
        Assertions.assertThat(storedTask.getOutput().toProto().build()).isEqualTo(progress.getOutput());
        Assertions.assertThat(storedTask.getEvents()).singleElement().satisfies(event -> {
            Assertions.assertThat(event.getType()).isEqualTo(EventCase.SAVED);
            Assertions.assertThat(event.getTime()).isEqualTo(savedAt);
            Assertions.assertThat(event.getSaved().getUserId()).isEqualTo("anakin");
            Assertions.assertThat(event.getSaved().toProto().getResultsMap())
                    .containsOnly(java.util.Map.entry("requestedItem", requestedItem));
        });
    }

    private SaveUserTaskRunProgressRequest createProgressRequest(
            UserTaskRunIdModel taskId, StructDefIdModel structDefId, VariableValue requestedItem) {
        return SaveUserTaskRunProgressRequest.newBuilder()
                .setUserTaskRunId(taskId.toProto())
                .setPolicy(SaveUserTaskRunAssignmentPolicy.FAIL_IF_CLAIMED_BY_OTHER)
                .setUserId("anakin")
                .setOutput(VariableValue.newBuilder()
                        .setStruct(Struct.newBuilder()
                                .setStructDefId(structDefId.toProto())
                                .setStruct(InlineStruct.newBuilder()
                                        .putFields(
                                                "requestedItem",
                                                StructField.newBuilder()
                                                        .setValue(requestedItem)
                                                        .build()))))
                .build();
    }

    private UserTaskDefModel createUserTaskDef(StructDefIdModel structDefId) {
        UserTaskDefModel taskDef = new UserTaskDefModel();
        taskDef.name = "it-request";
        taskDef.version = 0;
        taskDef.createdAt = new Date(0);
        taskDef.setResultStructDefId(structDefId);
        return taskDef;
    }

    private StructDefModel createStructDef(StructDefIdModel structDefId, TestCoreProcessorContext context) {
        StructFieldDef requiredString = StructFieldDef.newBuilder()
                .setFieldType(TypeDefinition.newBuilder().setPrimitiveType(VariableType.STR))
                .build();
        return StructDefModel.fromProto(
                StructDef.newBuilder()
                        .setId(structDefId.toProto())
                        .setCreatedAt(LHUtil.fromDate(new Date(0)))
                        .setStructDef(InlineStructDef.newBuilder()
                                .putFields("requestedItem", requiredString)
                                .putFields("justification", requiredString))
                        .build(),
                context);
    }

    private UserTaskRunModel createUserTaskRun(
            NodeRunIdModel nodeRunId, UserTaskDefIdModel taskDefId, TestCoreProcessorContext context) {
        UserTaskRunModel task = new UserTaskRunModel();
        task.setId(new UserTaskRunIdModel(nodeRunId));
        task.setNodeRunId(nodeRunId);
        task.setScheduledTime(new Date(0));
        task.setExecutionContext(context);
        task.setUserTaskDefId(taskDefId);
        task.setStatus(UserTaskRunStatus.ASSIGNED);
        task.setUserId("anakin");

        return task;
    }

    private TestCoreProcessorContext freshContext(
            Command command, MockProcessorContext<String, CommandProcessorOutput> processorContext) {
        return TestCoreProcessorContext.create(
                command,
                HeadersUtil.metadataHeadersFor(LHConstants.DEFAULT_TENANT, LHConstants.ANONYMOUS_PRINCIPAL),
                processorContext);
    }
}
