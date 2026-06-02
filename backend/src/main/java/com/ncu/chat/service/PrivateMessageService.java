package com.ncu.chat.service;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.model.dto.PrivateMessageSendDTO;
import com.ncu.chat.model.vo.ConversationVO;
import com.ncu.chat.model.vo.PrivateMessageVO;

import java.util.List;

public interface PrivateMessageService {

    /**
     * 发送私聊消息
     */
    PrivateMessageVO sendMessage(Long senderId, PrivateMessageSendDTO dto);

    /**
     * 获取历史消息（双向查询，按时间倒序分页）
     */
    PageResult<PrivateMessageVO> getHistory(Long userId, Long friendId, Integer pageNum, Integer pageSize);

    /**
     * 获取未读消息
     */
    List<PrivateMessageVO> getUnreadMessages(Long userId);

    /**
     * 标记某会话的消息为已读
     */
    void markAsRead(Long userId, Long friendId);

    /**
     * 获取最近会话列表
     */
    List<ConversationVO> getRecentConversations(Long userId);
}
