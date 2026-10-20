package org.example.nongye.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.nongye.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 登录拦截器逻辑测试
 */
@ExtendWith(MockitoExtension.class)
class LoginInterceptorTest {

    @InjectMocks
    private LoginInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Test
    void publicPathShouldPass() throws Exception {
        when(request.getRequestURI()).thenReturn("/trace/TZ123");
        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void loginAndLogoutShouldPass() throws Exception {
        when(request.getRequestURI()).thenReturn("/login");
        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void unauthenticatedUserShouldRedirectToLogin() throws Exception {
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("loginUser")).thenReturn(null);
        assertFalse(interceptor.preHandle(request, response, null));
        verify(response).sendRedirect("/login");
    }

    @Test
    void wrongRoleShouldRedirectToOwnDashboard() throws Exception {
        SysUser farmer = new SysUser();
        farmer.setRole("farmer");
        when(request.getRequestURI()).thenReturn("/admin/users");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("loginUser")).thenReturn(farmer);
        assertFalse(interceptor.preHandle(request, response, null));
        verify(response).sendRedirect("/farmer/dashboard");
    }

    @Test
    void correctRoleShouldPass() throws Exception {
        SysUser admin = new SysUser();
        admin.setRole("admin");
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("loginUser")).thenReturn(admin);
        assertTrue(interceptor.preHandle(request, response, null));
    }
}
