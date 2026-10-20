package org.example.nongye.common;

import cn.hutool.crypto.SecureUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具：统一明文密码的校验与加密规则
 * 存储形式：BCrypt( MD5(明文) )；兼容历史裸 MD5
 */
@Component
public class PasswordHelper {

    private final PasswordEncoder passwordEncoder;

    public PasswordHelper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 校验明文密码是否与库中哈希匹配
     */
    public boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        String md5 = SecureUtil.md5(rawPassword);
        if (isBcrypt(stored)) {
            return passwordEncoder.matches(md5, stored);
        }
        // 兼容旧裸 MD5
        return stored.equalsIgnoreCase(md5);
    }

    /**
     * 对明文密码加密，返回可入库的 BCrypt 哈希
     */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(SecureUtil.md5(rawPassword));
    }

    /**
     * 库中密码是否已是 BCrypt 格式
     */
    public boolean isBcrypt(String stored) {
        return stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }
}
