package com.ncu.chat.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiBotVO {
    private Long id;
    private Long ownerId;
    private String name;
    private String avatar;
    private String endpoint;
    private String model;
    private String systemPrompt;
    /** 触发条件：0=@触发 1=每次触发 2=随机概率 */
    private Integer triggerType;
    private BigDecimal triggerProbability;
    private BigDecimal temperature;
    private BigDecimal topP;
    /** 谁将此机器人添加到群聊（群主或管理员 userId） */
    private Long addedBy;
    private LocalDateTime createTime;
}
