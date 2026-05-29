package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer status;      // 0-离线 1-在线 2-忙碌 3-勿扰
    private Integer role;        // 0-普通用户 1-管理员
    private Integer enabled;     // 0-禁用 1-启用
    @TableLogic
    private Integer deleted;     // 0-未删除 1-已删除
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
}
