package com.zenius.one.platform.system;

import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.system.dto.PlatformModuleDto;
import com.zenius.one.platform.system.dto.SystemStatusDto;
import com.zenius.one.platform.system.dto.SystemStatusDto.PublicSettingDto;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemPlatformService {

    private final PlatformModuleRepository moduleRepository;
    private final SystemSettingRepository settingRepository;
    private final DataSource dataSource;
    private final String platformVersion;
    private final int currentPhase;

    public SystemPlatformService(
            PlatformModuleRepository moduleRepository,
            SystemSettingRepository settingRepository,
            DataSource dataSource,
            @Value("${zenius.platform.version:0.1.0-PHASE1}") String platformVersion,
            @Value("${zenius.platform.current-phase:1}") int currentPhase
    ) {
        this.moduleRepository = moduleRepository;
        this.settingRepository = settingRepository;
        this.dataSource = dataSource;
        this.platformVersion = platformVersion;
        this.currentPhase = currentPhase;
    }

    public SystemStatusDto getSystemStatus() {
        long total = moduleRepository.count();
        long active = moduleRepository.countByStatus("ACTIVE");
        long planned = moduleRepository.countByStatus("PLANNED");
        List<PublicSettingDto> settings = settingRepository.findByIsPublicTrueOrderByCategoryAscSettingKeyAsc()
                .stream()
                .map(s -> new PublicSettingDto(
                        s.getSettingKey(),
                        s.getSettingValue(),
                        s.getCategory(),
                        s.getDescription()
                ))
                .toList();

        return new SystemStatusDto(
                "Zenius One",
                platformVersion,
                currentPhase,
                "OPERATIONAL",
                resolveDatabaseProduct(),
                total,
                active,
                planned,
                Instant.now(),
                settings
        );
    }

    public Page<PlatformModuleDto> listModules(String category, Integer phase, String query, Pageable pageable) {
        String normalizedCategory = normalizeFilter(category);
        String normalizedQuery = normalizeFilter(query);
        return moduleRepository.searchModules(normalizedCategory, phase, normalizedQuery, pageable)
                .map(this::toDto);
    }

    public PlatformModuleDto getModuleByCode(String code) {
        PlatformModuleEntity entity = moduleRepository.findByCode(code.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Platform module not found: " + code));
        return toDto(entity);
    }

    private PlatformModuleDto toDto(PlatformModuleEntity entity) {
        return new PlatformModuleDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getTagline(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getPhaseNumber(),
                entity.getStatus(),
                entity.getRoutePath(),
                entity.getIconName(),
                entity.getSortOrder(),
                entity.getBoundedContext()
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveDatabaseProduct() {
        try (Connection connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (Exception ex) {
            return "PostgreSQL (Unavailable)";
        }
    }
}

