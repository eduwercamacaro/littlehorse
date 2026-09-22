package io.littlehorse.common.model.getable.global.wfspec;

import com.google.protobuf.Message;
import io.littlehorse.common.LHSerializable;
import io.littlehorse.sdk.common.proto.InlineWfSpec;
import io.littlehorse.server.streams.topology.core.ExecutionContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InlineWfSpecModel extends LHSerializable<InlineWfSpec> {

    private InlineWfSpecDefinitionModel definition;
    private String checksum;

    // Runtime view only. Inline definitions have no registered metadata ID.
    public WfSpecModel asWfSpecModel() {
        WfSpecModel spec = new WfSpecModel();
        spec.setId(null);
        spec.setThreadSpecs(definition.getThreadSpecs());
        spec.setEntrypointThreadName(definition.getEntrypointThreadName());
        spec.setRetentionPolicy(definition.getRetentionPolicy());
        spec.getThreadSpecs().forEach((name, thread) -> {
            thread.setName(name);
            thread.setWfSpec(spec);
        });
        return spec;
    }

    @Override
    public Class<InlineWfSpec> getProtoBaseClass() {
        return InlineWfSpec.class;
    }

    @Override
    public InlineWfSpec.Builder toProto() {
        InlineWfSpec.Builder out = InlineWfSpec.newBuilder();
        if (definition != null) {
            out.setDefinition(definition.toProto());
        }
        if (checksum != null) {
            out.setChecksum(checksum);
        }
        return out;
    }

    @Override
    public void initFrom(Message proto, ExecutionContext context) {
        InlineWfSpec p = (InlineWfSpec) proto;
        definition = p.hasDefinition()
                ? LHSerializable.fromProto(p.getDefinition(), InlineWfSpecDefinitionModel.class, context)
                : null;
        checksum = p.getChecksum();
    }
}
