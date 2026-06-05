package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoiceMessageVO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private String fileUrl;
    private Integer duration;
    private LocalDateTime createTime;
    private String senderNickname;
    private String senderAvatar;
}
