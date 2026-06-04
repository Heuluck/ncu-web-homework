package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.ChatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroup> {
    @Update("UPDATE chat_group SET member_count = member_count + 1 WHERE id = #{groupId}")
    int incrementMemberCount(@Param("groupId") Long groupId);

    @Update("UPDATE chat_group SET member_count = member_count - 1 WHERE id = #{groupId} AND member_count > 0")
    int decrementMemberCount(@Param("groupId") Long groupId);
}