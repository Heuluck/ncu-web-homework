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
    private LocalDateTime createTime;
}
