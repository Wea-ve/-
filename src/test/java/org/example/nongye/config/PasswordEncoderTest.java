package org.example.nongye.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码编码器测试：验证 BCrypt 编码/校验逻辑
 */
class PasswordEncoderTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Test
    void encodeAndMatch() {
        String hash = encoder.encode("123456");
        assertTrue(encoder.matches("123456", hash), "正确密码应匹配");
        assertFalse(encoder.matches("wrong", hash), "错误密码不应匹配");
    }

    @Test
    void eachHashIsUniqueDueToSalt() {
        String h1 = encoder.encode("123456");
        String h2 = encoder.encode("123456");
        assertNotEquals(h1, h2, "BCrypt 每次哈希应不同（随机盐）");
        assertTrue(encoder.matches("123456", h1));
        assertTrue(encoder.matches("123456", h2));
    }

    @Test
    void hashStartsWithBcryptMarker() {
        String hash = encoder.encode("secret");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                "BCrypt 哈希应以 $2 开头");
    }
}
