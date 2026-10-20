package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;      // farmer/dealer/admin
    private String phone;
    private String address;
    private String avatar;    // 头像图片访问路径，如 /uploads/avatar/xxx.jpg
    private Integer status;
    private LocalDateTime createTime;
}
