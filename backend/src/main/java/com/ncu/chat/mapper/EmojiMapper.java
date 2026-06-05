package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.Emoji;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表情 Mapper 接口
 */
@Mapper
public interface EmojiMapper extends BaseMapper<Emoji> {
}
