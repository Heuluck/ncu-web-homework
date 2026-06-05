package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.mapper.VoiceMessageMapper;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.entity.VoiceMessage;
import com.ncu.chat.model.vo.VoiceMessageVO;
import com.ncu.chat.service.VoiceMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceMessageServiceImpl implements VoiceMessageService {

    private final VoiceMessageMapper voiceMessageMapper;
    private final UserMapper userMapper;

    @Override
    public VoiceMessageVO saveVoiceMessage(Long senderId, Long receiverId, Long groupId, String fileUrl, Integer duration) {
        VoiceMessage vm = new VoiceMessage();
        vm.setSenderId(senderId);
        vm.setReceiverId(receiverId);
        vm.setGroupId(groupId);
        vm.setFileUrl(fileUrl);
        vm.setDuration(duration);
        voiceMessageMapper.insert(vm);

        User sender = userMapper.selectById(senderId);
        return convertToVO(vm, sender);
    }

    @Override
    public List<VoiceMessageVO> getVoiceMessages(Long userId, Long targetId, boolean isGroup) {
        LambdaQueryWrapper<VoiceMessage> wrapper = new LambdaQueryWrapper<>();
        if (isGroup) {
            wrapper.eq(VoiceMessage::getGroupId, targetId);
        } else {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(VoiceMessage::getSenderId, userId).eq(VoiceMessage::getReceiverId, targetId))
                    .or(w2 -> w2.eq(VoiceMessage::getSenderId, targetId).eq(VoiceMessage::getReceiverId, userId))
            );
        }
        wrapper.orderByDesc(VoiceMessage::getCreateTime);

        List<VoiceMessage> messages = voiceMessageMapper.selectList(wrapper);

        return messages.stream().map(vm -> {
            User sender = userMapper.selectById(vm.getSenderId());
            return convertToVO(vm, sender);
        }).collect(Collectors.toList());
    }

    private VoiceMessageVO convertToVO(VoiceMessage vm, User sender) {
        VoiceMessageVO vo = new VoiceMessageVO();
        vo.setId(vm.getId());
        vo.setSenderId(vm.getSenderId());
        vo.setReceiverId(vm.getReceiverId());
        vo.setGroupId(vm.getGroupId());
        vo.setFileUrl(vm.getFileUrl());
        vo.setDuration(vm.getDuration());
        vo.setCreateTime(vm.getCreateTime());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        } else {
            vo.setSenderNickname("未知用户");
            vo.setSenderAvatar(null);
        }
        return vo;
    }
}
