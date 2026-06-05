package com.ncu.chat.service;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.model.entity.SensitiveWord;

import java.util.Map;

public interface SensitiveWordService {
    PageResult<SensitiveWord> listSensitiveWords(int pageNum, int pageSize, String keyword);
    SensitiveWord addSensitiveWord(String word, String category, Integer enabled);
    SensitiveWord updateSensitiveWord(Long id, String word, String category, Integer enabled);
    void deleteSensitiveWord(Long id);
    Map<String, Object> checkSensitiveWords(String text);
}
