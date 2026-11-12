package com.zenius.one.modules.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocFolderRepository extends JpaRepository<DocFolderEntity, UUID> {
    List<DocFolderEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<DocFolderEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<DocFolderEntity> findFirstByOrganizationIdAndParentFolderIdIsNullOrderByCreatedAtAsc(UUID organizationId);
}

