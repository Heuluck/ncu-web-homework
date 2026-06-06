package com.ncu.chat.service.impl;

import com.ncu.chat.common.BusinessException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.mapper.SensitiveWordMapper;
import com.ncu.chat.model.entity.SensitiveWord;
import com.ncu.chat.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    /** 敏感词缓存：key=word, value=SensitiveWord */
    private volatile Map<String, SensitiveWord> wordCache;
    private volatile long lastCacheRefresh = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1 分钟刷新

    /**
     * 获取已启用的敏感词缓存（带 TTL 自动刷新）
     */
    private Map<String, SensitiveWord> getCachedWords() {
        long now = System.currentTimeMillis();
        if (wordCache == null || (now - lastCacheRefresh) > CACHE_TTL_MS) {
            synchronized (this) {
                if (wordCache == null || (now - lastCacheRefresh) > CACHE_TTL_MS) {
                    LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SensitiveWord::getEnabled, 1);
                    List<SensitiveWord> words = sensitiveWordMapper.selectList(wrapper);
                    Map<String, SensitiveWord> map = new ConcurrentHashMap<>();
                    for (SensitiveWord sw : words) {
                        map.put(sw.getWord().toLowerCase(), sw);
                    }
                    wordCache = map;
                    lastCacheRefresh = System.currentTimeMillis();
                }
            }
        }
        return wordCache;
    }

    /** 刷新缓存（增删改敏感词后调用） */
    private void invalidateCache() {
        lastCacheRefresh = 0;
    }

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
            throw new BusinessException("敏感词已存在");
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        sw.setCategory(category != null ? category : "通用");
        sw.setEnabled(enabled != null ? enabled : 1);
        sensitiveWordMapper.insert(sw);
        invalidateCache();
        return sw;
    }

    @Override
    public SensitiveWord updateSensitiveWord(Long id, String word, String category, Integer enabled) {
        SensitiveWord sw = sensitiveWordMapper.selectById(id);
        if (sw == null) {
            throw new BusinessException("敏感词不存在");
        }
        if (word != null && !word.equals(sw.getWord())) {
            LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SensitiveWord::getWord, word);
            if (sensitiveWordMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("敏感词已存在");
            }
            sw.setWord(word);
        }
        if (category != null) sw.setCategory(category);
        if (enabled != null) sw.setEnabled(enabled);
        sensitiveWordMapper.updateById(sw);
        invalidateCache();
        return sw;
    }

    @Override
    public void deleteSensitiveWord(Long id) {
        sensitiveWordMapper.deleteById(id);
        invalidateCache();
    }

    @Override
    public Map<String, Object> checkSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasSensitive", false);
            result.put("words", new ArrayList<>());
            return result;
        }

        // 使用缓存替代每次全表扫描
        Map<String, SensitiveWord> cache = getCachedWords();
        List<String> matchedWords = new ArrayList<>();
        String lowerText = text.toLowerCase();
        for (String word : cache.keySet()) {
            if (lowerText.contains(word)) {
                matchedWords.add(cache.get(word).getWord());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasSensitive", !matchedWords.isEmpty());
        result.put("words", matchedWords);
        return result;
    }
}
