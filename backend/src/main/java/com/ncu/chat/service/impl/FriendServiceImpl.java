package com.ncu.chat.service.impl;

import com.ncu.chat.common.BusinessException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ncu.chat.mapper.*;
import com.ncu.chat.model.dto.*;
import com.ncu.chat.model.entity.*;
import com.ncu.chat.model.vo.*;
import com.ncu.chat.service.FriendService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendshipMapper friendshipMapper;
    private final FriendGroupMapper friendGroupMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public FriendServiceImpl(FriendshipMapper friendshipMapper,
                             FriendGroupMapper friendGroupMapper,
                             UserMapper userMapper,
                             SimpMessagingTemplate messagingTemplate) {
        this.friendshipMapper = friendshipMapper;
        this.friendGroupMapper = friendGroupMapper;
        this.userMapper = userMapper;
        this.messagingTemplate = messagingTemplate;
    }

    // ==================== 搜索用户 ====================

    @Override
    public List<SearchUserVO> searchUsers(Long userId, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword))
               .eq(User::getEnabled, 1)
               .ne(User::getId, userId);
        List<User> users = userMapper.selectList(wrapper);

        return users.stream().map(user -> {
            SearchUserVO vo = new SearchUserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setSignature(user.getSignature());
            vo.setOnlineStatus(user.getStatus());

            // 检查是否已是好友
            int friendCount = friendshipMapper.isFriend(userId, user.getId());
            vo.setIsFriend(friendCount > 0);

            // 检查是否有待处理申请（我发出的 或 对方发来的）
            int pendingCount = friendshipMapper.hasPendingRequest(userId, user.getId());
            if (pendingCount == 0) {
                pendingCount = friendshipMapper.hasPendingRequest(user.getId(), userId);
            }
            vo.setHasPendingRequest(pendingCount > 0);

            // 获取已存在的好友关系 ID
            Friendship existing = friendshipMapper.findByUserPair(userId, user.getId());
            vo.setFriendshipId(existing != null ? existing.getId() : null);

            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 好友申请 ====================

    @Override
    @Transactional
    public void sendRequest(Long requesterId, SendFriendRequestDTO dto) {
        if (requesterId.equals(dto.getFriendId())) {
            throw new BusinessException("不能添加自己为好友");
        }

        // 检查目标用户是否存在
        User target = userMapper.selectById(dto.getFriendId());
        if (target == null || target.getEnabled() == 0) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        // 检查是否已有关系记录
        Friendship existing = friendshipMapper.findByUserPair(requesterId, dto.getFriendId());
        if (existing != null) {
            if (existing.getStatus() == 0) {
                throw new BusinessException("已发送过好友申请，请等待对方处理");
            }
            if (existing.getStatus() == 1) {
                throw new BusinessException("你们已经是好友了");
            }
            if ((existing.getRequesterId().equals(requesterId) && existing.getRequesterBlocked() != null && existing.getRequesterBlocked() == 1)
                || (existing.getReceiverId().equals(requesterId) && existing.getReceiverBlocked() != null && existing.getReceiverBlocked() == 1)) {
                throw new BusinessException("你已拉黑对方，无法发送申请");
            }
            // status == 2 (已拒绝) → 允许重新发送
            existing.setStatus(0);
            existing.setVerificationMessage(dto.getVerificationMessage());
            existing.setRequesterId(requesterId);
            existing.setReceiverId(dto.getFriendId());
            existing.setRequesterGroupId(dto.getGroupId());
            friendshipMapper.updateById(existing);
            return;
        }

        Friendship friendship = new Friendship();
        friendship.setRequesterId(requesterId);
        friendship.setReceiverId(dto.getFriendId());
        friendship.setStatus(0);
        friendship.setVerificationMessage(dto.getVerificationMessage());
        friendship.setRequesterGroupId(dto.getGroupId());
        friendshipMapper.insert(friendship);
    }

    @Override
    public List<FriendRequestVO> getReceivedRequests(Long userId) {
        List<Friendship> friendships = friendshipMapper.findPendingByReceiver(userId);
        return friendships.stream().map(f -> convertToRequestVO(f, f.getRequesterId())).collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestVO> getSentRequests(Long userId) {
        List<Friendship> friendships = friendshipMapper.findPendingByRequester(userId);
        return friendships.stream().map(f -> convertToRequestVO(f, f.getReceiverId())).collect(Collectors.toList());
    }

    // ==================== 处理申请 ====================

    @Override
    @Transactional
    public void acceptRequest(Long userId, Long friendshipId, Long groupId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) {
            throw new BusinessException("好友申请不存在");
        }
        if (!friendship.getReceiverId().equals(userId)) {
            throw new BusinessException("无权处理该申请");
        }
        if (friendship.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        // 确定为接收者的默认分组
        Long receiverGroupId = groupId;
        if (receiverGroupId == null) {
            FriendGroup defaultGroup = friendGroupMapper.findDefaultByUserId(userId);
            if (defaultGroup != null) {
                receiverGroupId = defaultGroup.getId();
            }
        }

        // 确定为发起者的分组（如果发起者没有指定，使用默认分组）
        Long requesterGroupId = friendship.getRequesterGroupId();
        if (requesterGroupId == null) {
            FriendGroup requesterDefault = friendGroupMapper.findDefaultByUserId(friendship.getRequesterId());
            if (requesterDefault != null) {
                requesterGroupId = requesterDefault.getId();
            }
        }

        friendship.setStatus(1);
        friendship.setRequesterGroupId(requesterGroupId);
        friendship.setReceiverGroupId(receiverGroupId);
        friendshipMapper.updateById(friendship);
    }

    @Override
    @Transactional
    public void rejectRequest(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) {
            throw new BusinessException("好友申请不存在");
        }
        if (!friendship.getReceiverId().equals(userId)) {
            throw new BusinessException("无权处理该申请");
        }
        if (friendship.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        friendship.setStatus(2);
        friendshipMapper.updateById(friendship);
    }

    // ==================== 删除好友 ====================

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) {
            throw new BusinessException("好友关系不存在");
        }
        if (!friendship.getRequesterId().equals(userId) && !friendship.getReceiverId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (friendship.getStatus() != 1 && friendship.getStatus() != 3) {
            throw new BusinessException("当前状态不允许删除");
        }

        friendshipMapper.deleteById(friendshipId);
    }

    // ==================== 好友列表 ====================

    @Override
    public List<FriendGroupVO> getFriendList(Long userId) {
        // 加载用户的所有分组
        List<FriendGroup> groups = friendGroupMapper.findByUserId(userId);
        // 加载所有已接受的好友关系
        List<Friendship> friendships = friendshipMapper.findAcceptedByUserId(userId);

        // 构建 groupId → FriendGroupVO 映射
        Map<Long, FriendGroupVO> groupMap = new LinkedHashMap<>();
        for (FriendGroup group : groups) {
            FriendGroupVO gvo = new FriendGroupVO();
            gvo.setGroupId(group.getId());
            gvo.setName(group.getName());
            gvo.setSortOrder(group.getSortOrder());
            gvo.setIsDefault(group.getIsDefault());
            gvo.setFriends(new ArrayList<>());
            groupMap.put(group.getId(), gvo);
        }

        // 无分组的好友归入"未分组"虚拟组
        FriendGroupVO uncategorized = null;

        for (Friendship fs : friendships) {
            Long friendId = fs.getRequesterId().equals(userId) ? fs.getReceiverId() : fs.getRequesterId();
            Long groupId = fs.getRequesterId().equals(userId) ? fs.getRequesterGroupId() : fs.getReceiverGroupId();
            User friendUser = userMapper.selectById(friendId);
            if (friendUser == null) continue;

            FriendVO fvo = convertToFriendVO(fs, friendUser, userId);

            FriendGroupVO targetGroup = groupMap.get(groupId);
            if (targetGroup != null) {
                targetGroup.getFriends().add(fvo);
            } else {
                if (uncategorized == null) {
                    uncategorized = new FriendGroupVO();
                    uncategorized.setGroupId(0L);
                    uncategorized.setName("未分组");
                    uncategorized.setSortOrder(999);
                    uncategorized.setIsDefault(0);
                    uncategorized.setFriends(new ArrayList<>());
                }
                uncategorized.getFriends().add(fvo);
            }
        }

        List<FriendGroupVO> result = new ArrayList<>(groupMap.values());
        if (uncategorized != null && !uncategorized.getFriends().isEmpty()) {
            result.add(uncategorized);
        }
        return result;
    }

    // ==================== 分组管理 ====================

    @Override
    @Transactional
    public FriendGroupVO createGroup(Long userId, CreateFriendGroupDTO dto) {
        // 检查重名
        LambdaQueryWrapper<FriendGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendGroup::getUserId, userId).eq(FriendGroup::getName, dto.getName());
        if (friendGroupMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分组名称已存在");
        }

        FriendGroup group = new FriendGroup();
        group.setUserId(userId);
        group.setName(dto.getName());
        group.setSortOrder(0);
        group.setIsDefault(0);
        friendGroupMapper.insert(group);

        FriendGroupVO vo = new FriendGroupVO();
        vo.setGroupId(group.getId());
        vo.setName(group.getName());
        vo.setSortOrder(group.getSortOrder());
        vo.setIsDefault(group.getIsDefault());
        vo.setFriends(new ArrayList<>());
        return vo;
    }

    @Override
    @Transactional
    public FriendGroupVO updateGroup(Long userId, Long groupId, UpdateFriendGroupDTO dto) {
        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException("分组不存在");
        }
        group.setName(dto.getName());
        friendGroupMapper.updateById(group);

        FriendGroupVO vo = new FriendGroupVO();
        vo.setGroupId(group.getId());
        vo.setName(group.getName());
        vo.setSortOrder(group.getSortOrder());
        vo.setIsDefault(group.getIsDefault());
        vo.setFriends(new ArrayList<>());
        return vo;
    }

    @Override
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException("分组不存在");
        }
        if (group.getIsDefault() == 1) {
            throw new BusinessException("默认分组不能删除");
        }

        // 获取默认分组
        FriendGroup defaultGroup = friendGroupMapper.findDefaultByUserId(userId);

        // 将该分组下的好友移到默认分组
        LambdaUpdateWrapper<Friendship> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Friendship::getRequesterId, userId)
                     .eq(Friendship::getRequesterGroupId, groupId)
                     .set(Friendship::getRequesterGroupId, defaultGroup != null ? defaultGroup.getId() : null);
        friendshipMapper.update(null, updateWrapper);

        LambdaUpdateWrapper<Friendship> updateWrapper2 = new LambdaUpdateWrapper<>();
        updateWrapper2.eq(Friendship::getReceiverId, userId)
                      .eq(Friendship::getReceiverGroupId, groupId)
                      .set(Friendship::getReceiverGroupId, defaultGroup != null ? defaultGroup.getId() : null);
        friendshipMapper.update(null, updateWrapper2);

        // 逻辑删除分组
        group.setDeleted(1);
        friendGroupMapper.updateById(group);
    }

    // ==================== 移动好友 ====================

    @Override
    @Transactional
    public void moveFriend(Long userId, Long friendshipId, MoveFriendDTO dto) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null || friendship.getStatus() != 1) {
            throw new BusinessException("好友关系不存在");
        }
        // 验证目标分组属于当前用户
        FriendGroup targetGroup = friendGroupMapper.selectById(dto.getGroupId());
        if (targetGroup == null || !targetGroup.getUserId().equals(userId)) {
            throw new BusinessException("目标分组不存在");
        }

        // 更新当前用户视角的分组
        if (friendship.getRequesterId().equals(userId)) {
            friendship.setRequesterGroupId(dto.getGroupId());
        } else if (friendship.getReceiverId().equals(userId)) {
            friendship.setReceiverGroupId(dto.getGroupId());
        } else {
            throw new BusinessException("无权操作");
        }
        friendshipMapper.updateById(friendship);
    }

    // ==================== 拉黑 / 取消拉黑 ====================

    @Override
    @Transactional
    public void blockFriend(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) {
            throw new BusinessException("好友关系不存在");
        }
        if (friendship.getStatus() != 1) {
            throw new BusinessException("当前状态不允许拉黑");
        }

        if (friendship.getRequesterId().equals(userId)) {
            if (friendship.getRequesterBlocked() != null && friendship.getRequesterBlocked() == 1) {
                throw new BusinessException("已经拉黑了");
            }
            friendship.setRequesterBlocked(1);
        } else if (friendship.getReceiverId().equals(userId)) {
            if (friendship.getReceiverBlocked() != null && friendship.getReceiverBlocked() == 1) {
                throw new BusinessException("已经拉黑了");
            }
            friendship.setReceiverBlocked(1);
        } else {
            throw new BusinessException("无权操作");
        }
        friendshipMapper.updateById(friendship);
        // WebSocket 通知对方
        Long otherId = friendship.getRequesterId().equals(userId)
                ? friendship.getReceiverId() : friendship.getRequesterId();
        Map<String, Object> notify = new HashMap<>();
        notify.put("type", "BLOCK_STATUS_CHANGE");
        notify.put("friendshipId", friendshipId);
        notify.put("blocked", true);
        notify.put("fromUserId", userId);
        messagingTemplate.convertAndSendToUser(String.valueOf(otherId), "/queue/friend-status", notify);
    }

    @Override
    @Transactional
    public void unblockFriend(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) {
            throw new BusinessException("好友关系不存在");
        }

        if (friendship.getRequesterId().equals(userId)) {
            if (friendship.getRequesterBlocked() == null || friendship.getRequesterBlocked() == 0) {
                throw new BusinessException("你未拉黑该好友");
            }
            friendship.setRequesterBlocked(0);
        } else if (friendship.getReceiverId().equals(userId)) {
            if (friendship.getReceiverBlocked() == null || friendship.getReceiverBlocked() == 0) {
                throw new BusinessException("你未拉黑该好友");
            }
            friendship.setReceiverBlocked(0);
        } else {
            throw new BusinessException("无权操作");
        }
        friendshipMapper.updateById(friendship);
        // WebSocket 通知对方
        Long otherId2 = friendship.getRequesterId().equals(userId)
                ? friendship.getReceiverId() : friendship.getRequesterId();
        Map<String, Object> notify2 = new HashMap<>();
        notify2.put("type", "BLOCK_STATUS_CHANGE");
        notify2.put("friendshipId", friendshipId);
        notify2.put("blocked", false);
        notify2.put("fromUserId", userId);
        messagingTemplate.convertAndSendToUser(String.valueOf(otherId2), "/queue/friend-status", notify2);
    }

    // ==================== 工具方法 ====================

    private FriendRequestVO convertToRequestVO(Friendship fs, Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        FriendRequestVO vo = new FriendRequestVO();
        vo.setFriendshipId(fs.getId());
        if (user != null) {
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setOnlineStatus(user.getStatus());
        }
        vo.setVerificationMessage(fs.getVerificationMessage());
        vo.setCreateTime(fs.getCreateTime());
        return vo;
    }

    private FriendVO convertToFriendVO(Friendship fs, User friendUser, Long currentUserId) {
        FriendVO vo = new FriendVO();
        vo.setFriendshipId(fs.getId());
        vo.setFriendId(friendUser.getId());
        vo.setUsername(friendUser.getUsername());
        vo.setNickname(friendUser.getNickname());
        vo.setAvatar(friendUser.getAvatar());
        vo.setSignature(friendUser.getSignature());
        vo.setOnlineStatus(friendUser.getStatus());
        vo.setStatus(fs.getStatus());

        // 当前用户视角的分组
        if (fs.getRequesterId().equals(currentUserId)) {
            vo.setGroupId(fs.getRequesterGroupId());
        } else {
            vo.setGroupId(fs.getReceiverGroupId());
        }

        // 计算拉黑状态
        boolean iBlockedThem, theyBlockedMe;
        if (fs.getRequesterId().equals(currentUserId)) {
            iBlockedThem = fs.getRequesterBlocked() != null && fs.getRequesterBlocked() == 1;
            theyBlockedMe = fs.getReceiverBlocked() != null && fs.getReceiverBlocked() == 1;
        } else {
            iBlockedThem = fs.getReceiverBlocked() != null && fs.getReceiverBlocked() == 1;
            theyBlockedMe = fs.getRequesterBlocked() != null && fs.getRequesterBlocked() == 1;
        }
        if (iBlockedThem && theyBlockedMe) vo.setBlockStatus("both");
        else if (iBlockedThem) vo.setBlockStatus("blocked_by_me");
        else if (theyBlockedMe) vo.setBlockStatus("blocked_by_them");
        else vo.setBlockStatus("none");

        return vo;
    }
}
