package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    /**
     * 批量获取多个群的最后一条消息（一次查询替代 N 次查询）
     */
    @Select("<script>" +
            "SELECT gm.* FROM group_message gm " +
            "INNER JOIN (" +
            "  SELECT group_id, MAX(id) AS max_id " +
            "  FROM group_message " +
            "  WHERE group_id IN " +
            "  <foreach collection='groupIds' item='gid' open='(' separator=',' close=')'>#{gid}</foreach>" +
            "  GROUP BY group_id" +
            ") t ON gm.id = t.max_id" +
            "</script>")
    List<GroupMessage> getLastMessagesByGroupIds(@Param("groupIds") List<Long> groupIds);

    /**
     * 批量统计群未读消息数（消息时间晚于 last_read_time）
     */
    @Select("<script>" +
            "SELECT gm.group_id, COUNT(*) AS cnt " +
            "FROM group_message gm " +
            "JOIN group_member gmb ON gm.group_id = gmb.group_id AND gmb.user_id = #{userId} AND gmb.deleted = 0 " +
            "WHERE gm.group_id IN " +
            "  <foreach collection='groupIds' item='gid' open='(' separator=',' close=')'>#{gid}</foreach>" +
            "  AND gm.create_time > gmb.last_read_time " +
            "  AND gm.sender_id != #{userId} " +
            "GROUP BY gm.group_id" +
            "</script>")
    List<Map<String, Object>> countUnreadByGroupIds(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);
}