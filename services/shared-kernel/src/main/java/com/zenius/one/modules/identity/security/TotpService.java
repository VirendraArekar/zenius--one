package com.zenius.one.modules.identity.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Pure Java RFC 6238 TOTP MFA service (HMAC-SHA1, 6 digits, 30s time step).
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateSecret() {
        byte[] buffer = new byte[20];
        RANDOM.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    public String buildOtpAuthUri(String issuer, String accountEmail, String secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(accountEmail, StandardCharsets.UTF_8);
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                encodedIssuer, encodedAccount, secret, encodedIssuer, DIGITS, TIME_STEP_SECONDS
        );
    }

    public boolean verifyCode(String base32Secret, String code, Instant now) {
        if (base32Secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentBucket = now.getEpochSecond() / TIME_STEP_SECONDS;
        byte[] secretBytes = decodeBase32(base32Secret);
        for (long offset = -1; offset <= 1; offset++) {
            String candidate = generateCodeForBucket(secretBytes, currentBucket + offset);
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String generateCodeForTime(String base32Secret, Instant time) {
        long bucket = time.getEpochSecond() / TIME_STEP_SECONDS;
        return generateCodeForBucket(decodeBase32(base32Secret), bucket);
    }

    private String generateCodeForBucket(byte[] secretBytes, long bucket) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(bucket).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
            byte[] hmac = mac.doFinal(data);

            int offset = hmac[hmac.length - 1] & 0x0F;
            int binary = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to compute TOTP code", ex);
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return out.toString();
    }

    private byte[] decodeBase32(String input) {
        String normalized = input.trim().toUpperCase().replace("=", "");
        byte[] result = new byte[(normalized.length() * 5) / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (int i = 0; i < normalized.length(); i++) {
            int val = BASE32_ALPHABET.indexOf(normalized.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Invalid Base32 character in TOTP secret");
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}

