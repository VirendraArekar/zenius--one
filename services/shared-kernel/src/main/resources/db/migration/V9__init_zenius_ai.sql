-- Phase 9: Zenius AI (Assistant, 11 Specialized Agents, RAG Knowledge Base, HITL Proposals)

CREATE TABLE ai_knowledge_docs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    title VARCHAR(180) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL',
    content_text TEXT NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE ai_knowledge_chunks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    document_id UUID NOT NULL REFERENCES ai_knowledge_docs(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding_json TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL,
    title VARCHAR(180) NOT NULL,
    agent_code VARCHAR(32) NOT NULL DEFAULT 'ASSISTANT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(24) NOT NULL,
    content TEXT NOT NULL,
    citations_json TEXT NOT NULL DEFAULT '[]',
    tool_calls_json TEXT NOT NULL DEFAULT '[]',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE ai_action_proposals (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE SET NULL,
    agent_code VARCHAR(32) NOT NULL,
    action_type VARCHAR(48) NOT NULL,
    summary VARCHAR(300) NOT NULL,
    payload_json TEXT NOT NULL DEFAULT '{}',
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(160),
    executed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system'
);

INSERT INTO permissions (id, module_code, permission_key, description) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'ai', 'ai:read', 'Query Zenius AI assistant, specialized agents, insights, and RAG knowledge'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002', 'ai', 'ai:write', 'Ingest RAG knowledge documents and approve AI-proposed workflow actions');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ORG_OWNER', 'ORG_ADMIN')
  AND p.module_code = 'ai'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE platform_modules SET status = 'ACTIVE' WHERE code = 'ai';
UPDATE system_settings SET setting_value = '9' WHERE setting_key = 'platform.current.phase';

