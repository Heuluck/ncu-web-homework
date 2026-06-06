package com.ncu.chat.service;

import com.ncu.chat.model.dto.CreateBotDTO;
import com.ncu.chat.model.dto.UpdateBotDTO;
import com.ncu.chat.model.entity.AiBot;
import com.ncu.chat.model.vo.AiBotVO;

import java.util.List;

public interface AiBotService {
    AiBotVO createBot(Long userId, CreateBotDTO dto);
    AiBotVO updateBot(Long userId, Long botId, UpdateBotDTO dto);
    void deleteBot(Long userId, Long botId);
    List<AiBotVO> getMyBots(Long userId);
    void addBotToGroup(Long userId, Long groupId, Long botId);
    void removeBotFromGroup(Long userId, Long groupId, Long botId);
    List<AiBotVO> getGroupBots(Long groupId);

    /**
     * 异步检查并触发 AI 回复
     * @param chainDepth 链式触发深度（0=用户消息，>0=机器人消息）
     */
    void checkAndTriggerBots(Long groupId, Long senderId, String content, int chainDepth);

    /** 默认链式深度为 0（用户消息） */
    default void checkAndTriggerBots(Long groupId, Long senderId, String content) {
        checkAndTriggerBots(groupId, senderId, content, 0);
    }

    /**
     * 调用 AI API 并在群聊中发送回复
     * @param chainDepth 链式触发深度
     */
    void callAiAndReply(Long groupId, AiBot bot, String userMessage, Long senderId, String senderName, int chainDepth);
}
