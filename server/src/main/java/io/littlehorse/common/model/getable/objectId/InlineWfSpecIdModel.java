package io.littlehorse.common.model.getable.objectId;

import com.google.protobuf.Message;
import io.littlehorse.common.LHSerializable;
import io.littlehorse.common.model.getable.CoreObjectId;
import io.littlehorse.common.model.getable.core.wfrun.InlineWfSpecModel;
import io.littlehorse.common.proto.GetableClassEnum;
import io.littlehorse.sdk.common.proto.InlineWfSpec;
import io.littlehorse.sdk.common.proto.InlineWfSpecId;
import io.littlehorse.server.streams.topology.core.ExecutionContext;
import java.util.Optional;
import lombok.Getter;

@Getter
public class InlineWfSpecIdModel extends CoreObjectId<InlineWfSpecId, InlineWfSpec, InlineWfSpecModel> {
    private WfRunIdModel wfRunId;

    public InlineWfSpecIdModel() {}

    public InlineWfSpecIdModel(WfRunIdModel wfRunId) {
        this.wfRunId = wfRunId;
    }

    @Override
    public InlineWfSpecId.Builder toProto() {
        return InlineWfSpecId.newBuilder().setWfRunId(wfRunId.toProto());
    }

    @Override
    public void initFrom(Message proto, ExecutionContext context) {
        wfRunId = LHSerializable.fromProto(((InlineWfSpecId) proto).getWfRunId(), WfRunIdModel.class, context);
    }

    @Override
    public Class<InlineWfSpecId> getProtoBaseClass() {
        return InlineWfSpecId.class;
    }

    @Override
    public String toString() {
        return wfRunId.toString();
    }

    @Override
    public void initFromString(String storeKey) {
        wfRunId = new WfRunIdModel();
        wfRunId.initFromString(storeKey);
    }

    @Override
    public GetableClassEnum getType() {
        return GetableClassEnum.INLINE_WF_SPEC;
    }

    @Override
    public Optional<String> getPartitionKey() {
        return wfRunId.getPartitionKey();
    }

    @Override
    public Optional<WfRunIdModel> getGroupingWfRunId() {
        return Optional.of(wfRunId);
    }
}
