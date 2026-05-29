package com.ncu.chat.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;

    @Size(max = 20, message = "昵称最长 20 个字符")
    private String nickname;

    private String avatar;

    @Size(max = 50, message = "个性签名最长 50 个字符")
    private String signature;

    private Integer status;
    private Integer role;
}
