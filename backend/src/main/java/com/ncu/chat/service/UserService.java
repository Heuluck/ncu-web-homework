package com.ncu.chat.service;

import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserProfileDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.model.entity.User;

import java.util.Map;

public interface UserService {
    Map<String, Object> register(UserRegisterDTO dto);
    Map<String, Object> login(UserLoginDTO dto);
    UserProfileDTO getProfile(Long userId);
    UserProfileDTO updateProfile(Long userId, UserProfileDTO dto);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void updateStatus(Long userId, Integer status);
    User getUserById(Long userId);
}
