package io.littlehorse.common.model.corecommand.subcommand;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.Status;
import io.littlehorse.common.LHSerializable;
import io.littlehorse.common.LHServerConfig;
import io.littlehorse.common.exceptions.LHApiException;
import io.littlehorse.common.model.corecommand.CoreSubCommand;
import io.littlehorse.common.model.getable.core.variable.VariableModel;
import io.littlehorse.common.model.getable.core.variable.VariableValueModel;
import io.littlehorse.common.model.getable.core.wfrun.WfRunModel;
import io.littlehorse.common.model.getable.objectId.VariableIdModel;
import io.littlehorse.sdk.common.proto.PutVariableRequest;
import io.littlehorse.server.streams.topology.core.CoreProcessorContext;
import io.littlehorse.server.streams.topology.core.ExecutionContext;

public class PutVariableRequestModel extends CoreSubCommand<PutVariableRequest> {
    private VariableIdModel id;
    private VariableValueModel value;

    @Override
    public Class<PutVariableRequest> getProtoBaseClass() {
        return PutVariableRequest.class;
    }

    @Override
    public PutVariableRequest.Builder toProto() {
        return PutVariableRequest.newBuilder().setId(id.toProto()).setValue(value.toProto());
    }

    @Override
    public void initFrom(Message proto, ExecutionContext context) {
        PutVariableRequest request = (PutVariableRequest) proto;
        if (!request.hasId()
                || !request.getId().hasWfRunId()
                || request.getId().getWfRunId().getId().isEmpty()
                || request.getId().getName().isEmpty()
                || request.getId().getThreadRunNumber() < 0
                || !request.hasValue()) {
            throw new LHApiException(Status.INVALID_ARGUMENT, "Variable ID and replacement value are required");
        }
        id = LHSerializable.fromProto(request.getId(), VariableIdModel.class, context);
        value = LHSerializable.fromProto(request.getValue(), VariableValueModel.class, context);
    }

    @Override
    public String getPartitionKey() {
        return id.getPartitionKey().get();
    }

    @Override
    public Empty process(CoreProcessorContext context, LHServerConfig config) {
        VariableModel variable = context.getableManager().get(id);
        if (variable == null) {
            throw new LHApiException(Status.NOT_FOUND, "Couldn't find provided Variable");
        }
        variable.setValue(value);
        context.getableManager().put(variable);
        WfRunModel wfRun = context.getableManager().get(id.getWfRunId());
        wfRun.advance(context.currentCommand().getTime());
        return Empty.getDefaultInstance();
    }
}
