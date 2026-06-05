package com.ncu.chat.model.vo;

import lombok.Data;

@Data
public class SearchUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer onlineStatus;
    private Boolean isFriend;
    private Boolean hasPendingRequest;
    private Long friendshipId;
}
