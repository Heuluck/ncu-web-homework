package com.ncu.chat.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateBotDTO {
    private String name;
    private String avatar;
    private String endpoint;
    /** 为空则不更新 API Key */
    private String apiKey;
    private String model;
    private String systemPrompt;
    private Integer triggerType;
    private BigDecimal triggerProbability;
    private BigDecimal temperature;
    private BigDecimal topP;
}
