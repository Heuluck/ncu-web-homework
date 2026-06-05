package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.mapper.SensitiveWordMapper;
import com.ncu.chat.model.entity.SensitiveWord;
import com.ncu.chat.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    @Override
    public PageResult<SensitiveWord> listSensitiveWords(int pageNum, int pageSize, String keyword) {
        Page<SensitiveWord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SensitiveWord::getWord, keyword);
        }
        wrapper.orderByDesc(SensitiveWord::getCreateTime);
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public SensitiveWord addSensitiveWord(String word, String category, Integer enabled) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getWord, word);
        if (sensitiveWordMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("敏感词已存在");
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        sw.setCategory(category != null ? category : "通用");
        sw.setEnabled(enabled != null ? enabled : 1);
        sensitiveWordMapper.insert(sw);
        return sw;
    }

    @Override
    public SensitiveWord updateSensitiveWord(Long id, String word, String category, Integer enabled) {
        SensitiveWord sw = sensitiveWordMapper.selectById(id);
        if (sw == null) {
            throw new RuntimeException("敏感词不存在");
        }
        if (word != null && !word.equals(sw.getWord())) {
            LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SensitiveWord::getWord, word);
            if (sensitiveWordMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("敏感词已存在");
            }
            sw.setWord(word);
        }
        if (category != null) sw.setCategory(category);
        if (enabled != null) sw.setEnabled(enabled);
        sensitiveWordMapper.updateById(sw);
        return sw;
    }

    @Override
    public void deleteSensitiveWord(Long id) {
        sensitiveWordMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> checkSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasSensitive", false);
            result.put("words", new ArrayList<>());
            return result;
        }

        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getEnabled, 1);
        List<SensitiveWord> allWords = sensitiveWordMapper.selectList(wrapper);

        List<String> matchedWords = new ArrayList<>();
        String lowerText = text.toLowerCase();
        for (SensitiveWord sw : allWords) {
            if (lowerText.contains(sw.getWord().toLowerCase())) {
                matchedWords.add(sw.getWord());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasSensitive", !matchedWords.isEmpty());
        result.put("words", matchedWords);
        return result;
    }
}
