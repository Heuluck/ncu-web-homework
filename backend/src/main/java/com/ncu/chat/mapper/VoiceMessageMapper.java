package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.VoiceMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoiceMessageMapper extends BaseMapper<VoiceMessage> {
}
