package com.zenius.one.platform.system.dto;

import java.time.Instant;
import java.util.List;

public record SystemStatusDto(
        String platformName,
        String version,
        int currentPhase,
        String status,
        String databaseProduct,
        long totalModules,
        long activeModules,
        long plannedModules,
        Instant serverTime,
        List<PublicSettingDto> publicSettings
) {
    public record PublicSettingDto(
            String key,
            String value,
            String category,
            String description
    ) {
    }
}

