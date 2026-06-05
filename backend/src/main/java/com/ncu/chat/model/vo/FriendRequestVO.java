package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendRequestVO {
    private Long friendshipId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer onlineStatus;
    private String verificationMessage;
    private LocalDateTime createTime;
}
