package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.SysUser;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND password = #{password} AND deleted = 0")
    SysUser login(@Param("username") String username, @Param("password") String password);

    /**
     * 按用户名查询（密码校验交由应用层用 BCrypt 完成）
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted = 0")
    SysUser selectById(Long id);

    @Select("SELECT * FROM sys_user WHERE role = #{role} AND deleted = 0")
    java.util.List<SysUser> selectByRole(String role);

    @Select("SELECT * FROM sys_user WHERE role != 'admin' AND deleted = 0 ORDER BY id DESC")
    java.util.List<SysUser> selectAllUsers();

    /**
     * 按账号/姓名/手机号模糊搜索，可按角色筛选（farmer/dealer/null=全部）
     */
    @Select("<script>SELECT * FROM sys_user WHERE role != 'admin' AND deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR real_name LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> " +
            "<if test='role != null and role != \"\"'>" +
            "AND role = #{role} " +
            "</if> ORDER BY id DESC</script>")
    java.util.List<SysUser> searchUsers(@Param("keyword") String keyword, @Param("role") String role);

    /**
     * 查询所有用户（含管理员），用于密码迁移
     */
    @Select("SELECT * FROM sys_user WHERE deleted = 0")
    java.util.List<SysUser> selectAll();

    /**
     * 更新密码
     */
    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 软删除用户
     */
    @Update("UPDATE sys_user SET deleted = 1 WHERE id = #{id}")
    int softDelete(Long id);

    /**
     * 修改个人信息（姓名/电话/地址）
     */
    @Update("UPDATE sys_user SET real_name = #{realName}, phone = #{phone}, address = #{address} WHERE id = #{id}")
    int updateProfile(SysUser user);

    /**
     * 更新头像
     */
    @Update("UPDATE sys_user SET avatar = #{avatar} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    /**
     * 新增账号（管理员创建农户/经销商，密码需先由 PasswordHelper 加密）
     */
    @Insert("INSERT INTO sys_user (username, password, real_name, role, phone, address, status) " +
            "VALUES (#{username}, #{password}, #{realName}, #{role}, #{phone}, #{address}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);
}
