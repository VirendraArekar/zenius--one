package com.zenius.one.modules.creator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatorService {

    private final CrtAppRepository appRepository;
    private final CrtFormRepository formRepository;
    private final CrtRecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public CreatorService(
            CrtAppRepository appRepository,
            CrtFormRepository formRepository,
            CrtRecordRepository recordRepository,
            ObjectMapper objectMapper,
            AuditLogService auditLogService) {
        this.appRepository = appRepository;
        this.formRepository = formRepository;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AppSummary> listApps() {
        UUID orgId = TenantContext.requireOrganizationId();
        return appRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(AppSummary::from)
                .toList();
    }

    @Transactional
    public AppSummary createApp(@Valid CreateAppRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        long seq = appRepository.countByOrganizationId(orgId) + 1;
        CrtAppEntity app = new CrtAppEntity();
        app.setOrganizationId(orgId);
        app.setAppCode(String.format("APP-%03d", seq));
        app.setName(request.name().trim());
        app.setDescription(request.description() == null ? null : request.description().trim());
        app.setStatus("DRAFT");
        app.setAppVersion(1);
        return AppSummary.from(appRepository.save(app));
    }

    @Transactional
    public AppSummary publishApp(ZeniusUserPrincipal principal, UUID appId) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrtAppEntity app = appRepository
                .findByIdAndOrganizationId(appId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Low-code app not found"));
        app.setStatus("PUBLISHED");
        app.setAppVersion(app.getAppVersion() + 1);
        CrtAppEntity saved = appRepository.save(app);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "CREATOR_APP_PUBLISHED",
                "CRT_APP",
                saved.getId().toString(),
                "Published low-code app " + saved.getAppCode() + " v" + saved.getAppVersion(),
                "SUCCESS");
        return AppSummary.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FormSummary> listForms(UUID appId) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<CrtFormEntity> list = appId == null
                ? formRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                : formRepository.findByOrganizationIdAndAppIdOrderByCreatedAtAsc(orgId, appId);
        return list.stream().map(this::toFormSummary).toList();
    }

    @Transactional
    public FormSummary createForm(@Valid CreateFormRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        appRepository
                .findByIdAndOrganizationId(request.appId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Low-code app not found"));

        long seq = formRepository.countByOrganizationId(orgId) + 1;
        CrtFormEntity form = new CrtFormEntity();
        form.setOrganizationId(orgId);
        form.setAppId(request.appId());
        form.setFormCode(String.format("FRM-%03d", seq));
        form.setName(request.name().trim());
        form.setSchemaJson(serializeFields(request.fields()));
        return toFormSummary(formRepository.save(form));
    }

    @Transactional(readOnly = true)
    public List<RecordSummary> listRecords(UUID formId) {
        UUID orgId = TenantContext.requireOrganizationId();
        return recordRepository.findByOrganizationIdAndFormIdOrderByCreatedAtDesc(orgId, formId).stream()
                .map(this::toRecordSummary)
                .toList();
    }

    @Transactional
    public RecordSummary submitRecord(
            ZeniusUserPrincipal principal, UUID formId, @Valid SubmitRecordRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        CrtFormEntity form = formRepository
                .findByIdAndOrganizationId(formId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        List<FieldDefinition> fields = parseFields(form.getSchemaJson());
        Map<String, Object> payload = new LinkedHashMap<>(request.data() == null ? Map.of() : request.data());

        for (FieldDefinition field : fields) {
            if (field.required() && !"FORMULA".equalsIgnoreCase(field.type())) {
                Object val = payload.get(field.key());
                if (val == null || val.toString().isBlank()) {
                    throw new IllegalArgumentException("Missing required custom field: " + field.label());
                }
            }
            if ("FORMULA".equalsIgnoreCase(field.type()) && field.formula() != null && !field.formula().isBlank()) {
                payload.put(field.key(), evaluateSimpleFormula(field.formula(), payload));
            }
        }

        long seq = recordRepository.countByOrganizationIdAndFormId(orgId, formId) + 1;
        CrtRecordEntity record = new CrtRecordEntity();
        record.setOrganizationId(orgId);
        record.setFormId(form.getId());
        record.setRecordNumber(String.format("REC-%04d", seq));
        record.setDataJson(serializeMap(payload));
        record.setSubmittedBy(principal.getFullName());
        return toRecordSummary(recordRepository.save(record));
    }

    private BigDecimal evaluateSimpleFormula(String formula, Map<String, Object> data) {
        String trimmed = formula.trim();
        for (String op : List.of("*", "+", "-")) {
            int idx = trimmed.indexOf(op);
            if (idx > 0) {
                String leftKey = trimmed.substring(0, idx).trim();
                String rightKey = trimmed.substring(idx + 1).trim();
                BigDecimal left = toDecimal(data.get(leftKey), leftKey);
                BigDecimal right = toDecimal(data.get(rightKey), rightKey);
                return switch (op) {
                    case "*" -> left.multiply(right).setScale(2, RoundingMode.HALF_UP);
                    case "+" -> left.add(right).setScale(2, RoundingMode.HALF_UP);
                    default -> left.subtract(right).setScale(2, RoundingMode.HALF_UP);
                };
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal toDecimal(Object value, String fallbackLiteral) {
        try {
            if (value != null) {
                return new BigDecimal(value.toString());
            }
            return new BigDecimal(fallbackLiteral);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String serializeFields(List<FieldDefinition> fields) {
        try {
            return objectMapper.writeValueAsString(fields == null ? List.of() : fields);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid form schema");
        }
    }

    private List<FieldDefinition> parseFields(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String serializeMap(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid record payload");
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private FormSummary toFormSummary(CrtFormEntity entity) {
        return new FormSummary(
                entity.getId(),
                entity.getAppId(),
                entity.getFormCode(),
                entity.getName(),
                parseFields(entity.getSchemaJson()),
                entity.getCreatedAt());
    }

    private RecordSummary toRecordSummary(CrtRecordEntity entity) {
        return new RecordSummary(
                entity.getId(),
                entity.getFormId(),
                entity.getRecordNumber(),
                parseMap(entity.getDataJson()),
                entity.getSubmittedBy(),
                entity.getCreatedAt());
    }

    public record CreateAppRequest(@NotBlank @Size(max = 160) String name, @Size(max = 500) String description) {}

    public record FieldDefinition(
            @NotBlank String key,
            @NotBlank String label,
            @NotBlank String type,
            boolean required,
            String formula) {}

    public record CreateFormRequest(
            @NotNull UUID appId, @NotBlank @Size(max = 160) String name, List<FieldDefinition> fields) {}

    public record SubmitRecordRequest(@NotNull Map<String, Object> data) {}

    public record AppSummary(
            UUID id, String appCode, String name, String description, String status, int appVersion, Instant createdAt) {
        public static AppSummary from(CrtAppEntity entity) {
            return new AppSummary(
                    entity.getId(),
                    entity.getAppCode(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getStatus(),
                    entity.getAppVersion(),
                    entity.getCreatedAt());
        }
    }

    public record FormSummary(
            UUID id, UUID appId, String formCode, String name, List<FieldDefinition> fields, Instant createdAt) {}

    public record RecordSummary(
            UUID id, UUID formId, String recordNumber, Map<String, Object> data, String submittedBy, Instant createdAt) {}
}

