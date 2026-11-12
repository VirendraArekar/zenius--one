package com.zenius.one.platform.system;

import com.zenius.one.platform.common.ApiResponse;
import com.zenius.one.platform.system.dto.PlatformModuleDto;
import com.zenius.one.platform.system.dto.SystemStatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Validated
@Tag(name = "Platform System", description = "Live platform readiness, module registry, and public configuration")
public class SystemPlatformController {

    private final SystemPlatformService systemPlatformService;

    public SystemPlatformController(SystemPlatformService systemPlatformService) {
        this.systemPlatformService = systemPlatformService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get live platform status, database engine verification, and module counts")
    public ApiResponse<SystemStatusDto> getStatus() {
        return ApiResponse.ok(systemPlatformService.getSystemStatus());
    }

    @GetMapping("/modules")
    @Operation(summary = "List and filter all registered Zenius One business modules")
    public ApiResponse<List<PlatformModuleDto>> listModules(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer phase,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder"));
        Page<PlatformModuleDto> result = systemPlatformService.listModules(category, phase, q, pageable);
        return ApiResponse.page(result, result.getContent());
    }

    @GetMapping("/modules/{code}")
    @Operation(summary = "Fetch details of a specific registered platform module by code")
    public ApiResponse<PlatformModuleDto> getModule(@PathVariable String code) {
        return ApiResponse.ok(systemPlatformService.getModuleByCode(code));
    }
}

