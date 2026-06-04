package com.ncu.chat.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupVO {
    private Long id;
    private String name;
    private String avatar;
    private String announcement;
    private Long ownerId;
    private String ownerNickname;
    private Integer memberCount;
    private Integer maxMembers;
    private Integer myRole;
    private Boolean isDoNotDisturb;
    private LocalDateTime createTime;
}