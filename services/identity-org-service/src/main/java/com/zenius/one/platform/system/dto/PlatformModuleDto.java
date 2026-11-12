package com.zenius.one.platform.system.dto;

import java.util.UUID;

public record PlatformModuleDto(
        UUID id,
        String code,
        String name,
        String tagline,
        String description,
        String category,
        Integer phaseNumber,
        String status,
        String routePath,
        String iconName,
        Integer sortOrder,
        String boundedContext
) {
}

