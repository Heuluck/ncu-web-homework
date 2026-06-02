package com.ncu.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.mapper.PrivateMessageMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.dto.PrivateMessageSendDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ncu.chat.model.entity.PrivateMessage;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.vo.ConversationVO;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.service.impl.PrivateMessageServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateMessageService 单元测试")
class PrivateMessageServiceTest {

    @Mock
    private PrivateMessageMapper privateMessageMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private PrivateMessageServiceImpl privateMessageService;

    private User sender;
    private User receiver;
    private PrivateMessage sampleMessage;

    @BeforeAll
    static void initMybatisPlusCache() {
        // 初始化 MyBatis-Plus Lambda 缓存（单元测试无 Spring 上下文时需要）
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
        sender.setStatus(1);

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setNickname("接收者");
        receiver.setAvatar("avatar2.png");
        receiver.setStatus(0);

        sampleMessage = new PrivateMessage();
        sampleMessage.setId(1L);
        sampleMessage.setSenderId(1L);
        sampleMessage.setReceiverId(2L);
        sampleMessage.setContent("你好");
        sampleMessage.setMessageType(0);
        sampleMessage.setStatus(0);
        sampleMessage.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("发送消息 - 正常流程")
    void sendMessage_Success() {
        when(userMapper.selectById(1L)).thenReturn(sender);
        when(privateMessageMapper.insert(any(PrivateMessage.class))).thenAnswer(invocation -> {
            PrivateMessage pm = invocation.getArgument(0);
            pm.setId(1L);
            pm.setCreateTime(LocalDateTime.now());
            return 1;
        });

        PrivateMessageSendDTO dto = new PrivateMessageSendDTO();
        dto.setReceiverId(2L);
        dto.setContent("你好");
        dto.setMessageType(0);

        PrivateMessageVO vo = privateMessageService.sendMessage(1L, dto);

        assertNotNull(vo);
        assertEquals(1L, vo.getSenderId());
        assertEquals(2L, vo.getReceiverId());
        assertEquals("你好", vo.getContent());
        assertEquals("发送者", vo.getSenderNickname());
        verify(privateMessageMapper, times(1)).insert(any(PrivateMessage.class));
    }

    @Test
    @DisplayName("发送消息 - 带文件URL")
    void sendMessage_WithFileUrl() {
        when(userMapper.selectById(1L)).thenReturn(sender);
        when(privateMessageMapper.insert(any(PrivateMessage.class))).thenAnswer(invocation -> {
            PrivateMessage pm = invocation.getArgument(0);
            pm.setId(2L);
            pm.setCreateTime(LocalDateTime.now());
            return 1;
        });

        PrivateMessageSendDTO dto = new PrivateMessageSendDTO();
        dto.setReceiverId(2L);
        dto.setContent("photo.jpg");
        dto.setMessageType(1);
        dto.setFileUrl("/uploads/photo.jpg");

        PrivateMessageVO vo = privateMessageService.sendMessage(1L, dto);

        assertNotNull(vo);
        assertEquals(1, vo.getMessageType());
        assertEquals("/uploads/photo.jpg", vo.getFileUrl());
    }

    @Test
    @DisplayName("获取历史消息 - 正常分页")
    void getHistory_Success() {
        Page<PrivateMessage> page = new Page<>(1, 20);
        page.setRecords(Collections.singletonList(sampleMessage));
        page.setTotal(1);

        when(privateMessageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Arrays.asList(sender, receiver));

        PageResult<PrivateMessageVO> result = privateMessageService.getHistory(1L, 2L, 1, 20);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
    }

    @Test
    @DisplayName("获取未读消息")
    void getUnreadMessages_Success() {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(sampleMessage));
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(sender));

        List<PrivateMessageVO> result = privateMessageService.getUnreadMessages(2L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getStatus());
    }

    @Test
    @DisplayName("标记已读")
    void markAsRead_Success() {
        when(privateMessageMapper.markAsRead(2L, 1L)).thenReturn(1);

        privateMessageService.markAsRead(2L, 1L);

        verify(privateMessageMapper, times(1)).markAsRead(2L, 1L);
    }

    @Test
    @DisplayName("获取最近会话 - 无消息时返回空列表")
    void getRecentConversations_Empty() {
        when(privateMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ConversationVO> result = privateMessageService.getRecentConversations(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
