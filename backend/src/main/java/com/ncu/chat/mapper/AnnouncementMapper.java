package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
