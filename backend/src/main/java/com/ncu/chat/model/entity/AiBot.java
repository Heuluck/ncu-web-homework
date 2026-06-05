package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_bot")
public class AiBot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private String name;
    private String avatar;
    private String endpoint;
    private String apiKeyEncrypted;
    private String model;
    private String systemPrompt;
    private Integer triggerType;
    private BigDecimal triggerProbability;
    private BigDecimal temperature;
    private BigDecimal topP;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
