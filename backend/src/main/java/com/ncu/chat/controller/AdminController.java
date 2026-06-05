package com.ncu.chat.controller;

import com.ncu.chat.common.BusinessException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.common.Result;
import com.ncu.chat.mapper.ChatGroupMapper;
import com.ncu.chat.mapper.GroupMemberMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.entity.ChatGroup;
import com.ncu.chat.model.entity.GroupMember;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.service.AnnouncementService;
import com.ncu.chat.service.SensitiveWordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final AnnouncementService announcementService;
    private final SensitiveWordService sensitiveWordService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 校验管理员权限
     */
    private void checkAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException("无权访问，需要管理员权限");
        }
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        checkAdmin(request);

        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(User::getRole, 1); // 不显示其他管理员，或也可以显示所有
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = result.getRecords().stream().map(u -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            m.put("status", u.getStatus());
            m.put("role", u.getRole());
            m.put("enabled", u.getEnabled());
            m.put("createTime", u.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(records, result.getTotal(), pageNum, pageSize));
    }

    @PutMapping("/users/{id}/toggle-status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        checkAdmin(request);
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        Integer enabled = body.get("enabled");
        if (enabled == null) throw new BusinessException("缺少 enabled 参数");
        user.setEnabled(enabled);
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.success();
    }

    // ==================== 群聊管理 ====================

    @GetMapping("/groups")
    public Result<PageResult<Map<String, Object>>> listGroups(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkAdmin(request);

        Page<ChatGroup> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChatGroup> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ChatGroup::getName, keyword);
        }
        wrapper.orderByDesc(ChatGroup::getCreateTime);
        Page<ChatGroup> result = chatGroupMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = result.getRecords().stream().map(g -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("avatar", g.getAvatar());
            m.put("createTime", g.getCreateTime());

            // 群主
            User owner = userMapper.selectById(g.getOwnerId());
            m.put("ownerName", owner != null ? owner.getNickname() : "未知");

            // 成员数
            Long memberCount = groupMemberMapper.selectCount(
                    new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, g.getId()));
            m.put("memberCount", memberCount);
            return m;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(records, result.getTotal(), pageNum, pageSize));
    }

    @DeleteMapping("/groups/{id}")
    public Result<Void> disbandGroup(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        ChatGroup group = chatGroupMapper.selectById(id);
        if (group == null) throw new BusinessException("群聊不存在");
        // 删除群聊及成员
        groupMemberMapper.delete(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, id));
        chatGroupMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 公告管理 ====================

    @GetMapping("/announcements")
    public Result<?> listAnnouncements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        checkAdmin(request);
        var pageResult = announcementService.listAnnouncements(pageNum, pageSize);
        // Enrich with publisher name
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("content", a.getContent());
            m.put("isPublished", a.getIsPublished());
            m.put("createTime", a.getCreateTime());
            m.put("updateTime", a.getUpdateTime());
            User publisher = userMapper.selectById(a.getPublisherId());
            m.put("publisherName", publisher != null ? publisher.getNickname() : "未知");
            return m;
        }).collect(Collectors.toList());
        return Result.success(new PageResult<>(records, pageResult.getTotal(), pageNum, pageSize));
    }

    @PostMapping("/announcements")
    public Result<?> createAnnouncement(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        checkAdmin(request);
        Long userId = (Long) request.getAttribute("userId");
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Integer isPublished = body.get("isPublished") != null ? ((Number) body.get("isPublished")).intValue() : 0;
        if (title == null || title.isEmpty()) throw new BusinessException("标题不能为空");
        return Result.success(announcementService.createAnnouncement(userId, title, content, isPublished));
    }

    @PutMapping("/announcements/{id}")
    public Result<?> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        checkAdmin(request);
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Integer isPublished = body.get("isPublished") != null ? ((Number) body.get("isPublished")).intValue() : null;
        return Result.success(announcementService.updateAnnouncement(id, title, content, isPublished));
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    // ==================== 敏感词管理 ====================

    @GetMapping("/sensitive-words")
    public Result<?> listSensitiveWords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(sensitiveWordService.listSensitiveWords(pageNum, pageSize, keyword != null ? keyword : ""));
    }

    @PostMapping("/sensitive-words")
    public Result<?> addSensitiveWord(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        checkAdmin(request);
        String word = (String) body.get("word");
        String category = (String) body.get("category");
        Integer enabled = body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : 1;
        if (word == null || word.isEmpty()) throw new BusinessException("敏感词不能为空");
        return Result.success(sensitiveWordService.addSensitiveWord(word, category, enabled));
    }

    @PutMapping("/sensitive-words/{id}")
    public Result<?> updateSensitiveWord(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        checkAdmin(request);
        String word = (String) body.get("word");
        String category = (String) body.get("category");
        Integer enabled = body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : null;
        return Result.success(sensitiveWordService.updateSensitiveWord(id, word, category, enabled));
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        sensitiveWordService.deleteSensitiveWord(id);
        return Result.success();
    }

    @PostMapping("/sensitive-words/check")
    public Result<?> checkSensitiveWords(@RequestBody Map<String, String> body, HttpServletRequest request) {
        checkAdmin(request);
        String text = body.get("text");
        return Result.success(sensitiveWordService.checkSensitiveWords(text));
    }
}
