package com.zenius.one.modules.identity;

import com.zenius.one.modules.identity.AuthService.AuthSessionResponse;
import com.zenius.one.modules.identity.AuthService.LoginRequest;
import com.zenius.one.modules.identity.AuthService.LoginResult;
import com.zenius.one.modules.identity.AuthService.MfaLoginVerifyRequest;
import com.zenius.one.modules.identity.AuthService.MfaSetupResponse;
import com.zenius.one.modules.identity.AuthService.RegisterRequest;
import com.zenius.one.modules.identity.AuthService.SessionDto;
import com.zenius.one.modules.identity.AuthService.UserProfileDto;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Identity & Authentication", description = "SSO registration, JWT rotation, MFA, and active session control")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user and provision their initial organization")
    public ApiResponse<AuthSessionResponse> register(
            @Valid @RequestBody RegisterBody body,
            HttpServletRequest request
    ) {
        var req = new RegisterRequest(
                body.fullName(),
                body.email(),
                body.password(),
                body.jobTitle(),
                body.organizationName(),
                body.organizationSlug(),
                body.industry(),
                body.baseCurrency(),
                body.timezone()
        );
        return ApiResponse.ok(authService.register(req, request.getHeader("User-Agent"), request.getRemoteAddr()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password (or receive MFA challenge)")
    public ApiResponse<LoginResult> login(
            @Valid @RequestBody LoginBody body,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(authService.login(
                new LoginRequest(body.email(), body.password()),
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        ));
    }

    @PostMapping("/mfa/verify-login")
    @Operation(summary = "Complete MFA login using challenge token and 6-digit TOTP code")
    public ApiResponse<AuthSessionResponse> verifyMfaLogin(
            @Valid @RequestBody MfaVerifyLoginBody body,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(authService.verifyMfaLogin(
                new MfaLoginVerifyRequest(body.mfaToken(), body.code()),
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a fresh Access JWT")
    public ApiResponse<AuthSessionResponse> refresh(
            @Valid @RequestBody RefreshBody body,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(authService.refresh(
                body.refreshToken(),
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token session")
    public ApiResponse<Map<String, Boolean>> logout(@RequestBody(required = false) RefreshBody body) {
        authService.logout(body != null ? body.refreshToken() : null);
        return ApiResponse.ok(Map.of("loggedOut", true));
    }

    @GetMapping("/me")
    @Operation(summary = "Fetch current authenticated user profile, organizations, and active permissions")
    public ApiResponse<UserProfileDto> me(@AuthenticationPrincipal ZeniusUserPrincipal principal) {
        return ApiResponse.ok(authService.getProfile(principal));
    }

    @PostMapping("/mfa/setup")
    @Operation(summary = "Generate a TOTP secret and otpauth URI for MFA enrollment")
    public ApiResponse<MfaSetupResponse> setupMfa(@AuthenticationPrincipal ZeniusUserPrincipal principal) {
        return ApiResponse.ok(authService.setupMfa(principal));
    }

    @PostMapping("/mfa/enable")
    @Operation(summary = "Verify TOTP code and enable MFA for the current user")
    public ApiResponse<Map<String, Boolean>> enableMfa(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody MfaCodeBody body
    ) {
        authService.enableMfa(principal, body.code());
        return ApiResponse.ok(Map.of("mfaEnabled", true));
    }

    @PostMapping("/mfa/disable")
    @Operation(summary = "Verify TOTP code and disable MFA for the current user")
    public ApiResponse<Map<String, Boolean>> disableMfa(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody MfaCodeBody body
    ) {
        authService.disableMfa(principal, body.code());
        return ApiResponse.ok(Map.of("mfaEnabled", false));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active refresh-token sessions for the current user")
    public ApiResponse<List<SessionDto>> sessions(@AuthenticationPrincipal ZeniusUserPrincipal principal) {
        return ApiResponse.ok(authService.listActiveSessions(principal));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Revoke a specific active session")
    public ApiResponse<Map<String, Boolean>> revokeSession(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        authService.revokeSession(principal, id);
        return ApiResponse.ok(Map.of("revoked", true));
    }

    public record RegisterBody(
            @NotBlank @Size(min = 2, max = 150) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            String jobTitle,
            @NotBlank @Size(min = 2, max = 150) String organizationName,
            String organizationSlug,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }

    public record LoginBody(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record MfaVerifyLoginBody(
            @NotBlank String mfaToken,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code
    ) {
    }

    public record MfaCodeBody(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code
    ) {
    }

    public record RefreshBody(@NotBlank String refreshToken) {
    }
}

