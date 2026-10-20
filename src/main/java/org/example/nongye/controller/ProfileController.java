package org.example.nongye.controller;

import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.PasswordHelper;
import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * 个人中心（管理员 / 农户 / 经销商通用）
 * 账号只读，可改姓名/电话/地址、上传头像、修改密码
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final SysUserMapper userMapper;
    private final PasswordHelper passwordHelper;

    @Value("${upload.path}")
    private String uploadPath;

    public ProfileController(SysUserMapper userMapper, PasswordHelper passwordHelper) {
        this.userMapper = userMapper;
        this.passwordHelper = passwordHelper;
    }

    /**
     * 取当前登录用户；未登录返回 null（由调用方决定跳转）
     */
    private SysUser current(HttpSession session) {
        return (SysUser) session.getAttribute("loginUser");
    }

    // ==================== 个人信息 ====================

    @GetMapping
    public String info(HttpSession session, Model model) {
        SysUser user = current(session);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", userMapper.selectById(user.getId()));
        return "profile/index";
    }

    @PostMapping
    public String updateInfo(@RequestParam String realName,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String address,
                             HttpSession session) {
        SysUser user = current(session);
        if (user == null) {
            return "redirect:/login";
        }
        user.setRealName(realName);
        user.setPhone(phone);
        user.setAddress(address);
        userMapper.updateProfile(user);
        // 刷新 session 中的用户信息
        SysUser fresh = userMapper.selectById(user.getId());
        session.setAttribute("loginUser", fresh);
        log.info("用户[{}]更新个人信息", fresh.getUsername());
        return "redirect:/profile?success=1";
    }

    // ==================== 头像上传 ====================

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) throws Exception {
        SysUser user = current(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (file == null || file.isEmpty()) {
            return "redirect:/profile?error=empty";
        }
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf('.'))
                : "";
        if (!ext.toLowerCase().matches("\\.(png|jpg|jpeg|gif)")) {
            return "redirect:/profile?error=type";
        }
        File dir = new File(uploadPath, "avatar");
        dir.mkdirs();
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        // 必须用绝对路径：MultipartFile.transferTo 的相对路径会被解析到 Tomcat 临时目录
        File dest = new File(dir, fileName).getAbsoluteFile();
        file.transferTo(dest);

        String url = "/uploads/avatar/" + fileName;
        userMapper.updateAvatar(user.getId(), url);

        SysUser fresh = userMapper.selectById(user.getId());
        session.setAttribute("loginUser", fresh);
        log.info("用户[{}]更新头像", fresh.getUsername());
        return "redirect:/profile?success=1";
    }

    // ==================== 修改密码 ====================

    @GetMapping("/password")
    public String passwordPage(HttpSession session) {
        if (current(session) == null) {
            return "redirect:/login";
        }
        return "profile/password";
    }

    @PostMapping("/password")
    public String updatePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session, Model model) {
        SysUser user = current(session);
        if (user == null) {
            return "redirect:/login";
        }
        SysUser dbUser = userMapper.selectById(user.getId());
        if (!passwordHelper.matches(oldPassword, dbUser.getPassword())) {
            model.addAttribute("error", "原密码不正确");
            return "profile/password";
        }
        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("error", "新密码长度至少 6 位");
            return "profile/password";
        }
        userMapper.updatePassword(user.getId(), passwordHelper.encode(newPassword));
        log.info("用户[{}]修改登录密码", user.getUsername());
        return "redirect:/profile/password?success=1";
    }
}
