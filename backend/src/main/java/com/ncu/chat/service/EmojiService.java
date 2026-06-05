package com.ncu.chat.service;

import com.ncu.chat.model.entity.Emoji;

import java.util.List;

/**
 * 表情服务
 */
public interface EmojiService {

    /**
     * 获取所有表情列表（按分类和排序）
     */
    List<Emoji> getAllEmojis();

    /**
     * 根据分类获取表情
     */
    List<Emoji> getEmojisByCategory(String category);
}
