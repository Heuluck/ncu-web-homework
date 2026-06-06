package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}