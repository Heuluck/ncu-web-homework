package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncu.chat.common.BusinessException;
import com.ncu.chat.mapper.*;
import com.ncu.chat.model.dto.CreateBotDTO;
import com.ncu.chat.model.dto.UpdateBotDTO;
import com.ncu.chat.model.entity.*;
import com.ncu.chat.model.vo.AiBotVO;
import com.ncu.chat.service.AiBotService;
import com.ncu.chat.util.CryptoUtil;
import com.ncu.chat.websocket.WebSocketGroupController;
import com.ncu.chat.model.vo.GroupMessageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AiBotServiceImpl implements AiBotService {

    private final AiBotMapper aiBotMapper;
    private final GroupBotMapper groupBotMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final UserMapper userMapper;
    private final CryptoUtil cryptoUtil;
    private final WebSocketGroupController webSocketGroupController;
    private final SimpMessagingTemplate messagingTemplate;

    /** 限流：botId -> 上次调用时间戳 */
    private final ConcurrentHashMap<Long, Long> lastCallTime = new ConcurrentHashMap<>();
    /** 限流：groupId -> 该分钟内调用次数 */
    private final ConcurrentHashMap<Long, Integer> groupCallCount = new ConcurrentHashMap<>();
    /** 限流：当前分钟时间戳（分钟级） */
    private volatile long currentMinute = 0;

    @Value("${bot.rate-limit.min-interval-ms:3000}")
    private long minIntervalMs;

    @Value("${bot.rate-limit.max-per-group-per-minute:10}")
    private int maxPerGroupPerMinute;

    @Value("${bot.context.max-messages:5}")
    private int maxContextMessages;

    @Value("${bot.context.max-chars-per-message:200}")
    private int maxCharsPerMessage;

    public AiBotServiceImpl(AiBotMapper aiBotMapper,
                            GroupBotMapper groupBotMapper,
                            GroupMemberMapper groupMemberMapper,
                            GroupMessageMapper groupMessageMapper,
                            ChatGroupMapper chatGroupMapper,
                            UserMapper userMapper,
                            CryptoUtil cryptoUtil,
                            @Lazy WebSocketGroupController webSocketGroupController,
                            SimpMessagingTemplate messagingTemplate) {
        this.aiBotMapper = aiBotMapper;
        this.groupBotMapper = groupBotMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.chatGroupMapper = chatGroupMapper;
        this.userMapper = userMapper;
        this.cryptoUtil = cryptoUtil;
        this.webSocketGroupController = webSocketGroupController;
        this.messagingTemplate = messagingTemplate;
    }

    // ==================== CRUD ====================

    @Override
    public AiBotVO createBot(Long userId, CreateBotDTO dto) {
        AiBot bot = new AiBot();
        bot.setOwnerId(userId);
        bot.setName(dto.getName());
        bot.setAvatar(dto.getAvatar());
        bot.setEndpoint(dto.getEndpoint());
        // 复制模式：从原机器人复制加密的 API Key
        if ("__copy__".equals(dto.getApiKey()) && dto.getCopyFromBotId() != null) {
            AiBot sourceBot = aiBotMapper.selectById(dto.getCopyFromBotId());
            if (sourceBot == null) throw new BusinessException("源机器人不存在");
            bot.setApiKeyEncrypted(sourceBot.getApiKeyEncrypted());
        } else {
            bot.setApiKeyEncrypted(cryptoUtil.encrypt(dto.getApiKey()));
        }
        bot.setModel(dto.getModel());
        bot.setSystemPrompt(dto.getSystemPrompt() != null ? dto.getSystemPrompt() : "你是一个友好的AI助手，请用中文回答问题。");
        bot.setTriggerType(dto.getTriggerType() != null ? dto.getTriggerType() : 0);
        bot.setTriggerProbability(dto.getTriggerProbability());
        bot.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : BigDecimal.ONE);
        bot.setTopP(dto.getTopP() != null ? dto.getTopP() : BigDecimal.ONE);
        aiBotMapper.insert(bot);
        return toVO(bot);
    }

    @Override
    public AiBotVO updateBot(Long userId, Long botId, UpdateBotDTO dto) {
        AiBot bot = aiBotMapper.selectById(botId);
        if (bot == null || !bot.getOwnerId().equals(userId)) {
            throw new BusinessException("无权操作此机器人");
        }
        if (dto.getName() != null) bot.setName(dto.getName());
        if (dto.getAvatar() != null) bot.setAvatar(dto.getAvatar());
        if (dto.getEndpoint() != null) bot.setEndpoint(dto.getEndpoint());
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            bot.setApiKeyEncrypted(cryptoUtil.encrypt(dto.getApiKey()));
        }
        if (dto.getModel() != null) bot.setModel(dto.getModel());
        if (dto.getSystemPrompt() != null) bot.setSystemPrompt(dto.getSystemPrompt());
        if (dto.getTriggerType() != null) bot.setTriggerType(dto.getTriggerType());
        if (dto.getTriggerProbability() != null) bot.setTriggerProbability(dto.getTriggerProbability());
        if (dto.getTemperature() != null) bot.setTemperature(dto.getTemperature());
        if (dto.getTopP() != null) bot.setTopP(dto.getTopP());
        aiBotMapper.updateById(bot);
        return toVO(bot);
    }

    @Override
    public void deleteBot(Long userId, Long botId) {
        AiBot bot = aiBotMapper.selectById(botId);
        if (bot == null || !bot.getOwnerId().equals(userId)) {
            throw new BusinessException("无权操作此机器人");
        }
        // 检查是否有群聊在使用此机器人
        Long usageCount = groupBotMapper.selectCount(
                new LambdaQueryWrapper<GroupBot>().eq(GroupBot::getBotId, botId));
        if (usageCount > 0) {
            throw new BusinessException("该机器人正在被 " + usageCount + " 个群聊使用，请先从群聊中移除后再删除");
        }
        aiBotMapper.deleteById(botId);
    }

    @Override
    public List<AiBotVO> getMyBots(Long userId) {
        List<AiBot> bots = aiBotMapper.selectList(
                new LambdaQueryWrapper<AiBot>().eq(AiBot::getOwnerId, userId));
        return bots.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void addBotToGroup(Long userId, Long groupId, Long botId) {
        // 验证用户是群主或管理员
        GroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
        if (member == null || member.getRole() < 1) {
            throw new BusinessException("只有群主或管理员可以添加机器人");
        }
        AiBot bot = aiBotMapper.selectById(botId);
        if (bot == null) throw new BusinessException("机器人不存在");

        // 检查是否已存在
        Long exists = groupBotMapper.selectCount(
                new LambdaQueryWrapper<GroupBot>()
                        .eq(GroupBot::getGroupId, groupId)
                        .eq(GroupBot::getBotId, botId));
        if (exists > 0) throw new BusinessException("该机器人已在群聊中");

        GroupBot gb = new GroupBot();
        gb.setGroupId(groupId);
        gb.setBotId(botId);
        gb.setAddedBy(userId);
        groupBotMapper.insert(gb);
    }

    @Override
    public void removeBotFromGroup(Long userId, Long groupId, Long botId) {
        GroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
        if (member == null) throw new BusinessException("你不是群成员");

        // 群主可以移除任何机器人；添加该机器人的管理员可以移除；机器人主人可以移除
        GroupBot gb = groupBotMapper.selectOne(
                new LambdaQueryWrapper<GroupBot>()
                        .eq(GroupBot::getGroupId, groupId)
                        .eq(GroupBot::getBotId, botId));
        if (gb == null) throw new BusinessException("该机器人不在此群聊中");

        AiBot bot = aiBotMapper.selectById(botId);
        boolean isOwner = member.getRole() == 2;
        boolean isAddedBy = gb.getAddedBy() != null && gb.getAddedBy().equals(userId);
        boolean isBotOwner = bot != null && bot.getOwnerId().equals(userId);

        if (!isOwner && !isAddedBy && !isBotOwner) {
            throw new BusinessException("只有群主、添加者或机器人主人可以移除机器人");
        }

        groupBotMapper.delete(
                new LambdaQueryWrapper<GroupBot>()
                        .eq(GroupBot::getGroupId, groupId)
                        .eq(GroupBot::getBotId, botId));
    }

    @Override
    public List<AiBotVO> getGroupBots(Long groupId) {
        List<GroupBot> gbs = groupBotMapper.selectList(
                new LambdaQueryWrapper<GroupBot>().eq(GroupBot::getGroupId, groupId));
        if (gbs.isEmpty()) return Collections.emptyList();
        List<Long> botIds = gbs.stream().map(GroupBot::getBotId).collect(Collectors.toList());
        List<AiBot> bots = aiBotMapper.selectBatchIds(botIds);
        Map<Long, Long> addedByMap = gbs.stream()
                .collect(Collectors.toMap(GroupBot::getBotId, gb -> gb.getAddedBy() != null ? gb.getAddedBy() : 0L));
        return bots.stream().map(b -> {
            AiBotVO vo = toVO(b);
            vo.setAddedBy(addedByMap.getOrDefault(b.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 触发逻辑 ====================

    @Override
    @Async
    public void checkAndTriggerBots(Long groupId, Long senderId, String content, int chainDepth) {
        // 链式触发最多 3 层
        if (chainDepth >= 3) return;

        try {
            // 获取群内所有机器人
            List<GroupBot> gbs = groupBotMapper.selectList(
                    new LambdaQueryWrapper<GroupBot>().eq(GroupBot::getGroupId, groupId));
            if (gbs.isEmpty()) return;

            List<Long> botIds = gbs.stream().map(GroupBot::getBotId).collect(Collectors.toList());
            List<AiBot> bots = aiBotMapper.selectBatchIds(botIds);
            if (bots.isEmpty()) return;

            // 获取发送者名称
            boolean isBotSender = senderId == null || senderId == 0L;
            User sender = isBotSender ? null : userMapper.selectById(senderId);
            String senderName = sender != null ? sender.getNickname() : "用户";

            for (AiBot bot : bots) {
                boolean shouldTrigger = false;
                boolean isAtTrigger = false;
                String userMessage = content;

                // @触发优先检查：无论 triggerType 如何，只要消息 @了该机器人就触发
                String atPrefix = "@" + bot.getName();
                if (content.contains(atPrefix)) {
                    shouldTrigger = true;
                    isAtTrigger = true;
                    int atIdx = content.indexOf(atPrefix);
                    userMessage = content.substring(atIdx + atPrefix.length()).trim();
                    if (userMessage.isEmpty()) userMessage = "你好";
                }

                // 非 @触发时，按 triggerType 判断（仅用户消息，机器人消息只响应 @）
                if (!shouldTrigger && !isBotSender) {
                    switch (bot.getTriggerType()) {
                        case 1: // 每次触发
                            shouldTrigger = true;
                            break;
                        case 2: // 随机概率
                            if (bot.getTriggerProbability() != null) {
                                shouldTrigger = Math.random() < bot.getTriggerProbability().doubleValue();
                            }
                            break;
                        default:
                            break;
                    }
                }

                if (shouldTrigger) {
                    // 限流检查
                    if (!checkRateLimit(groupId, bot.getId())) {
                        if (isAtTrigger) {
                            sendBotMessage(groupId, bot, "⚠️ AI 机器人暂时繁忙，请稍后再试。", chainDepth);
                        }
                        continue;
                    }
                    callAiAndReply(groupId, bot, userMessage, senderId, senderName, chainDepth);
                }
            }
        } catch (Exception e) {
            System.err.println("[Bot] 触发检查异常: " + e.getMessage());
        }
    }

    @Override
    public void callAiAndReply(Long groupId, AiBot bot, String userMessage, Long senderId, String senderName, int chainDepth) {
        try {
            String apiKey = cryptoUtil.decrypt(bot.getApiKeyEncrypted());

            // 构建上下文：最近 N 条消息（含本机器人自己的回复，保持对话连贯）
            List<GroupMessage> recentMsgs = groupMessageMapper.selectList(
                    new LambdaQueryWrapper<GroupMessage>()
                            .eq(GroupMessage::getGroupId, groupId)
                            .orderByDesc(GroupMessage::getCreateTime)
                            .last("LIMIT " + maxContextMessages));

            // 获取发送者名称映射 + 机器人名称映射
            Map<Long, String> nameMap = new HashMap<>();
            Map<Long, AiBot> botMap = new HashMap<>();
            for (GroupMessage m : recentMsgs) {
                if (m.getBotId() != null) {
                    if (!botMap.containsKey(m.getBotId())) {
                        botMap.put(m.getBotId(), aiBotMapper.selectById(m.getBotId()));
                    }
                } else if (!nameMap.containsKey(m.getSenderId())) {
                    User u = userMapper.selectById(m.getSenderId());
                    nameMap.put(m.getSenderId(), u != null ? u.getNickname() : "用户");
                }
            }

            // 构建 messages 数组
            List<Map<String, String>> messages = new ArrayList<>();
            // 系统提示词（注入群聊上下文）
            String systemPrompt = buildSystemPrompt(bot, groupId);
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                Map<String, String> sys = new HashMap<>();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
                messages.add(sys);
            }
            // 上下文消息（从旧到新），正确区分 assistant/user 角色
            for (int i = recentMsgs.size() - 1; i >= 0; i--) {
                GroupMessage m = recentMsgs.get(i);
                String text = m.getContent();
                if (text != null && text.length() > maxCharsPerMessage) {
                    text = text.substring(0, maxCharsPerMessage) + "...";
                }
                Map<String, String> ctx = new HashMap<>();

                if (m.getBotId() != null && m.getBotId().equals(bot.getId())) {
                    // 本机器人自己的消息 → assistant
                    ctx.put("role", "assistant");
                    ctx.put("content", text);
                } else if (m.getBotId() != null) {
                    // 其他机器人的消息 → user（模拟为普通用户发言）
                    AiBot other = botMap.get(m.getBotId());
                    String botName = other != null ? "🤖" + other.getName() : "机器人";
                    Long botOwnerId = other != null ? other.getOwnerId() : null;
                    String botOwnerInfo = botOwnerId != null ? "[主人ID:" + botOwnerId + "]" : "";
                    ctx.put("role", "user");
                    ctx.put("content", botName + botOwnerInfo + " 说：" + text);
                } else {
                    // 普通用户消息 → user
                    String name = nameMap.getOrDefault(m.getSenderId(), "用户");
                    ctx.put("role", "user");
                    ctx.put("content", "[用户:" + m.getSenderId() + "] " + name + " 说：" + text);
                }
                messages.add(ctx);
            }
            // 当前用户消息
            Map<String, String> user = new HashMap<>();
            user.put("role", "user");
            user.put("content", "[用户:" + senderId + "] " + senderName + " 说：" + userMessage);
            messages.add(user);

            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", bot.getModel());
            body.put("messages", messages);
            body.put("temperature", bot.getTemperature() != null ? bot.getTemperature().doubleValue() : 1.0);
            body.put("top_p", bot.getTopP() != null ? bot.getTopP().doubleValue() : 1.0);

            // 发送请求
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String url = bot.getEndpoint();
            if (!url.endsWith("/")) url += "/";
            url += "v1/chat/completions";

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        String reply = (String) message.get("content");
                        if (reply != null && !reply.isBlank()) {
                            sendBotMessage(groupId, bot, reply.trim(), chainDepth);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Bot] AI 调用失败: " + e.getMessage());
            sendBotMessage(groupId, bot, "⚠️ AI 调用失败：" + e.getMessage(), chainDepth);
        }
    }

    // ==================== 内部方法 ====================

    private boolean checkRateLimit(Long groupId, Long botId) {
        long now = System.currentTimeMillis();
        // 每机器人间隔检查
        Long last = lastCallTime.get(botId);
        if (last != null && (now - last) < minIntervalMs) {
            return false;
        }
        lastCallTime.put(botId, now);

        // 每群每分钟次数检查
        long thisMinute = now / 60000;
        if (thisMinute != currentMinute) {
            currentMinute = thisMinute;
            groupCallCount.clear();
        }
        int count = groupCallCount.getOrDefault(groupId, 0);
        if (count >= maxPerGroupPerMinute) {
            return false;
        }
        groupCallCount.put(groupId, count + 1);
        return true;
    }

    private void sendBotMessage(Long groupId, AiBot bot, String content, int chainDepth) {
        GroupMessage msg = new GroupMessage();
        msg.setGroupId(groupId);
        msg.setSenderId(0L);
        msg.setBotId(bot.getId());
        msg.setContent(content);
        msg.setMessageType(0);
        msg.setIsRecall(0);
        groupMessageMapper.insert(msg);

        GroupMessageVO vo = new GroupMessageVO();
        vo.setId(msg.getId());
        vo.setGroupId(groupId);
        vo.setSenderId(0L);
        vo.setBotId(bot.getId());
        vo.setBotName(bot.getName());
        vo.setBotAvatar(bot.getAvatar());
        vo.setSenderNickname(bot.getName());
        vo.setSenderAvatar(bot.getAvatar());
        vo.setContent(content);
        vo.setMessageType(0);
        vo.setCreateTime(msg.getCreateTime());
        vo.setIsSelf(false);

        webSocketGroupController.sendGroupMessage(vo);

        // 链式触发：机器人回复后检查是否 @ 了其他机器人
        try {
            checkAndTriggerBots(groupId, 0L, content, chainDepth + 1);
        } catch (Exception e) {
            System.err.println("[Bot] 链式触发异常: " + e.getMessage());
        }
    }

    /**
     * 构建带群聊上下文的系统提示词
     */
    private String buildSystemPrompt(AiBot bot, Long groupId) {
        StringBuilder sb = new StringBuilder();
        // 机器人自身信息
        sb.append("【你的名字】").append(bot.getName()).append("\n");
        if (bot.getSystemPrompt() != null && !bot.getSystemPrompt().isBlank()) {
            sb.append(bot.getSystemPrompt()).append("\n\n");
        }
        // 机器人主人信息
        User owner = userMapper.selectById(bot.getOwnerId());
        if (owner != null) {
            sb.append("【你的主人】").append(owner.getNickname()).append(" (ID: ").append(owner.getId()).append(")");
            sb.append("\n注意：你的主人是 ").append(owner.getNickname()).append("，你可以称呼ta。\n\n");
        }
        // 群聊基本信息
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group != null) {
            sb.append("【当前群聊】").append(group.getName()).append("\n");
            sb.append("【群公告】").append(group.getAnnouncement() != null ? group.getAnnouncement() : "无").append("\n");
            sb.append("【群成员人数】").append(group.getMemberCount()).append(" 人\n");
        }
        // 前 50 名群成员
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getDeleted, 0)
               .orderByDesc(GroupMember::getRole).last("LIMIT 50");
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        if (!members.isEmpty()) {
            List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
            }
            sb.append("【群成员列表】\n");
            for (GroupMember m : members) {
                User u = userMap.get(m.getUserId());
                String name = u != null ? u.getNickname() : "用户" + m.getUserId();
                String role = m.getRole() == 2 ? "(群主)" : m.getRole() == 1 ? "(管理员)" : "";
                sb.append("- ").append(name).append(role).append("\n");
            }
        }
        sb.append("\n注意：你在这个群聊中，请以群成员的身份自然回复。");
        return sb.toString();
    }

    private AiBotVO toVO(AiBot bot) {
        AiBotVO vo = new AiBotVO();
        BeanUtils.copyProperties(bot, vo);
        return vo;
    }
}
