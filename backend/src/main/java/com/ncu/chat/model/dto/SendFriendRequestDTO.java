package com.ncu.chat.model.dto;

import lombok.Data;

@Data
public class SendFriendRequestDTO {
    private Long friendId;
    private Long groupId;
    private String verificationMessage;
}
