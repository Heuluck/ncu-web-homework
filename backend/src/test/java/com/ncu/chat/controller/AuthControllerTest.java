package com.ncu.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.service.UserService;
import com.ncu.chat.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_success() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setNickname("新用户");

        Map<String, Object> result = new HashMap<>();
        result.put("token", "test-token");
        result.put("user", new HashMap<>());

        when(userService.register(any(UserRegisterDTO.class))).thenReturn(result);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.token").value("test-token"));
    }

    @Test
    void register_invalidInput() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("ab"); // 太短
        dto.setPassword("123"); // 太短
        dto.setConfirmPassword("123");
        dto.setNickname("");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void login_success() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        Map<String, Object> result = new HashMap<>();
        result.put("token", "test-token");
        result.put("user", new HashMap<>());

        when(userService.login(any(UserLoginDTO.class))).thenReturn(result);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"));
    }

    @Test
    void login_userNotFound() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("nonexistent");
        dto.setPassword("password123");

        when(userService.login(any(UserLoginDTO.class)))
                .thenThrow(new RuntimeException("用户名不存在"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("用户名不存在"));
    }

    @Test
    void login_wrongPassword() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");

        when(userService.login(any(UserLoginDTO.class)))
                .thenThrow(new RuntimeException("密码错误"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }
}
