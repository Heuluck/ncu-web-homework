package com.ncu.chat.model.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer status;
    private Integer role;
}
