package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateGroupDTO {
    @NotBlank(message = "群名称不能为空")
    @Size(max = 100, message = "群名称最长100个字符")
    private String name;
    private String avatar;
    @Size(max = 500, message = "群公告最长500个字符")
    private String announcement;
    private List<Long> memberIds;
}