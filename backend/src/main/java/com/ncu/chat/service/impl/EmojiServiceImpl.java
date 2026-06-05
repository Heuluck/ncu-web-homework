package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncu.chat.mapper.EmojiMapper;
import com.ncu.chat.model.entity.Emoji;
import com.ncu.chat.service.EmojiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 表情服务实现类
 */
@Service
@RequiredArgsConstructor
public class EmojiServiceImpl implements EmojiService {

    private final EmojiMapper emojiMapper;

    @Override
    public List<Emoji> getAllEmojis() {
        LambdaQueryWrapper<Emoji> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Emoji::getCategory)
                .orderByAsc(Emoji::getSortOrder);
        return emojiMapper.selectList(wrapper);
    }

    @Override
    public List<Emoji> getEmojisByCategory(String category) {
        LambdaQueryWrapper<Emoji> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Emoji::getCategory, category)
                .orderByAsc(Emoji::getSortOrder);
        return emojiMapper.selectList(wrapper);
    }
}
