package com.ncu.chat.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class FriendGroupVO {
    private Long groupId;
    private String name;
    private Integer sortOrder;
    private Integer isDefault;
    private List<FriendVO> friends;
}
