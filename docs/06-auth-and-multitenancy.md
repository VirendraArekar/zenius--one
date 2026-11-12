# Zenius One — Authentication & Multi-Tenant Architecture

## 1. Authentication & SSO Architecture (Phase 2 Blueprint)

1. **Identity Provider & Token Issuance:**
   - Users authenticate once against the Identity Service (`/api/v1/auth/login` or OAuth2/OIDC SSO).
   - Passwords hashed with Argon2id / BCrypt (cost >= 12).
   - Issues short-lived **Access JWT (15 min, RS256)** containing `sub (userId)`, `email`, `activeOrgId`, `sessionId`, and `jti`.
   - Issues **Opaque Refresh Token (30 days)** stored hashed (SHA-256) in PostgreSQL with **token rotation** and reuse detection (if a rotated token is replayed, the entire token family is revoked immediately).
2. **MFA & Session Security:**
   - TOTP (RFC 6238) MFA with encrypted secrets (AES-256-GCM) and single-use backup codes.
   - Redis-backed JWT revocation list (`jti`) for instant logout and forced session termination.
   - Per-organization IP allowlists, password complexity policies, and immutable login history.
3. **Fine-Grained RBAC + ABAC Authorization:**
   - Permission naming convention: `<module>:<resource>:<action>` (e.g., `crm:lead:write`, `finance:invoice:approve`, `ai:agent:execute`).
   - Evaluated via custom Spring Security `PermissionEvaluator` and `@PreAuthorize("@authz.hasPermission('crm:lead:write')")`.
   - Scoped to the active `organization_id`, plus optional record-level ownership/territory/department constraints.

## 2. Multi-Tenant Isolation Design

Zenius One uses a **Shared Database, Shared Schema with Strict Row-Level Isolation** model (ready to split large enterprise tenants into dedicated schemas/clusters later):

1. **Request Resolution (`TenantResolutionFilter`):**
   - Extracts `X-Organization-Id` (or org claim from JWT).
   - Verifies that `userId` holds an `ACTIVE` membership in that `organization_id` (403 Forbidden otherwise).
   - Binds `TenantContext.setOrganizationId(orgId)` in a `ThreadLocal` (and clears it in `finally` block).
2. **Persistence Enforcement:**
   - Every tenant-owned entity extends `TenantScopedEntity` (`organization_id NOT NULL`).
   - JPA `@PrePersist` / `@PreUpdate` automatically stamps and validates `organization_id` against `TenantContext` — preventing cross-tenant writes even if a developer forgets.
   - Hibernate `@Filter(name = "tenantFilter", condition = "organization_id = :organizationId")` automatically appends tenant predicates to every JPQL/Criteria query.
   - PostgreSQL Row-Level Security (`CREATE POLICY tenant_isolation ...`) serves as defense-in-depth at the database engine layer.
3. **Non-Relational & AI Isolation:**
   - **Redis:** Keys prefixed with `zenius:org:{orgId}:...`
   - **MongoDB:** Compound indexes leading with `organizationId` and mandatory repository filter aspect.
   - **Elasticsearch:** Filtered alias or mandatory `term { organizationId }` filter on every search query.
   - **pgvector RAG & Zenius AI:** Every vector search includes `WHERE organization_id = :orgId AND visibility_acl && :userRoles`, ensuring AI never retrieves or leaks another organization's documents or records.

