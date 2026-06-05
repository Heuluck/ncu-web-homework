package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFriendGroupDTO {
    @NotBlank(message = "分组名称不能为空")
    @Size(max = 50, message = "分组名称最长50个字符")
    private String name;
}
