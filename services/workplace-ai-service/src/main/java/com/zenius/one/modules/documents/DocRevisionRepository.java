package com.zenius.one.modules.documents;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocRevisionRepository extends JpaRepository<DocRevisionEntity, UUID> {
    List<DocRevisionEntity> findByOrganizationIdAndDocumentIdOrderByRevisionNumberDesc(
            UUID organizationId, UUID documentId);
}

