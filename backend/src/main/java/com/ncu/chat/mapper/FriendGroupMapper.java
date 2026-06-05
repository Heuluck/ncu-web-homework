package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.FriendGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FriendGroupMapper extends BaseMapper<FriendGroup> {

    @Select("SELECT * FROM friend_group WHERE user_id = #{userId} AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<FriendGroup> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM friend_group WHERE user_id = #{userId} AND is_default = 1 AND deleted = 0 LIMIT 1")
    FriendGroup findDefaultByUserId(@Param("userId") Long userId);
}
