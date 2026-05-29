package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.service.UserService;
import com.ncu.chat.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterDTO dto) {
        Map<String, Object> result = userService.register(dto);
        return Result.success("注册成功", result);
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody UserLoginDTO dto) {
        Map<String, Object> result = userService.login(dto);
        return Result.success("登录成功", result);
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                userService.updateStatus(userId, 0);
            } catch (Exception e) {
                // token invalid, still return success
            }
        }
        return Result.success("登出成功", null);
    }
}
