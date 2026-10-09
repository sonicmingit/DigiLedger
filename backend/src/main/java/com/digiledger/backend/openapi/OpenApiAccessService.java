package com.digiledger.backend.openapi;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.OpenApiAccessMapper;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class OpenApiAccessService {
    private final OpenApiAccessMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public OpenApiAccessService(OpenApiAccessMapper mapper) { this.mapper = mapper; }

    public OpenApiAccess get() { return mapper.get(); }

    public void update(boolean enabled, boolean allowWrite) {
        OpenApiAccess access = get();
        if (enabled && (access == null || access.getTokenHash() == null)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请先生成访问 Token");
        }
        mapper.update(enabled, allowWrite);
    }

    public String rotate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = "dl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        mapper.rotate(hash(token), token.substring(0, 11));
        return token;
    }

    /** Returns an HTTP status; zero means access is allowed. */
    public int authorize(String authorization, boolean write) {
        OpenApiAccess access = get();
        if (access == null || !access.isEnabled()) return 403;
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return 401;
        String token = authorization.substring(7).trim();
        if (token.isEmpty() || access.getTokenHash() == null || !MessageDigest.isEqual(
                hash(token).getBytes(StandardCharsets.US_ASCII),
                access.getTokenHash().getBytes(StandardCharsets.US_ASCII))) return 401;
        return write && !access.isAllowWrite() ? 403 : 0;
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
