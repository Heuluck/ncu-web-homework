package com.ncu.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncu.chat.model.dto.*;
import com.ncu.chat.model.vo.*;
import com.ncu.chat.service.FriendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendController.class)
class FriendControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FriendService friendService;

    // ==================== 搜索 ====================

    @Test
    void search_success() throws Exception {
        SearchUserVO vo = new SearchUserVO();
        vo.setId(2L);
        vo.setUsername("bob");
        vo.setNickname("Bob");

        when(friendService.searchUsers(anyLong(), anyString())).thenReturn(List.of(vo));

        mockMvc.perform(post("/api/friend/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].username").value("bob"));
    }

    // ==================== 发送申请 ====================

    @Test
    void sendRequest_success() throws Exception {
        doNothing().when(friendService).sendRequest(anyLong(), any());

        mockMvc.perform(post("/api/friend/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"friendId\":2,\"verificationMessage\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 申请列表 ====================

    @Test
    void receivedRequests_success() throws Exception {
        when(friendService.getReceivedRequests(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/friend/requests/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void sentRequests_success() throws Exception {
        when(friendService.getSentRequests(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/friend/requests/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 接受/拒绝 ====================

    @Test
    void acceptRequest_success() throws Exception {
        doNothing().when(friendService).acceptRequest(anyLong(), anyLong(), any());

        mockMvc.perform(put("/api/friend/request/1/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void rejectRequest_success() throws Exception {
        doNothing().when(friendService).rejectRequest(anyLong(), anyLong());

        mockMvc.perform(put("/api/friend/request/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void rejectRequest_serviceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("该申请已处理")).when(friendService).rejectRequest(anyLong(), anyLong());

        mockMvc.perform(put("/api/friend/request/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该申请已处理"));
    }

    // ==================== 删除 ====================

    @Test
    void deleteFriend_success() throws Exception {
        doNothing().when(friendService).deleteFriend(anyLong(), anyLong());

        mockMvc.perform(delete("/api/friend/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 好友列表 ====================

    @Test
    void friendList_success() throws Exception {
        when(friendService.getFriendList(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/friend/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 分组管理 ====================

    @Test
    void createGroup_success() throws Exception {
        FriendGroupVO vo = new FriendGroupVO();
        vo.setGroupId(1L);
        vo.setName("同学");
        when(friendService.createGroup(anyLong(), any())).thenReturn(vo);

        mockMvc.perform(post("/api/friend/group")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("同学"));
    }

    @Test
    void updateGroup_success() throws Exception {
        FriendGroupVO vo = new FriendGroupVO();
        vo.setGroupId(1L);
        vo.setName("同事");
        when(friendService.updateGroup(anyLong(), anyLong(), any())).thenReturn(vo);

        mockMvc.perform(put("/api/friend/group/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"同事\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("同事"));
    }

    @Test
    void deleteGroup_success() throws Exception {
        doNothing().when(friendService).deleteGroup(anyLong(), anyLong());

        mockMvc.perform(delete("/api/friend/group/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 移动好友 ====================

    @Test
    void moveFriend_success() throws Exception {
        doNothing().when(friendService).moveFriend(anyLong(), anyLong(), any());

        mockMvc.perform(put("/api/friend/1/move-group")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 拉黑/取消拉黑 ====================

    @Test
    void blockFriend_success() throws Exception {
        doNothing().when(friendService).blockFriend(anyLong(), anyLong());

        mockMvc.perform(put("/api/friend/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void unblockFriend_success() throws Exception {
        doNothing().when(friendService).unblockFriend(anyLong(), anyLong());

        mockMvc.perform(put("/api/friend/1/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
