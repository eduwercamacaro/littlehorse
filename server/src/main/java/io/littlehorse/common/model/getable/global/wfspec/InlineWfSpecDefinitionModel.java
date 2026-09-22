package io.littlehorse.common.model.getable.global.wfspec;

import com.google.protobuf.Message;
import io.littlehorse.common.LHSerializable;
import io.littlehorse.common.model.getable.global.wfspec.thread.ThreadSpecModel;
import io.littlehorse.sdk.common.proto.InlineWfSpecDefinition;
import io.littlehorse.sdk.common.proto.ThreadSpec;
import io.littlehorse.server.streams.topology.core.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InlineWfSpecDefinitionModel extends LHSerializable<InlineWfSpecDefinition> {

    private Map<String, ThreadSpecModel> threadSpecs = new HashMap<>();
    private String entrypointThreadName;
    private WorkflowRetentionPolicyModel retentionPolicy;

    @Override
    public Class<InlineWfSpecDefinition> getProtoBaseClass() {
        return InlineWfSpecDefinition.class;
    }

    @Override
    public InlineWfSpecDefinition.Builder toProto() {
        InlineWfSpecDefinition.Builder out =
                InlineWfSpecDefinition.newBuilder().setEntrypointThreadName(entrypointThreadName);
        for (Map.Entry<String, ThreadSpecModel> entry : threadSpecs.entrySet()) {
            out.putThreadSpecs(entry.getKey(), entry.getValue().toProto().build());
        }
        if (retentionPolicy != null) {
            out.setRetentionPolicy(retentionPolicy.toProto());
        }
        return out;
    }

    @Override
    public void initFrom(Message proto, ExecutionContext context) {
        InlineWfSpecDefinition p = (InlineWfSpecDefinition) proto;
        entrypointThreadName = p.getEntrypointThreadName();
        threadSpecs.clear();
        for (Map.Entry<String, ThreadSpec> entry : p.getThreadSpecsMap().entrySet()) {
            ThreadSpecModel threadSpec = LHSerializable.fromProto(entry.getValue(), ThreadSpecModel.class, context);
            threadSpec.setName(entry.getKey());
            threadSpecs.put(entry.getKey(), threadSpec);
        }
        retentionPolicy = p.hasRetentionPolicy()
                ? LHSerializable.fromProto(p.getRetentionPolicy(), WorkflowRetentionPolicyModel.class, context)
                : null;
    }
}
