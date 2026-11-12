package com.zenius.one.modules.identity.security;

import com.zenius.one.modules.identity.UserEntity;
import com.zenius.one.modules.identity.UserRepository;
import com.zenius.one.modules.integration.ApiKeyEntity;
import com.zenius.one.modules.integration.ApiKeyRepository;
import com.zenius.one.modules.organization.OrgMembershipEntity;
import com.zenius.one.modules.organization.OrgMembershipRepository;
import com.zenius.one.modules.organization.PermissionEntity;
import com.zenius.one.platform.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates Bearer JWT or X-Api-Key credentials, validates active organization membership,
 * and binds TenantContext strictly for the lifetime of the request.
 */
@Component
public class JwtAndApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final OrgMembershipRepository membershipRepository;
    private final ApiKeyRepository apiKeyRepository;

    public JwtAndApiKeyAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            OrgMembershipRepository membershipRepository,
            ApiKeyRepository apiKeyRepository
    ) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            String apiKeyHeader = request.getHeader("X-Api-Key");
            String orgHeader = request.getHeader("X-Organization-Id");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (!authenticateJwt(token, orgHeader, request, response)) {
                    return;
                }
            } else if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
                if (!authenticateApiKey(apiKeyHeader.trim(), orgHeader, request, response)) {
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean authenticateJwt(
            String token,
            String orgHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        JwtTokenService.ParsedJwt parsed;
        try {
            parsed = jwtTokenService.parseAndVerify(token, "ACCESS");
        } catch (Exception ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired access token");
            return false;
        }

        UserEntity user = userRepository.findById(parsed.userId()).orElse(null);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "User account inactive or missing");
            return false;
        }

        UUID targetOrgId = resolveTargetOrgId(orgHeader, parsed.organizationId(), response);
        if (orgHeader != null && !orgHeader.isBlank() && targetOrgId == null) {
            return false;
        }

        Set<String> permissions = new HashSet<>();
        if (targetOrgId != null) {
            OrgMembershipEntity membership = membershipRepository
                    .findByOrganizationIdAndUserId(targetOrgId, user.getId())
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .orElse(null);
            if (membership == null) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "User is not an active member of target organization");
                return false;
            }
            permissions = membership.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(PermissionEntity::getPermissionKey)
                    .collect(Collectors.toSet());
            TenantContext.setOrganizationId(targetOrgId);
        }

        ZeniusUserPrincipal principal = new ZeniusUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                targetOrgId,
                permissions
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return true;
    }

    private boolean authenticateApiKey(
            String rawKey,
            String orgHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String keyHash = TokenHashing.sha256Hex(rawKey);
        ApiKeyEntity key = apiKeyRepository.findByKeyHash(keyHash).orElse(null);
        Instant now = Instant.now();
        if (key == null || !key.isActive(now)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or revoked API key");
            return false;
        }

        if (orgHeader != null && !orgHeader.isBlank()) {
            UUID requestedOrg = parseUuid(orgHeader, response);
            if (requestedOrg == null) {
                return false;
            }
            if (!requestedOrg.equals(key.getOrganizationId())) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "API key does not belong to target organization");
                return false;
            }
        }

        key.setLastUsedAt(now);
        apiKeyRepository.save(key);

        Set<String> scopes = Arrays.stream(key.getScopes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        TenantContext.setOrganizationId(key.getOrganizationId());
        ZeniusUserPrincipal principal = new ZeniusUserPrincipal(
                key.getCreatedByUserId(),
                "apikey:" + key.getKeyPrefix(),
                key.getName(),
                key.getOrganizationId(),
                scopes
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        return true;
    }

    private UUID resolveTargetOrgId(String orgHeader, UUID jwtOrgId, HttpServletResponse response) throws IOException {
        if (orgHeader != null && !orgHeader.isBlank()) {
            return parseUuid(orgHeader, response);
        }
        return jwtOrgId;
    }

    private UUID parseUuid(String raw, HttpServletResponse response) throws IOException {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-Organization-Id UUID header");
            return null;
        }
    }
}

