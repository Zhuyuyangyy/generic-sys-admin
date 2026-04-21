package com.zyy.service;

import com.zyy.config.AppProperties;
import com.zyy.mapper.SysUserMapper;
import com.zyy.model.dto.SysUserLoginDTO;
import com.zyy.model.entity.SysUserEntity;
import com.zyy.model.vo.SysUserLoginVO;
import com.zyy.security.JwtUtil;
import com.zyy.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SysUserService 业务逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SysUserService 业务逻辑测试")
class SysUserServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private AppProperties appProperties;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RbacService rbacService;

    private SysUserServiceImpl userService;
    private SysUserEntity testUser;

    @BeforeEach
    void setUp() throws Exception {
        var securityProps = new AppProperties.SecurityProperties();
        securityProps.setJwtSecret("test-secret-key-must-be-at-least-32-chars-long-for-hs256!");
        securityProps.setJwtExpiration(7200L);
        securityProps.setBcryptCostFactor(10);
        securityProps.setMaxFailedAttempts(5);
        securityProps.setLockoutMinutes(30);
        when(appProperties.getSecurity()).thenReturn(securityProps);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        testUser = new SysUserEntity();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword(encoder.encode("admin123"));
        testUser.setStatus(1);
        testUser.setIsDeleted(0);
        testUser.setRealName("管理员");
        testUser.setFailedAttempts(0);
        testUser.setLockedUntil(null);

        userService = new SysUserServiceImpl(userMapper, appProperties, jwtUtil, rbacService);

        Field encoderField = SysUserServiceImpl.class.getDeclaredField("passwordEncoder");
        encoderField.setAccessible(true);
        encoderField.set(userService, encoder);
    }

    @Test
    @DisplayName("正确用户名和密码登录成功")
    void loginSuccess() {
        SysUserLoginDTO loginDTO = new SysUserLoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("admin123");

        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(rbacService.getRolesByUserId(1L)).thenReturn(Set.of("ROLE_ADMIN"));
        when(rbacService.getPermissionsByUserId(1L)).thenReturn(Set.of("system:user:list"));
        when(jwtUtil.sign(anyLong(), anyString(), any())).thenReturn("mock-access-token");
        when(jwtUtil.refresh(anyLong(), anyString(), any())).thenReturn("mock-refresh-token");

        SysUserLoginVO result = userService.login(loginDTO, "127.0.0.1");

        assertNotNull(result);
        assertEquals("mock-access-token", result.getAccessToken());
        assertEquals("mock-refresh-token", result.getRefreshToken());
        assertNotNull(result.getUser());
        assertEquals("admin", result.getUser().getUsername());
        verify(jwtUtil).sign(eq(1L), eq("admin"), any());
        verify(jwtUtil).refresh(eq(1L), eq("admin"), any());
    }

    @Test
    @DisplayName("用户不存在时登录失败")
    void loginUserNotFound() {
        SysUserLoginDTO loginDTO = new SysUserLoginDTO();
        loginDTO.setUsername("nonexistent");
        loginDTO.setPassword("any");

        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(Exception.class, () -> userService.login(loginDTO, "127.0.0.1"));
    }

    @Test
    @DisplayName("账户被禁用时登录失败")
    void loginAccountDisabled() {
        testUser.setStatus(0);

        SysUserLoginDTO loginDTO = new SysUserLoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("admin123");

        when(userMapper.selectOne(any())).thenReturn(testUser);

        assertThrows(Exception.class, () -> userService.login(loginDTO, "127.0.0.1"));
    }

    @Test
    @DisplayName("密码错误时登录失败")
    void loginWrongPassword() {
        SysUserLoginDTO loginDTO = new SysUserLoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("wrongpassword");

        when(userMapper.selectOne(any())).thenReturn(testUser);

        assertThrows(Exception.class, () -> userService.login(loginDTO, "127.0.0.1"));
    }

    @Test
    @DisplayName("根据用户名查询用户成功")
    void getByUsernameSuccess() {
        when(userMapper.selectOne(any())).thenReturn(testUser);

        var result = userService.getByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    @DisplayName("根据用户名查询用户返回null")
    void getByUsernameNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        var result = userService.getByUsername("nonexistent");

        assertNull(result);
    }
}