package com.ncu.chat.service;

import com.ncu.chat.model.vo.VoiceMessageVO;
import java.util.List;

public interface VoiceMessageService {
    VoiceMessageVO saveVoiceMessage(Long senderId, Long receiverId, Long groupId, String fileUrl, Integer duration);
    List<VoiceMessageVO> getVoiceMessages(Long userId, Long targetId, boolean isGroup);
}
