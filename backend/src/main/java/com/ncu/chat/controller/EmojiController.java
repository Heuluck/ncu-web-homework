package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.entity.Emoji;
import com.ncu.chat.service.EmojiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表情控制器
 */
@RestController
@RequestMapping("/api/emoji")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiService emojiService;

    /**
     * 获取所有表情列表
     */
    @GetMapping("/list")
    public Result<List<Emoji>> getAllEmojis() {
        List<Emoji> emojis = emojiService.getAllEmojis();
        return Result.success(emojis);
    }

    /**
     * 根据分类获取表情
     */
    @GetMapping("/list/{category}")
    public Result<List<Emoji>> getEmojisByCategory(@PathVariable String category) {
        List<Emoji> emojis = emojiService.getEmojisByCategory(category);
        return Result.success(emojis);
    }
}
