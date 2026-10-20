package org.example.nongye.common;

import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.ComplaintMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

/**
 * 全局模型注入：所有页面自动获得顶栏铃铛所需数据
 * 仅管理员查询待处理投诉数，其他角色不注入
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAdvice.class);

    @Autowired
    private ComplaintMapper complaintMapper;

    @ModelAttribute
    public void addAttributes(HttpSession session, Model model) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user != null && "admin".equals(user.getRole())) {
            try {
                model.addAttribute("pendingComplaints", complaintMapper.countPending());
            } catch (Exception e) {
                log.warn("查询待处理投诉数失败", e);
            }
        }
    }
}
