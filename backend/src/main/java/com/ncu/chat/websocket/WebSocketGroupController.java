package com.ncu.chat.websocket;

import com.ncu.chat.mapper.GroupMemberMapper;
import com.ncu.chat.model.dto.GroupMessageSendDTO;
import com.ncu.chat.model.vo.GroupMessageVO;
import com.ncu.chat.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketGroupController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupService groupService;
    private final GroupMemberMapper groupMemberMapper;

    @MessageMapping("/group.send")
    public void sendGroupMessage(GroupMessageSendDTO dto, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        GroupMessageVO vo = groupService.sendMessage(userId, dto);
        sendGroupMessage(vo);
    }

    @MessageMapping("/group.read")
    public void markGroupRead(GroupReadNotification notification, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        groupService.markRead(userId, notification.getGroupId());
    }

    public void sendGroupMessage(GroupMessageVO vo) {
        List<Long> memberIds = groupMemberMapper.getUserIdsByGroupId(vo.getGroupId());
        for (Long memberId : memberIds) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(memberId),
                    "/queue/group_messages",
                    vo
            );
        }
        log.info("群消息已广播: groupId={}", vo.getGroupId());
    }

    @lombok.Data
    public static class GroupReadNotification {
        private Long groupId;
    }
}