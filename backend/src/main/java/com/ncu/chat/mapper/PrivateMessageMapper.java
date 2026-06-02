package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /**
     * 批量标记某会话的消息为已读
     */
    @Update("UPDATE private_message SET status = 1 WHERE sender_id = #{friendId} AND receiver_id = #{userId} AND status = 0")
    int markAsRead(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
