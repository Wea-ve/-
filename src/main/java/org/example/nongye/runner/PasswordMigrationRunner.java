package org.example.nongye.runner;

import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时自动迁移旧 MD5 密码为 BCrypt 哈希（幂等执行）
 * <p>
 * 判定规则：BCrypt 哈希固定以 "$2" 开头，长度约 60；MD5 为 32 位十六进制。
 * 已是 BCrypt 的跳过，MD5 重新加密并更新。
 */
@Component
@Order(10)
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<SysUser> users = userMapper.selectAll();
        int migrated = 0;
        for (SysUser user : users) {
            String pwd = user.getPassword();
            if (pwd == null || pwd.isEmpty()) {
                continue;
            }
            // 已经是 BCrypt 哈希则跳过
            if (isBcrypt(pwd)) {
                continue;
            }
            // 旧 MD5：无法还原明文，但测试数据明文已知为 123456。
            // 这里采用"对旧 MD5 哈希值再做 BCrypt 编码"的方式，保证兼容登录：
            // 登录时会先对输入做 MD5 再与 BCrypt 比对，迁移后仍可登录。
            // 为彻底杜绝 MD5，改为：测试库重建后密码已是 BCrypt（见 schema.sql）。
            // 对于仍为 MD5 的存量数据，这里做 BCrypt(原MD5值) 迁移。
            String newHash = passwordEncoder.encode(pwd);
            userMapper.updatePassword(user.getId(), newHash);
            migrated++;
        }
        if (migrated > 0) {
            log.info("密码安全迁移完成：共迁移 {} 个用户由 MD5 升级为 BCrypt", migrated);
        }
    }

    private boolean isBcrypt(String pwd) {
        return pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$");
    }
}
