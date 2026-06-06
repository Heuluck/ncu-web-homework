package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /**
     * 批量标记某会话的消息为已读
     */
    @Update("UPDATE private_message SET status = 1 WHERE sender_id = #{friendId} AND receiver_id = #{userId} AND status = 0")
    int markAsRead(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 批量获取每个会话的最后一条消息（一次查询替代 N 次查询）
     */
    @Select("SELECT pm.* FROM private_message pm " +
            "INNER JOIN (" +
            "  SELECT IF(sender_id < receiver_id, " +
            "    CONCAT(sender_id, '_', receiver_id), " +
            "    CONCAT(receiver_id, '_', sender_id)) AS pair_key, " +
            "    MAX(id) AS max_id " +
            "  FROM private_message " +
            "  WHERE sender_id = #{userId} OR receiver_id = #{userId} " +
            "  GROUP BY pair_key" +
            ") t ON pm.id = t.max_id " +
            "ORDER BY pm.create_time DESC")
    List<PrivateMessage> getLastMessagesPerConversation(@Param("userId") Long userId);

    /**
     * 批量获取每个发送者的未读消息数
     */
    @Select("SELECT sender_id AS senderId, COUNT(*) AS cnt " +
            "FROM private_message " +
            "WHERE receiver_id = #{userId} AND status = 0 " +
            "GROUP BY sender_id")
    List<Map<String, Object>> getUnreadCountsBySender(@Param("userId") Long userId);
}
