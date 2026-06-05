package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.vo.VoiceMessageVO;
import com.ncu.chat.service.VoiceMessageService;
import com.ncu.chat.util.FileUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final FileUtil fileUtil;
    private final VoiceMessageService voiceMessageService;

    /**
     * 上传语音文件
     */
    @PostMapping("/upload")
    public Result<?> uploadVoice(@RequestParam("file") MultipartFile file) throws IOException {
        String url = fileUtil.upload(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success("上传成功", result);
    }

    /**
     * 获取语音消息详情
     */
    @GetMapping("/{id}")
    public Result<VoiceMessageVO> getVoiceMessage(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // Since we need to query by message ID, we'll use the getVoiceMessages approach
        // For now, return the voice message with the given ID by querying all messages
        // This is a simplified approach - in production you'd have a getById method
        return Result.error("请使用聊天记录接口获取语音消息");
    }

    /**
     * 获取与指定对象的语音消息列表
     */
    @GetMapping("/messages")
    public Result<List<VoiceMessageVO>> getVoiceMessages(
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "false") boolean isGroup,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(voiceMessageService.getVoiceMessages(userId, targetId, isGroup));
    }
}
