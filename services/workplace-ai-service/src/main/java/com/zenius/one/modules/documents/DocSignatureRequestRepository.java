package com.zenius.one.modules.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocSignatureRequestRepository extends JpaRepository<DocSignatureRequestEntity, UUID> {
    List<DocSignatureRequestEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<DocSignatureRequestEntity> findByOrganizationIdAndDocumentIdOrderByCreatedAtDesc(
            UUID organizationId, UUID documentId);

    Optional<DocSignatureRequestEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

