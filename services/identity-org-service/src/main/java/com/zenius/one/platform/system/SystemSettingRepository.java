package com.zenius.one.platform.system;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, UUID> {

    List<SystemSettingEntity> findByIsPublicTrueOrderByCategoryAscSettingKeyAsc();
}

