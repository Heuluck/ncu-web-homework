package com.ncu.chat.controller;

import com.ncu.chat.service.MessageService;
import com.ncu.chat.model.vo.PrivateMessageVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @Test
    void searchPrivateMessages_success() throws Exception {
        PrivateMessageVO msg = new PrivateMessageVO();
        msg.setId(1L);
        msg.setSenderId(1L);
        msg.setReceiverId(2L);
        msg.setContent("测试消息");
        msg.setMessageType(0);
        msg.setCreateTime(LocalDateTime.now());
        msg.setSenderNickname("发送者");

        List<PrivateMessageVO> messages = Arrays.asList(msg);
        when(messageService.searchPrivateMessages(eq(1L), eq(2L), any(String.class)))
                .thenReturn(messages);

        mockMvc.perform(get("/api/message/search/private")
                .param("targetId", "2")
                .param("keyword", "测试")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("测试消息"));
    }

    @Test
    void searchPrivateMessages_emptyKeyword() throws Exception {
        when(messageService.searchPrivateMessages(any(Long.class), any(Long.class), eq("")))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/message/search/private")
                .param("targetId", "2")
                .param("keyword", "")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void filterPrivateMessages_success() throws Exception {
        PrivateMessageVO msg = new PrivateMessageVO();
        msg.setId(1L);
        msg.setSenderId(1L);
        msg.setReceiverId(2L);
        msg.setContent("筛选结果");
        msg.setMessageType(0);
        msg.setCreateTime(LocalDateTime.now());

        List<PrivateMessageVO> messages = Arrays.asList(msg);
        when(messageService.filterPrivateMessages(any(Long.class), any(Long.class), any(String.class), any(String.class)))
                .thenReturn(messages);

        mockMvc.perform(get("/api/message/filter/private")
                .param("targetId", "2")
                .param("startTime", "2026-06-05 00:00:00")
                .param("endTime", "2026-06-05 23:59:59")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("筛选结果"));
    }

    @Test
    void exportPrivateTxt_success() throws Exception {
        String txtContent = "聊天记录导出\n2026-06-05 10:30:00 发送者：测试消息\n";
        when(messageService.exportPrivateMessagesAsTxt(any(Long.class), any(Long.class)))
                .thenReturn(txtContent);

        mockMvc.perform(get("/api/message/export/private/txt")
                .param("targetId", "2")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.mockito.ArgumentMatchers.contains("attachment")))
                .andExpect(header().string("Content-Type", "application/octet-stream"));
    }

    @Test
    void exportPrivateCsv_success() throws Exception {
        String csvContent = "时间，发送者，内容，类型\n2026-06-05 10:30:00，发送者，测试消息，文字\n";
        when(messageService.exportPrivateMessagesAsCsv(any(Long.class), any(Long.class)))
                .thenReturn(csvContent);

        mockMvc.perform(get("/api/message/export/private/csv")
                .param("targetId", "2")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.mockito.ArgumentMatchers.contains("attachment")));
    }

    @Test
    void searchGroupMessages_notImplemented() throws Exception {
        when(messageService.searchGroupMessages(any(Long.class), any(String.class)))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/message/search/group")
                .param("groupId", "1")
                .param("keyword", "测试")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void exportGroupTxt_notImplemented() throws Exception {
        when(messageService.exportGroupMessagesAsTxt(any(Long.class)))
                .thenReturn("群聊消息导出功能待实现");

        mockMvc.perform(get("/api/message/export/group/txt")
                .param("groupId", "1")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("群聊消息导出功能待实现"));
    }
}
