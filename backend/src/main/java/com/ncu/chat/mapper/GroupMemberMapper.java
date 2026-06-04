package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    @Select("SELECT user_id FROM group_member WHERE group_id = #{groupId} AND deleted = 0")
    List<Long> getUserIdsByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT DISTINCT group_id FROM group_member WHERE user_id = #{userId} AND deleted = 0")
    List<Long> getGroupIdsByUserId(@Param("userId") Long userId);

    @Update("UPDATE group_member SET last_read_time = NOW() WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateLastReadTime(@Param("groupId") Long groupId, @Param("userId") Long userId);
}