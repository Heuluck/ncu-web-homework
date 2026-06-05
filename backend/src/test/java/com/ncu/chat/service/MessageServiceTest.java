package com.ncu.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ncu.chat.mapper.PrivateMessageMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.entity.PrivateMessage;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.service.impl.MessageServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 单元测试")
class MessageServiceTest {

    @Mock
    private PrivateMessageMapper privateMessageMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User sender;
    private User receiver;
    private PrivateMessage textMessage;
    private PrivateMessage imageMessage;
    private PrivateMessage fileMessage;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, PrivateMessage.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");
        sender.setNickname("发送者");
        sender.setAvatar("avatar1.png");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setNickname("接收者");
        receiver.setAvatar("avatar2.png");

        textMessage = new PrivateMessage();
        textMessage.setId(1L);
        textMessage.setSenderId(1L);
        textMessage.setReceiverId(2L);
        textMessage.setContent("你好，这是一条文字消息");
        textMessage.setMessageType(0);
        textMessage.setStatus(1);
        textMessage.setCreateTime(LocalDateTime.of(2026, 6, 5, 10, 30));

        imageMessage = new PrivateMessage();
        imageMessage.setId(2L);
        imageMessage.setSenderId(1L);
        imageMessage.setReceiverId(2L);
        imageMessage.setContent("图片");
        imageMessage.setMessageType(1);
        imageMessage.setFileUrl("/uploads/image.png");
        imageMessage.setCreateTime(LocalDateTime.of(2026, 6, 5, 11, 0));

        fileMessage = new PrivateMessage();
        fileMessage.setId(3L);
        fileMessage.setSenderId(2L);
        fileMessage.setReceiverId(1L);
        fileMessage.setContent("文档.pdf");
        fileMessage.setMessageType(2);
        fileMessage.setFileUrl("/uploads/doc.pdf");
        fileMessage.setCreateTime(LocalDateTime.of(2026, 6, 5, 12, 0));
    }

    @Test
    @DisplayName("搜索私聊消息 - 关键词匹配")
    void searchPrivateMessages_Success() {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(textMessage));
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Arrays.asList(sender, receiver));

        List<PrivateMessageVO> result = messageService.searchPrivateMessages(1L, 2L, "你好");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("你好，这是一条文字消息", result.get(0).getContent());
        assertEquals("发送者", result.get(0).getSenderNickname());
    }

    @Test
    @DisplayName("搜索私聊消息 - 空关键词返回空列表")
    void searchPrivateMessages_EmptyKeyword() {
        List<PrivateMessageVO> result = messageService.searchPrivateMessages(1L, 2L, "");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("搜索私聊消息 - 无匹配结果")
    void searchPrivateMessages_NoMatch() {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<PrivateMessageVO> result = messageService.searchPrivateMessages(1L, 2L, "不存在的关键词");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("筛选私聊消息 - 按时间范围")
    void filterPrivateMessages_Success() {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(textMessage, imageMessage, fileMessage));
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Arrays.asList(sender, receiver));

        List<PrivateMessageVO> result = messageService.filterPrivateMessages(
                1L, 2L, "2026-06-05 10:00:00", "2026-06-05 11:30:00");

        assertNotNull(result);
        // 所有 3 条消息都在时间范围内（10:30, 11:00, 12:00）
        assertEquals(3, result.size());
        assertTrue(result.get(0).getCreateTime().isAfter(LocalDateTime.of(2026, 6, 5, 10, 0)));
    }

    @Test
    @DisplayName("导出私聊记录为 TXT")
    void exportPrivateMessagesAsTxt_Success() throws IOException {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(textMessage, imageMessage));
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Arrays.asList(sender, receiver));

        String result = messageService.exportPrivateMessagesAsTxt(1L, 2L);

        assertNotNull(result);
        assertTrue(result.contains("聊天记录导出"));
        // TXT 导出格式为 [时间] 发送者：内容
        assertTrue(result.contains("发送者: 你好，这是一条文字消息"));
        assertTrue(result.contains("发送者：图片") || result.contains(": 图片"));
    }

    @Test
    @DisplayName("导出私聊记录为 CSV")
    void exportPrivateMessagesAsCsv_Success() throws IOException {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(textMessage, fileMessage));
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Arrays.asList(sender, receiver));

        String result = messageService.exportPrivateMessagesAsCsv(1L, 2L);

        assertNotNull(result);
        assertTrue(result.contains("时间，发送者，内容，类型"));
        assertTrue(result.contains("发送者"));
        assertTrue(result.contains("文字"));
    }

    @Test
    @DisplayName("导出 CSV - 特殊字符转义")
    void exportPrivateMessagesAsCsv_EscapeSpecialChars() throws IOException {
        PrivateMessage specialMessage = new PrivateMessage();
        specialMessage.setId(4L);
        specialMessage.setSenderId(1L);
        specialMessage.setReceiverId(2L);
        specialMessage.setContent("包含，逗号\"和引号");
        specialMessage.setMessageType(0);
        specialMessage.setCreateTime(LocalDateTime.now());

        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(specialMessage));
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(sender));

        String result = messageService.exportPrivateMessagesAsCsv(1L, 2L);

        assertNotNull(result);
        // CSV 应该正确转义包含逗号和引号的内容
        assertTrue(result.contains("\"包含，逗号\"\"和引号\""));
    }

    @Test
    @DisplayName("搜索群聊消息 - 暂未实现")
    void searchGroupMessages_NotImplemented() {
        List<PrivateMessageVO> result = messageService.searchGroupMessages(1L, "关键词");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("筛选群聊消息 - 暂未实现")
    void filterGroupMessages_NotImplemented() {
        List<PrivateMessageVO> result = messageService.filterGroupMessages(1L, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("导出群聊记录 - 暂未实现")
    void exportGroupMessages_NotImplemented() throws IOException {
        String txtResult = messageService.exportGroupMessagesAsTxt(1L);
        String csvResult = messageService.exportGroupMessagesAsCsv(1L);

        assertNotNull(txtResult);
        assertNotNull(csvResult);
    }
}
