package com.ncu.chat.model.vo;

import lombok.Data;

@Data
public class FriendVO {
    private Long friendshipId;
    private Long friendId;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer onlineStatus;
    private Long groupId;
    private String groupName;
    private Integer status; // 好友关系状态（1=正常）
    private String blockStatus; // none / blocked_by_me / blocked_by_them / both
}
