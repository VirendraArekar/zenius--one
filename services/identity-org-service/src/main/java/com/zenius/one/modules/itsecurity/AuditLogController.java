package com.zenius.one.modules.itsecurity;

import com.zenius.one.modules.itsecurity.AuditLogService.AuditLogDto;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Validated
@Tag(name = "Security Audit Logs", description = "Immutable tenant-isolated security & mutation audit trail")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('audit:read')")
    @Operation(summary = "List and filter organization audit logs")
    public ApiResponse<List<AuditLogDto>> listLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<AuditLogDto> result = auditLogService.listLogs(
                action,
                q,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponse.page(result, result.getContent());
    }
}

