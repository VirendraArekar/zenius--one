package com.zenius.one.platform.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenius.one.platform.common.ResourceNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SystemPlatformServiceTest {

    @Mock
    private PlatformModuleRepository moduleRepository;

    @Mock
    private SystemSettingRepository settingRepository;

    @Mock
    private DataSource dataSource;

    private SystemPlatformService service;

    @BeforeEach
    void setUp() {
        service = new SystemPlatformService(
                moduleRepository,
                settingRepository,
                dataSource,
                "0.1.0-TEST",
                1
        );
    }

    @Test
    void getSystemStatusAggregatesCountsAndSettings() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("16.4");

        when(moduleRepository.count()).thenReturn(20L);
        when(moduleRepository.countByStatus("ACTIVE")).thenReturn(1L);
        when(moduleRepository.countByStatus("PLANNED")).thenReturn(19L);

        SystemSettingEntity setting = new SystemSettingEntity();
        setting.setSettingKey("platform.name");
        setting.setSettingValue("Zenius One");
        setting.setCategory("GENERAL");
        setting.setDescription("Product name");
        setting.setPublic(true);
        when(settingRepository.findByIsPublicTrueOrderByCategoryAscSettingKeyAsc())
                .thenReturn(List.of(setting));

        var status = service.getSystemStatus();

        assertThat(status.platformName()).isEqualTo("Zenius One");
        assertThat(status.version()).isEqualTo("0.1.0-TEST");
        assertThat(status.currentPhase()).isEqualTo(1);
        assertThat(status.databaseProduct()).isEqualTo("PostgreSQL 16.4");
        assertThat(status.totalModules()).isEqualTo(20L);
        assertThat(status.activeModules()).isEqualTo(1L);
        assertThat(status.plannedModules()).isEqualTo(19L);
        assertThat(status.publicSettings()).hasSize(1);
    }

    @Test
    void listModulesNormalizesBlankFilters() {
        PlatformModuleEntity entity = new PlatformModuleEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode("crm");
        entity.setName("CRM");
        entity.setTagline("Customer Hub");
        entity.setDescription("Full CRM");
        entity.setCategory("REVENUE");
        entity.setPhaseNumber(3);
        entity.setStatus("PLANNED");
        entity.setRoutePath("/crm");
        entity.setIconName("Users");
        entity.setSortOrder(2);
        entity.setBoundedContext("crm");

        var pageable = PageRequest.of(0, 20);
        when(moduleRepository.searchModules(isNull(), eq(3), eq("crm"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        var page = service.listModules("   ", 3, " crm ", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().code()).isEqualTo("crm");
        verify(moduleRepository).searchModules(isNull(), eq(3), eq("crm"), any());
    }

    @Test
    void getModuleByCodeThrowsWhenMissing() {
        when(moduleRepository.findByCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getModuleByCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }
}

