package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    @Select("SELECT * FROM friendship WHERE status IN (1, 3) AND ((requester_id = #{userId}) OR (receiver_id = #{userId})) ORDER BY update_time DESC")
    List<Friendship> findAcceptedByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM friendship WHERE receiver_id = #{userId} AND status = 0 ORDER BY create_time DESC")
    List<Friendship> findPendingByReceiver(@Param("userId") Long userId);

    @Select("SELECT * FROM friendship WHERE requester_id = #{userId} AND status = 0 ORDER BY create_time DESC")
    List<Friendship> findPendingByRequester(@Param("userId") Long userId);

    @Select("SELECT * FROM friendship WHERE ((requester_id = #{userId1} AND receiver_id = #{userId2}) OR (requester_id = #{userId2} AND receiver_id = #{userId1})) LIMIT 1")
    Friendship findByUserPair(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Select("SELECT COUNT(*) FROM friendship WHERE ((requester_id = #{userId1} AND receiver_id = #{userId2}) OR (requester_id = #{userId2} AND receiver_id = #{userId1})) AND status = 1")
    int isFriend(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Select("SELECT COUNT(*) FROM friendship WHERE requester_id = #{requesterId} AND receiver_id = #{receiverId} AND status = 0")
    int hasPendingRequest(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);
}
