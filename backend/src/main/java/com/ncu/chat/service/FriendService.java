package com.ncu.chat.service;

import com.ncu.chat.model.dto.*;
import com.ncu.chat.model.vo.*;
import java.util.List;

public interface FriendService {

    /** 搜索用户（用户名/昵称模糊匹配），排除自己，标记好友关系和待处理申请 */
    List<SearchUserVO> searchUsers(Long userId, String keyword);

    /** 发送好友申请 */
    void sendRequest(Long requesterId, SendFriendRequestDTO dto);

    /** 获取收到的待处理申请 */
    List<FriendRequestVO> getReceivedRequests(Long userId);

    /** 获取发出的待处理申请 */
    List<FriendRequestVO> getSentRequests(Long userId);

    /** 接受好友申请 */
    void acceptRequest(Long userId, Long friendshipId, Long groupId);

    /** 拒绝好友申请 */
    void rejectRequest(Long userId, Long friendshipId);

    /** 删除好友 */
    void deleteFriend(Long userId, Long friendshipId);

    /** 按分组获取好友列表 */
    List<FriendGroupVO> getFriendList(Long userId);

    /** 创建好友分组 */
    FriendGroupVO createGroup(Long userId, CreateFriendGroupDTO dto);

    /** 重命名分组 */
    FriendGroupVO updateGroup(Long userId, Long groupId, UpdateFriendGroupDTO dto);

    /** 删除分组（不能删除默认分组，好友移入默认分组） */
    void deleteGroup(Long userId, Long groupId);

    /** 移动好友到指定分组 */
    void moveFriend(Long userId, Long friendshipId, MoveFriendDTO dto);

    /** 拉黑好友 */
    void blockFriend(Long userId, Long friendshipId);

    /** 取消拉黑 */
    void unblockFriend(Long userId, Long friendshipId);
}
