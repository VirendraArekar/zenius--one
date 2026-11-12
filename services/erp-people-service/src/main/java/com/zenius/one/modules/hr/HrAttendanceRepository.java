package com.zenius.one.modules.hr;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrAttendanceRepository extends JpaRepository<HrAttendanceEntity, UUID> {
    List<HrAttendanceEntity> findByOrganizationIdOrderByWorkDateDescCheckInAtDesc(UUID organizationId);
    Optional<HrAttendanceEntity> findByOrganizationIdAndEmployeeIdAndWorkDate(UUID organizationId, UUID employeeId, LocalDate workDate);
}

