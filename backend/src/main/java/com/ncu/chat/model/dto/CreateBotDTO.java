package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateBotDTO {
    @NotBlank(message = "机器人名称不能为空")
    private String name;
    private String avatar;
    @NotBlank(message = "Endpoint 不能为空")
    private String endpoint;
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;
    @NotBlank(message = "模型名称不能为空")
    private String model;
    private String systemPrompt;
    /** 触发条件：0=@触发 1=每次触发 2=随机概率 */
    private Integer triggerType = 0;
    /** 随机概率值（0~1），triggerType=2 时必填 */
    private BigDecimal triggerProbability;
    private BigDecimal temperature = BigDecimal.ONE;
    private BigDecimal topP = BigDecimal.ONE;
}
