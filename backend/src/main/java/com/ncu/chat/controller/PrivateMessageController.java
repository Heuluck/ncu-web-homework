package com.ncu.chat.controller;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.PrivateMessageSendDTO;
import com.ncu.chat.model.vo.ConversationVO;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.service.PrivateMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message/private")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    /**
     * 发送私聊消息
     */
    @PostMapping("/send")
    public Result<PrivateMessageVO> send(@Valid @RequestBody PrivateMessageSendDTO dto,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PrivateMessageVO vo = privateMessageService.sendMessage(userId, dto);
        return Result.success(vo);
    }

    /**
     * 获取历史消息
     */
    @GetMapping("/history")
    public Result<PageResult<PrivateMessageVO>> history(@RequestParam Long friendId,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "20") Integer pageSize,
                                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<PrivateMessageVO> page = privateMessageService.getHistory(userId, friendId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取未读消息
     */
    @GetMapping("/unread")
    public Result<List<PrivateMessageVO>> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<PrivateMessageVO> messages = privateMessageService.getUnreadMessages(userId);
        return Result.success(messages);
    }

    /**
     * 标记某会话消息为已读
     */
    @PutMapping("/read")
    public Result<Void> markRead(@RequestParam Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        privateMessageService.markAsRead(userId, friendId);
        return Result.success();
    }

    /**
     * 获取最近会话列表
     */
    @GetMapping("/recent")
    public Result<List<ConversationVO>> recent(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ConversationVO> conversations = privateMessageService.getRecentConversations(userId);
        return Result.success(conversations);
    }
}
