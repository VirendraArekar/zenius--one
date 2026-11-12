package com.zenius.one.modules.identity;

import com.zenius.one.modules.identity.security.JwtTokenService;
import com.zenius.one.modules.identity.security.TokenHashing;
import com.zenius.one.modules.identity.security.TotpService;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.modules.organization.OrgMembershipEntity;
import com.zenius.one.modules.organization.OrgMembershipRepository;
import com.zenius.one.modules.organization.OrganizationEntity;
import com.zenius.one.modules.organization.OrganizationProvisioningService;
import com.zenius.one.modules.organization.OrganizationRepository;
import com.zenius.one.modules.organization.PermissionEntity;
import com.zenius.one.platform.common.ResourceNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository membershipRepository;
    private final OrganizationProvisioningService provisioningService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TotpService totpService;
    private final AuditLogService auditLogService;
    private final long refreshTokenDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            OrganizationRepository organizationRepository,
            OrgMembershipRepository membershipRepository,
            OrganizationProvisioningService provisioningService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            TotpService totpService,
            AuditLogService auditLogService,
            @Value("${zenius.security.jwt.refresh-token-days:30}") long refreshTokenDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.provisioningService = provisioningService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.totpService = totpService;
        this.auditLogService = auditLogService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthSessionResponse register(RegisterRequest req, String userAgent, String ipAddress) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setJobTitle(req.jobTitle() != null ? req.jobTitle().trim() : "Founder");
        user.setStatus("ACTIVE");
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);

        OrganizationEntity org = provisioningService.createOrganizationWithDefaults(
                user.getId(),
                user.getEmail(),
                req.organizationName(),
                req.organizationSlug(),
                req.industry(),
                req.baseCurrency(),
                req.timezone()
        );

        return issueSession(user, org.getId(), null, userAgent, ipAddress);
    }

    @Transactional
    public LoginResult login(LoginRequest req, String userAgent, String ipAddress) {
        String email = req.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!"ACTIVE".equals(user.getStatus()) || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.isMfaEnabled()) {
            String mfaToken = jwtTokenService.generateMfaChallengeToken(user.getId(), user.getEmail());
            return LoginResult.mfaRequired(mfaToken);
        }

        UUID defaultOrgId = resolveDefaultOrganizationId(user.getId());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditLogService.record(
                defaultOrgId,
                user.getId(),
                user.getEmail(),
                "AUTH_LOGIN",
                "SESSION",
                user.getId().toString(),
                "User signed in via password authentication",
                "SUCCESS"
        );

        return LoginResult.authenticated(issueSession(user, defaultOrgId, null, userAgent, ipAddress));
    }

    @Transactional
    public AuthSessionResponse verifyMfaLogin(MfaLoginVerifyRequest req, String userAgent, String ipAddress) {
        JwtTokenService.ParsedJwt parsed;
        try {
            parsed = jwtTokenService.parseAndVerify(req.mfaToken(), "MFA_CHALLENGE");
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid or expired MFA challenge token");
        }

        UserEntity user = userRepository.findById(parsed.userId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.isMfaEnabled() || !totpService.verifyCode(user.getMfaSecret(), req.code(), Instant.now())) {
            throw new BadCredentialsException("Invalid MFA verification code");
        }

        UUID defaultOrgId = resolveDefaultOrganizationId(user.getId());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditLogService.record(
                defaultOrgId,
                user.getId(),
                user.getEmail(),
                "AUTH_LOGIN_MFA",
                "SESSION",
                user.getId().toString(),
                "User completed MFA-verified sign in",
                "SUCCESS"
        );

        return issueSession(user, defaultOrgId, null, userAgent, ipAddress);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthSessionResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        String hash = TokenHashing.sha256Hex(rawRefreshToken);
        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        Instant now = Instant.now();
        if (existing.getRevokedAt() != null) {
            // Token reuse detected: revoke entire family immediately
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), now);
            throw new BadCredentialsException("Refresh token reuse detected; session family revoked");
        }
        if (existing.getExpiresAt().isBefore(now)) {
            throw new BadCredentialsException("Refresh token expired");
        }

        UserEntity user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        UUID orgId = existing.getOrganizationId() != null
                ? existing.getOrganizationId()
                : resolveDefaultOrganizationId(user.getId());

        AuthSessionResponse next = issueSession(user, orgId, existing.getFamilyId(), userAgent, ipAddress);
        existing.setRevokedAt(now);
        existing.setReplacedByTokenId(next.sessionId());
        refreshTokenRepository.save(existing);
        return next;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = TokenHashing.sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public MfaSetupResponse setupMfa(ZeniusUserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);
        String otpAuthUri = totpService.buildOtpAuthUri("Zenius One", user.getEmail(), secret);
        return new MfaSetupResponse(secret, otpAuthUri);
    }

    @Transactional
    public void enableMfa(ZeniusUserPrincipal principal, String code) {
        UserEntity user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getMfaSecret() == null || !totpService.verifyCode(user.getMfaSecret(), code, Instant.now())) {
            throw new IllegalArgumentException("Invalid TOTP verification code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditLogService.record(
                principal.getActiveOrganizationId(),
                user.getId(),
                user.getEmail(),
                "MFA_ENABLED",
                "USER",
                user.getId().toString(),
                "TOTP Multi-Factor Authentication enabled",
                "SUCCESS"
        );
    }

    @Transactional
    public void disableMfa(ZeniusUserPrincipal principal, String code) {
        UserEntity user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isMfaEnabled() || !totpService.verifyCode(user.getMfaSecret(), code, Instant.now())) {
            throw new IllegalArgumentException("Invalid TOTP verification code");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditLogService.record(
                principal.getActiveOrganizationId(),
                user.getId(),
                user.getEmail(),
                "MFA_DISABLED",
                "USER",
                user.getId().toString(),
                "TOTP Multi-Factor Authentication disabled",
                "SUCCESS"
        );
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(ZeniusUserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildUserProfile(user, principal.getActiveOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<SessionDto> listActiveSessions(ZeniusUserPrincipal principal) {
        return refreshTokenRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(principal.getUserId())
                .stream()
                .filter(t -> t.isActive(Instant.now()))
                .map(t -> new SessionDto(
                        t.getId(),
                        t.getOrganizationId(),
                        t.getUserAgent(),
                        t.getIpAddress(),
                        t.getCreatedAt(),
                        t.getExpiresAt()
                ))
                .toList();
    }

    @Transactional
    public void revokeSession(ZeniusUserPrincipal principal, UUID sessionId) {
        RefreshTokenEntity token = refreshTokenRepository.findById(sessionId)
                .filter(t -> t.getUserId().equals(principal.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    public AuthSessionResponse issueSession(
            UserEntity user,
            UUID activeOrgId,
            UUID existingFamilyId,
            String userAgent,
            String ipAddress
    ) {
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), activeOrgId);
        String rawRefreshToken = TokenHashing.generateOpaqueToken(48);

        RefreshTokenEntity refreshEntity = new RefreshTokenEntity();
        refreshEntity.setUserId(user.getId());
        refreshEntity.setOrganizationId(activeOrgId);
        refreshEntity.setFamilyId(existingFamilyId != null ? existingFamilyId : UUID.randomUUID());
        refreshEntity.setTokenHash(TokenHashing.sha256Hex(rawRefreshToken));
        refreshEntity.setUserAgent(truncate(userAgent, 250));
        refreshEntity.setIpAddress(truncate(ipAddress, 64));
        refreshEntity.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        refreshEntity = refreshTokenRepository.save(refreshEntity);

        return new AuthSessionResponse(
                refreshEntity.getId(),
                accessToken,
                rawRefreshToken,
                "Bearer",
                buildUserProfile(user, activeOrgId)
        );
    }

    public UserProfileDto buildUserProfile(UserEntity user, UUID requestedOrgId) {
        List<OrganizationEntity> orgs = organizationRepository.findActiveOrganizationsForUser(user.getId());
        UUID activeOrgId = requestedOrgId;
        if (activeOrgId == null && !orgs.isEmpty()) {
            activeOrgId = orgs.getFirst().getId();
        }

        Set<String> permissions = Set.of();
        if (activeOrgId != null) {
            permissions = membershipRepository.findByOrganizationIdAndUserId(activeOrgId, user.getId())
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .map(m -> m.getRoles().stream()
                            .flatMap(r -> r.getPermissions().stream())
                            .map(PermissionEntity::getPermissionKey)
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
        }

        List<OrganizationSummary> orgSummaries = orgs.stream()
                .map(o -> new OrganizationSummary(
                        o.getId(),
                        o.getSlug(),
                        o.getName(),
                        o.getIndustry(),
                        o.getBaseCurrency(),
                        o.getTimezone()
                ))
                .toList();

        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getJobTitle(),
                user.isMfaEnabled(),
                activeOrgId,
                permissions,
                orgSummaries
        );
    }

    private UUID resolveDefaultOrganizationId(UUID userId) {
        List<OrgMembershipEntity> memberships = membershipRepository.findByUserIdAndStatusOrderByJoinedAtAsc(
                userId,
                "ACTIVE"
        );
        return memberships.isEmpty() ? null : memberships.getFirst().getOrganizationId();
    }

    private String truncate(String val, int max) {
        if (val == null) return null;
        return val.length() <= max ? val : val.substring(0, max);
    }

    public record RegisterRequest(
            String fullName,
            String email,
            String password,
            String jobTitle,
            String organizationName,
            String organizationSlug,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record MfaLoginVerifyRequest(String mfaToken, String code) {
    }

    public record MfaSetupResponse(String secret, String otpAuthUri) {
    }

    public record OrganizationSummary(
            UUID id,
            String slug,
            String name,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }

    public record UserProfileDto(
            UUID id,
            String email,
            String fullName,
            String jobTitle,
            boolean mfaEnabled,
            UUID activeOrganizationId,
            Set<String> permissions,
            List<OrganizationSummary> organizations
    ) {
    }

    public record AuthSessionResponse(
            UUID sessionId,
            String accessToken,
            String refreshToken,
            String tokenType,
            UserProfileDto user
    ) {
    }

    public record LoginResult(
            boolean mfaRequired,
            String mfaToken,
            AuthSessionResponse session
    ) {
        public static LoginResult mfaRequired(String mfaToken) {
            return new LoginResult(true, mfaToken, null);
        }

        public static LoginResult authenticated(AuthSessionResponse session) {
            return new LoginResult(false, null, session);
        }
    }

    public record SessionDto(
            UUID id,
            UUID organizationId,
            String userAgent,
            String ipAddress,
            Instant createdAt,
            Instant expiresAt
    ) {
    }
}

