package com.zenius.one.modules.crm;

import com.zenius.one.platform.common.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmPipelineService {

    private final CrmPipelineRepository pipelineRepository;
    private final CrmStageRepository stageRepository;

    public CrmPipelineService(CrmPipelineRepository pipelineRepository, CrmStageRepository stageRepository) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
    }

    @Transactional
    public PipelineWithStagesDto getOrProvisionDefaultPipeline() {
        UUID orgId = TenantContext.requireOrganizationId();
        CrmPipelineEntity pipeline = pipelineRepository
                .findFirstByOrganizationIdAndDefaultPipelineTrue(orgId)
                .orElseGet(() -> createDefaultPipeline(orgId));
        return toDto(pipeline, orgId);
    }

    @Transactional
    public List<PipelineWithStagesDto> listPipelines() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<CrmPipelineEntity> pipelines = pipelineRepository.findByOrganizationIdOrderByDefaultPipelineDescNameAsc(orgId);
        if (pipelines.isEmpty()) {
            pipelines = List.of(createDefaultPipeline(orgId));
        }
        return pipelines.stream().map(p -> toDto(p, orgId)).toList();
    }

    private CrmPipelineEntity createDefaultPipeline(UUID orgId) {
        CrmPipelineEntity pipeline = new CrmPipelineEntity();
        pipeline.setOrganizationId(orgId);
        pipeline.setName("Enterprise Sales Pipeline");
        pipeline.setDefaultPipeline(true);
        pipeline = pipelineRepository.save(pipeline);

        createStage(orgId, pipeline.getId(), "Qualification", 15, 1, false, false);
        createStage(orgId, pipeline.getId(), "Needs Analysis", 30, 2, false, false);
        createStage(orgId, pipeline.getId(), "Proposal & Quote", 55, 3, false, false);
        createStage(orgId, pipeline.getId(), "Negotiation", 80, 4, false, false);
        createStage(orgId, pipeline.getId(), "Closed Won", 100, 5, true, true);
        createStage(orgId, pipeline.getId(), "Closed Lost", 0, 6, true, false);
        return pipeline;
    }

    private void createStage(
            UUID orgId,
            UUID pipelineId,
            String name,
            int probability,
            int order,
            boolean closed,
            boolean won
    ) {
        CrmStageEntity stage = new CrmStageEntity();
        stage.setOrganizationId(orgId);
        stage.setPipelineId(pipelineId);
        stage.setName(name);
        stage.setProbability(probability);
        stage.setStageOrder(order);
        stage.setClosed(closed);
        stage.setWon(won);
        stageRepository.save(stage);
    }

    private PipelineWithStagesDto toDto(CrmPipelineEntity p, UUID orgId) {
        List<StageDto> stages = stageRepository
                .findByOrganizationIdAndPipelineIdOrderByStageOrderAsc(orgId, p.getId())
                .stream()
                .map(s -> new StageDto(
                        s.getId(),
                        s.getName(),
                        s.getProbability(),
                        s.getStageOrder(),
                        s.isClosed(),
                        s.isWon()
                ))
                .toList();
        return new PipelineWithStagesDto(p.getId(), p.getName(), p.isDefaultPipeline(), stages);
    }

    public record StageDto(
            UUID id,
            String name,
            Integer probability,
            Integer stageOrder,
            boolean closed,
            boolean won
    ) {
    }

    public record PipelineWithStagesDto(
            UUID id,
            String name,
            boolean defaultPipeline,
            List<StageDto> stages
    ) {
    }
}

