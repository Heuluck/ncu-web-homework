package com.ncu.chat.service;

import com.ncu.chat.model.vo.PrivateMessageVO;

import java.io.IOException;
import java.util.List;

/**
 * 消息中心服务（聊天记录查询、搜索、导出）
 */
public interface MessageService {

    /**
     * 搜索私聊聊天记录（关键词）
     */
    List<PrivateMessageVO> searchPrivateMessages(Long userId, Long targetId, String keyword);

    /**
     * 搜索群聊聊天记录（关键词）
     */
    List<PrivateMessageVO> searchGroupMessages(Long groupId, String keyword);

    /**
     * 按时间范围筛选私聊记录
     */
    List<PrivateMessageVO> filterPrivateMessages(Long userId, Long targetId, String startTime, String endTime);

    /**
     * 按时间范围筛选群聊记录
     */
    List<PrivateMessageVO> filterGroupMessages(Long groupId, String startTime, String endTime);

    /**
     * 导出私聊记录为 TXT
     */
    String exportPrivateMessagesAsTxt(Long userId, Long targetId) throws IOException;

    /**
     * 导出私聊记录为 CSV
     */
    String exportPrivateMessagesAsCsv(Long userId, Long targetId) throws IOException;

    /**
     * 导出群聊记录为 TXT
     */
    String exportGroupMessagesAsTxt(Long groupId) throws IOException;

    /**
     * 导出群聊记录为 CSV
     */
    String exportGroupMessagesAsCsv(Long groupId) throws IOException;
}
