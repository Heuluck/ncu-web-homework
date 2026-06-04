package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.ChangePasswordDTO;
import com.ncu.chat.model.dto.UserProfileDTO;
import com.ncu.chat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@RequestAttribute("userId") Long userId,
                                                @Valid @RequestBody UserProfileDTO dto) {
        return Result.success(userService.updateProfile(userId, dto));
    }

    @PutMapping("/password")
    public Result<?> changePassword(@RequestAttribute("userId") Long userId,
                                    @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    @PutMapping("/status")
    public Result<?> updateStatus(@RequestAttribute("userId") Long userId,
                                  @RequestBody Map<String, Integer> params) {
        userService.updateStatus(userId, params.get("status"));
        return Result.success("状态更新成功", null);
    }
}
