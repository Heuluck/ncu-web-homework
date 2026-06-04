package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private String groupNickname;
    private Integer status;
    private LocalDateTime joinTime;
}