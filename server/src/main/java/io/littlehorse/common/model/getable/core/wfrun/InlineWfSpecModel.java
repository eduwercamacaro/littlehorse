package io.littlehorse.common.model.getable.core.wfrun;

import com.google.protobuf.Message;
import io.littlehorse.common.LHSerializable;
import io.littlehorse.common.model.AbstractGetable;
import io.littlehorse.common.model.CoreGetable;
import io.littlehorse.common.model.getable.global.wfspec.InlineWfSpecDefinitionModel;
import io.littlehorse.common.model.getable.global.wfspec.WfSpecModel;
import io.littlehorse.common.model.getable.objectId.InlineWfSpecIdModel;
import io.littlehorse.common.proto.TagStorageType;
import io.littlehorse.common.util.LHUtil;
import io.littlehorse.sdk.common.proto.InlineWfSpec;
import io.littlehorse.server.streams.storeinternals.GetableIndex;
import io.littlehorse.server.streams.storeinternals.index.IndexedField;
import io.littlehorse.server.streams.topology.core.ExecutionContext;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InlineWfSpecModel extends CoreGetable<InlineWfSpec> {

    private InlineWfSpecIdModel id;
    private Date createdAt;
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
        if (id != null) out.setId(id.toProto());
        if (createdAt != null) out.setCreatedAt(LHUtil.fromDate(createdAt));
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
        id = p.hasId() ? LHSerializable.fromProto(p.getId(), InlineWfSpecIdModel.class, context) : null;
        createdAt = p.hasCreatedAt() ? LHUtil.fromProtoTs(p.getCreatedAt()) : null;
        definition = p.hasDefinition()
                ? LHSerializable.fromProto(p.getDefinition(), InlineWfSpecDefinitionModel.class, context)
                : null;
        checksum = p.getChecksum();
    }

    @Override
    public InlineWfSpecIdModel getObjectId() {
        return id;
    }

    @Override
    public List<GetableIndex<? extends AbstractGetable<?>>> getIndexConfigurations() {
        return List.of();
    }

    @Override
    public List<IndexedField> getIndexValues(String key, Optional<TagStorageType> tagStorageType) {
        return List.of();
    }
}
