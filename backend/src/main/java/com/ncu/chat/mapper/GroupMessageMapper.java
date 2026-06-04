package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {
}