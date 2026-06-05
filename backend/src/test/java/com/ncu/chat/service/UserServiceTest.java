package com.ncu.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncu.chat.mapper.FriendGroupMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserProfileDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.service.impl.UserServiceImpl;
import com.ncu.chat.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FriendGroupMapper friendGroupMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNickname("测试用户");
        testUser.setStatus(1);
        testUser.setRole(0);
        testUser.setEnabled(1);
        testUser.setDeleted(0);
        testUser.setCreateTime(LocalDateTime.now());
    }

    @Test
    void register_success() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setNickname("新用户");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(friendGroupMapper.insert(any())).thenReturn(1);
        when(jwtUtil.generateToken(any(), any())).thenReturn("test-token");

        var result = userService.register(dto);

        assertNotNull(result);
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_passwordMismatch() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("different");
        dto.setNickname("新用户");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(dto));
        assertEquals("两次密码不一致", exception.getMessage());
    }

    @Test
    void register_usernameExists() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("existinguser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setNickname("用户");

        when(userMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(dto));
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void login_success() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(jwtUtil.generateToken(any(), any())).thenReturn("test-token");

        var result = userService.login(dto);

        assertNotNull(result);
        assertNotNull(result.get("token"));
        assertEquals(1, testUser.getStatus());
    }

    @Test
    void login_userNotFound() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("nonexistent");
        dto.setPassword("password123");

        when(userMapper.selectOne(any())).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(dto));
        assertEquals("用户名不存在", exception.getMessage());
    }

    @Test
    void login_wrongPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");

        when(userMapper.selectOne(any())).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(dto));
        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    void login_disabledUser() {
        testUser.setEnabled(0);
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        when(userMapper.selectOne(any())).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(dto));
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    void getProfile_success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        UserProfileDTO result = userService.getProfile(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("测试用户", result.getNickname());
    }

    @Test
    void getProfile_userNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.getProfile(999L));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void updateProfile_success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setNickname("新昵称");
        dto.setSignature("新签名");

        UserProfileDTO result = userService.updateProfile(1L, dto);

        assertNotNull(result);
        assertEquals("新昵称", result.getNickname());
    }

    @Test
    void changePassword_success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.changePassword(1L, "password123", "newpassword123"));
    }

    @Test
    void changePassword_wrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.changePassword(1L, "wrongpassword", "newpassword123"));
        assertEquals("原密码错误", exception.getMessage());
    }

    @Test
    void updateStatus_success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.updateStatus(1L, 2));
        assertEquals(2, testUser.getStatus());
    }
}
