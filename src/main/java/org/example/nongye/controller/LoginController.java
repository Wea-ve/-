package org.example.nongye.controller;

import cn.hutool.crypto.SecureUtil;
import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(String username, String password, String role, HttpSession session, Model model) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            model.addAttribute("error", "账号或密码错误");
            return "login";
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            model.addAttribute("error", "账号已禁用");
            return "login";
        }
        if (!checkPassword(password, user)) {
            model.addAttribute("error", "账号或密码错误");
            return "login";
        }
        // 回传所选角色，登录页据此保持当前通道高亮
        model.addAttribute("selectedRole", role);
        // 分角色登录：所选通道与账号实际角色必须一致，互不干扰
        if (role == null || !role.equals(user.getRole())) {
            model.addAttribute("error", "该账号不属于当前登录通道，请选择正确的角色入口");
            return "login";
        }
        session.setAttribute("loginUser", user);
        log.info("用户登录成功：{}({})", username, user.getRole());
        switch (user.getRole()) {
            case "farmer":  return "redirect:/farmer/dashboard";
            case "dealer":  return "redirect:/dealer/dashboard";
            case "admin":   return "redirect:/admin/dashboard";
            default:        return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Object u = session.getAttribute("loginUser");
        if (u instanceof SysUser user) {
            log.info("用户登出：{}", user.getUsername());
        }
        session.invalidate();
        return "redirect:/login";
    }

    /**
     * 密码校验：优先 BCrypt，兼容未迁移的裸 MD5（登录成功后自动迁移）
     */
    private boolean checkPassword(String rawPassword, SysUser user) {
        String stored = user.getPassword();
        if (isBcrypt(stored)) {
            // 已迁移：明文先 MD5 再与 BCrypt 比对（与迁移逻辑一致）
            String md5 = SecureUtil.md5(rawPassword);
            return passwordEncoder.matches(md5, stored);
        }
        // 兼容旧裸 MD5
        boolean ok = stored.equalsIgnoreCase(SecureUtil.md5(rawPassword));
        if (ok) {
            // 登录即迁移，避免数据库留存裸 MD5
            userMapper.updatePassword(user.getId(), passwordEncoder.encode(SecureUtil.md5(rawPassword)));
            log.info("用户 {} 密码已自动迁移为 BCrypt", user.getUsername());
        }
        return ok;
    }

    private boolean isBcrypt(String pwd) {
        return pwd != null && (pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"));
    }
}
