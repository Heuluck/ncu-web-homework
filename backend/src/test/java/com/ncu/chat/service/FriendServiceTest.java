package com.ncu.chat.service;

import com.ncu.chat.mapper.FriendGroupMapper;
import com.ncu.chat.mapper.FriendshipMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.dto.*;
import com.ncu.chat.model.entity.*;
import com.ncu.chat.model.vo.*;
import com.ncu.chat.service.impl.FriendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock private FriendshipMapper friendshipMapper;
    @Mock private FriendGroupMapper friendGroupMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private FriendServiceImpl friendService;

    private User user1, user2, user3;
    private FriendGroup defaultGroup1, defaultGroup2;
    private Friendship pendingFriendship, acceptedFriendship;

    @BeforeEach
    void setUp() {
        user1 = buildUser(1L, "alice", "Alice", 1);
        user2 = buildUser(2L, "bob", "Bob", 1);
        user3 = buildUser(3L, "charlie", "Charlie", 0);

        defaultGroup1 = buildGroup(1L, 1L, "我的好友", 1);
        defaultGroup2 = buildGroup(2L, 2L, "我的好友", 1);

        pendingFriendship = new Friendship();
        pendingFriendship.setId(1L);
        pendingFriendship.setRequesterId(1L);
        pendingFriendship.setReceiverId(2L);
        pendingFriendship.setStatus(0);
        pendingFriendship.setVerificationMessage("你好");

        acceptedFriendship = new Friendship();
        acceptedFriendship.setId(2L);
        acceptedFriendship.setRequesterId(1L);
        acceptedFriendship.setReceiverId(2L);
        acceptedFriendship.setStatus(1);
        acceptedFriendship.setRequesterGroupId(1L);
        acceptedFriendship.setReceiverGroupId(2L);
    }

    // ==================== 搜索用户 ====================

    @Test
    void searchUsers_findsUsers() {
        when(userMapper.selectList(any())).thenReturn(List.of(user2, user3));
        when(friendshipMapper.isFriend(anyLong(), anyLong())).thenReturn(0);
        when(friendshipMapper.hasPendingRequest(anyLong(), anyLong())).thenReturn(0);
        when(friendshipMapper.findByUserPair(anyLong(), anyLong())).thenReturn(null);

        List<SearchUserVO> result = friendService.searchUsers(1L, "test");

        assertEquals(2, result.size());
        assertFalse(result.get(0).getIsFriend());
        assertFalse(result.get(0).getHasPendingRequest());
    }

    @Test
    void searchUsers_marksExistingFriend() {
        when(userMapper.selectList(any())).thenReturn(List.of(user2));
        when(friendshipMapper.isFriend(1L, 2L)).thenReturn(1);
        when(friendshipMapper.findByUserPair(1L, 2L)).thenReturn(acceptedFriendship);

        List<SearchUserVO> result = friendService.searchUsers(1L, "bob");

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsFriend());
    }

    // ==================== 发送申请 ====================

    @Test
    void sendRequest_selfFriend_throws() {
        SendFriendRequestDTO dto = new SendFriendRequestDTO();
        dto.setFriendId(1L);

        assertThrows(RuntimeException.class, () -> friendService.sendRequest(1L, dto));
    }

    @Test
    void sendRequest_targetNotFound_throws() {
        SendFriendRequestDTO dto = new SendFriendRequestDTO();
        dto.setFriendId(99L);

        when(userMapper.selectById(99L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> friendService.sendRequest(1L, dto));
    }

    @Test
    void sendRequest_alreadyPending_throws() {
        SendFriendRequestDTO dto = new SendFriendRequestDTO();
        dto.setFriendId(2L);

        Friendship existing = new Friendship();
        existing.setId(1L);
        existing.setStatus(0);
        when(friendshipMapper.findByUserPair(1L, 2L)).thenReturn(existing);
        when(userMapper.selectById(2L)).thenReturn(user2);

        assertThrows(RuntimeException.class, () -> friendService.sendRequest(1L, dto));
    }

    @Test
    void sendRequest_resendAfterRejected_success() {
        SendFriendRequestDTO dto = new SendFriendRequestDTO();
        dto.setFriendId(2L);

        Friendship existing = new Friendship();
        existing.setId(1L);
        existing.setStatus(2); // rejected
        when(friendshipMapper.findByUserPair(1L, 2L)).thenReturn(existing);
        when(userMapper.selectById(2L)).thenReturn(user2);
        when(friendshipMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> friendService.sendRequest(1L, dto));
        verify(friendshipMapper).updateById(any());
    }

    @Test
    void sendRequest_newRequest_success() {
        SendFriendRequestDTO dto = new SendFriendRequestDTO();
        dto.setFriendId(2L);
        dto.setVerificationMessage("Hello");

        when(friendshipMapper.findByUserPair(anyLong(), anyLong())).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(user2);
        when(friendshipMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> friendService.sendRequest(1L, dto));
        verify(friendshipMapper).insert(any());
    }

    // ==================== 接受/拒绝申请 ====================

    @Test
    void acceptRequest_success() {
        Friendship fs = new Friendship();
        fs.setId(1L);
        fs.setRequesterId(2L);
        fs.setReceiverId(1L);
        fs.setStatus(0);
        fs.setRequesterGroupId(null);

        when(friendshipMapper.selectById(1L)).thenReturn(fs);
        when(friendGroupMapper.findDefaultByUserId(1L)).thenReturn(defaultGroup1);
        when(friendGroupMapper.findDefaultByUserId(2L)).thenReturn(defaultGroup2);
        when(friendshipMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> friendService.acceptRequest(1L, 1L, null));
        verify(friendshipMapper).updateById(any());
    }

    @Test
    void acceptRequest_notReceiver_throws() {
        Friendship fs = new Friendship();
        fs.setId(1L);
        fs.setReceiverId(2L);
        fs.setStatus(0);

        when(friendshipMapper.selectById(1L)).thenReturn(fs);

        assertThrows(RuntimeException.class, () -> friendService.acceptRequest(1L, 1L, null));
    }

    @Test
    void acceptRequest_alreadyProcessed_throws() {
        Friendship fs = new Friendship();
        fs.setId(1L);
        fs.setReceiverId(1L);
        fs.setStatus(1); // already accepted

        when(friendshipMapper.selectById(1L)).thenReturn(fs);

        assertThrows(RuntimeException.class, () -> friendService.acceptRequest(1L, 1L, null));
    }

    @Test
    void rejectRequest_success() {
        when(friendshipMapper.selectById(1L)).thenReturn(pendingFriendship);
        when(friendshipMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> friendService.rejectRequest(2L, 1L));
        verify(friendshipMapper).updateById(any());
    }

    // ==================== 删除好友 ====================

    @Test
    void deleteFriend_success() {
        when(friendshipMapper.selectById(2L)).thenReturn(acceptedFriendship);
        when(friendshipMapper.deleteById(2L)).thenReturn(1);

        assertDoesNotThrow(() -> friendService.deleteFriend(1L, 2L));
        verify(friendshipMapper).deleteById(2L);
    }

    @Test
    void deleteFriend_notInvolved_throws() {
        Friendship fs = new Friendship();
        fs.setRequesterId(3L);
        fs.setReceiverId(4L);
        fs.setStatus(1);
        when(friendshipMapper.selectById(1L)).thenReturn(fs);

        assertThrows(RuntimeException.class, () -> friendService.deleteFriend(1L, 1L));
    }

    // ==================== 分组管理 ====================

    @Test
    void createGroup_duplicateName_throws() {
        CreateFriendGroupDTO dto = new CreateFriendGroupDTO();
        dto.setName("我的好友");

        when(friendGroupMapper.selectCount(any())).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> friendService.createGroup(1L, dto));
    }

    @Test
    void createGroup_success() {
        CreateFriendGroupDTO dto = new CreateFriendGroupDTO();
        dto.setName("同学");

        when(friendGroupMapper.selectCount(any())).thenReturn(0L);
        when(friendGroupMapper.insert(any())).thenReturn(1);

        FriendGroupVO result = friendService.createGroup(1L, dto);
        assertNotNull(result);
        assertEquals("同学", result.getName());
    }

    @Test
    void deleteGroup_defaultGroup_throws() {
        FriendGroup defaultGrp = buildGroup(1L, 1L, "我的好友", 1);
        defaultGrp.setIsDefault(1);
        when(friendGroupMapper.selectById(1L)).thenReturn(defaultGrp);

        assertThrows(RuntimeException.class, () -> friendService.deleteGroup(1L, 1L));
    }

    // ==================== 拉黑/取消拉黑 ====================

    @Test
    void blockFriend_success() {
        when(friendshipMapper.selectById(2L)).thenReturn(acceptedFriendship);
        when(friendshipMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> friendService.blockFriend(1L, 2L));
    }

    @Test
    void blockFriend_alreadyBlocked_throws() {
        Friendship blocked = new Friendship();
        blocked.setId(2L);
        blocked.setRequesterId(1L);
        blocked.setReceiverId(2L);
        blocked.setStatus(3);
        when(friendshipMapper.selectById(2L)).thenReturn(blocked);

        assertThrows(RuntimeException.class, () -> friendService.blockFriend(1L, 2L));
    }

    @Test
    void unblockFriend_success() {
        Friendship blocked = new Friendship();
        blocked.setId(2L);
        blocked.setRequesterId(1L);
        blocked.setReceiverId(2L);
        blocked.setStatus(3);
        when(friendshipMapper.selectById(2L)).thenReturn(blocked);
        when(friendshipMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> friendService.unblockFriend(1L, 2L));
    }

    @Test
    void unblockFriend_notBlocked_throws() {
        when(friendshipMapper.selectById(2L)).thenReturn(acceptedFriendship);

        assertThrows(RuntimeException.class, () -> friendService.unblockFriend(1L, 2L));
    }

    // ==================== 辅助方法 ====================

    private User buildUser(Long id, String username, String nickname, Integer status) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setStatus(status);
        u.setEnabled(1);
        u.setCreateTime(LocalDateTime.now());
        return u;
    }

    private FriendGroup buildGroup(Long id, Long userId, String name, Integer isDefault) {
        FriendGroup g = new FriendGroup();
        g.setId(id);
        g.setUserId(userId);
        g.setName(name);
        g.setSortOrder(0);
        g.setIsDefault(isDefault);
        return g;
    }
}
