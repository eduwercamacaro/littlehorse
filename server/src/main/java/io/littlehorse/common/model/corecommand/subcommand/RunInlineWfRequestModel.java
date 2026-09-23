package io.littlehorse.common.model.corecommand.subcommand;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import io.grpc.Status;
import io.littlehorse.common.LHSerializable;
import io.littlehorse.common.LHServerConfig;
import io.littlehorse.common.exceptions.LHApiException;
import io.littlehorse.common.exceptions.LHValidationException;
import io.littlehorse.common.model.corecommand.CoreSubCommand;
import io.littlehorse.common.model.getable.core.noderun.NodeFailureException;
import io.littlehorse.common.model.getable.core.variable.VariableValueModel;
import io.littlehorse.common.model.getable.core.wfrun.InlineWfSpecModel;
import io.littlehorse.common.model.getable.core.wfrun.WfRunModel;
import io.littlehorse.common.model.getable.global.wfspec.InlineWfSpecDefinitionModel;
import io.littlehorse.common.model.getable.global.wfspec.WfSpecModel;
import io.littlehorse.common.model.getable.objectId.InlineWfSpecIdModel;
import io.littlehorse.common.model.getable.objectId.WfRunIdModel;
import io.littlehorse.common.util.LHUtil;
import io.littlehorse.sdk.common.proto.InlineWfSpecDefinition;
import io.littlehorse.sdk.common.proto.LHStatus;
import io.littlehorse.sdk.common.proto.Node.NodeCase;
import io.littlehorse.sdk.common.proto.RunInlineWfRequest;
import io.littlehorse.sdk.common.proto.ThreadType;
import io.littlehorse.sdk.common.proto.WfRun;
import io.littlehorse.server.streams.topology.core.CoreProcessorContext;
import io.littlehorse.server.streams.topology.core.ExecutionContext;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

public class RunInlineWfRequestModel extends CoreSubCommand<RunInlineWfRequest> {
    // Prototype limits bound validation work and the stored definition size.
    private static final int MAX_DEFINITION_BYTES = 256 * 1024;
    private static final int MAX_NODES = 256;
    private String id;
    private InlineWfSpecDefinitionModel definition;
    private final Map<String, VariableValueModel> variables = new HashMap<>();

    @Override
    public String getPartitionKey() {
        if (id == null) id = LHUtil.generateGuid();
        return id;
    }

    @Override
    public Class<RunInlineWfRequest> getProtoBaseClass() {
        return RunInlineWfRequest.class;
    }

    @Override
    public RunInlineWfRequest.Builder toProto() {
        RunInlineWfRequest.Builder out = RunInlineWfRequest.newBuilder();
        if (id != null) out.setId(id);
        if (definition != null) out.setWfSpec(definition.toProto());
        variables.forEach(
                (name, value) -> out.putVariables(name, value.toProto().build()));
        return out;
    }

    @Override
    public void initFrom(Message proto, ExecutionContext context) {
        RunInlineWfRequest request = (RunInlineWfRequest) proto;
        id = request.hasId() ? request.getId() : null;
        if (!request.hasWfSpec()) {
            throw new LHApiException(Status.INVALID_ARGUMENT, "Missing required argument 'wf_spec'");
        }
        if (request.getWfSpec().getSerializedSize() > MAX_DEFINITION_BYTES) {
            throw new LHApiException(Status.INVALID_ARGUMENT, "Inline definition exceeds 256 KiB");
        }
        definition = LHSerializable.fromProto(request.getWfSpec(), InlineWfSpecDefinitionModel.class, context);
        request.getVariablesMap()
                .forEach((name, value) -> variables.put(name, VariableValueModel.fromProto(value, context)));
    }

    @Override
    public WfRun process(CoreProcessorContext context, LHServerConfig config) {
        getPartitionKey();
        if (id.isEmpty() || !LHUtil.isValidLHName(id)) {
            throw new LHApiException(Status.INVALID_ARGUMENT, "Optional argument 'id' must be a valid hostname");
        }
        WfRunIdModel runId = new WfRunIdModel(id);
        if (context.getableManager().get(runId) != null) {
            throw new LHApiException(Status.ALREADY_EXISTS, "WfRun with id " + id + " already exists!");
        }
        long nodeCount = definition.getThreadSpecs().values().stream()
                .mapToLong(thread -> thread.getNodes().size())
                .sum();
        if (nodeCount > MAX_NODES || definition.getThreadSpecs().size() > MAX_NODES) {
            throw new LHApiException(Status.INVALID_ARGUMENT, "Inline definition exceeds 256 nodes or threads");
        }
        // Parent-spec identity and migration are intentionally outside the prototype.
        definition.getThreadSpecs().values().forEach(thread -> thread.getNodes()
                .values()
                .forEach(node -> {
                    if (node.getType() == NodeCase.RUN_CHILD_WF || node.getType() == NodeCase.WAIT_FOR_CHILD_WF) {
                        throw new LHApiException(
                                Status.INVALID_ARGUMENT, "Inline child workflows are not supported yet");
                    }
                }));

        InlineWfSpecModel inline = new InlineWfSpecModel();
        inline.setDefinition(definition);
        WfSpecModel spec = inline.asWfSpecModel();
        try {
            spec.validateAndMaybeBumpVersion(Optional.empty(), context);
            spec.getEntrypointThread().validateStartVariables(variables, context.metadataManager());
        } catch (LHValidationException ex) {
            throw new LHApiException(Status.INVALID_ARGUMENT, ex.getMessage());
        }
        // Validation may pin metadata references; persist and fingerprint that normalized definition.
        InlineWfSpecDefinition normalized = definition.toProto().build();
        if (normalized.getSerializedSize() > MAX_DEFINITION_BYTES) {
            throw new LHApiException(Status.INVALID_ARGUMENT, "Normalized inline definition exceeds 256 KiB");
        }
        inline.setChecksum(checksum(normalized));
        inline.setId(new InlineWfSpecIdModel(runId));
        inline.setCreatedAt(context.currentCommand().getTime());
        // Both records are staged in the same core transaction and partition.
        context.getableManager().put(inline);

        WfRunModel run = new WfRunModel(context);
        run.setId(runId);
        run.setInlineWfSpecId(inline.getId());
        run.setWfSpec(spec);
        run.setStartTime(context.currentCommand().getTime());
        run.transitionTo(LHStatus.RUNNING);
        context.getableManager().put(run);
        try {
            run.startThread(spec.getEntrypointThreadName(), run.getStartTime(), null, variables, ThreadType.ENTRYPOINT);
        } catch (NodeFailureException ex) {
            throw new IllegalStateException("Could not start inline entrypoint", ex);
        }
        run.advance(run.getStartTime());
        return run.toProto().build();
    }

    private static String checksum(InlineWfSpecDefinition definition) {
        byte[] bytes = new byte[definition.getSerializedSize()];
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.useDeterministicSerialization();
        try {
            definition.writeTo(output);
            output.checkNoSpaceLeft();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not fingerprint inline definition", ex);
        }
    }
}
