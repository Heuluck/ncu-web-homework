package com.ncu.chat.controller;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.CreateGroupDTO;
import com.ncu.chat.model.dto.GroupMessageSendDTO;
import com.ncu.chat.model.dto.UpdateGroupDTO;
import com.ncu.chat.model.vo.*;
import com.ncu.chat.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public Result<GroupVO> createGroup(@Valid @RequestBody CreateGroupDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.createGroup(userId, dto));
    }

    @GetMapping("/info/{groupId}")
    public Result<GroupVO> getGroupInfo(@PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.getGroupInfo(userId, groupId));
    }

    @PutMapping("/info/{groupId}")
    public Result<GroupVO> updateGroupInfo(@PathVariable Long groupId, @Valid @RequestBody UpdateGroupDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.updateGroupInfo(userId, groupId, dto));
    }

    @DeleteMapping("/disband/{groupId}")
    public Result<Void> disbandGroup(@PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.disbandGroup(userId, groupId);
        return Result.success();
    }

    @GetMapping("/my-groups")
    public Result<List<GroupConversationVO>> getMyGroups(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.getMyGroups(userId));
    }

    @GetMapping("/{groupId}/members")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.getGroupMembers(userId, groupId));
    }

    @PostMapping("/{groupId}/invite")
    public Result<Void> inviteMembers(@PathVariable Long groupId, @RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.inviteMembers(userId, groupId, body.get("memberIds"));
        return Result.success();
    }

    @DeleteMapping("/{groupId}/member/{targetUserId}")
    public Result<Void> removeMember(@PathVariable Long groupId, @PathVariable Long targetUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.removeMember(userId, groupId, targetUserId);
        return Result.success();
    }

    @PutMapping("/{groupId}/admin/{targetUserId}")
    public Result<Void> setAdmin(@PathVariable Long groupId, @PathVariable Long targetUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.setAdmin(userId, groupId, targetUserId);
        return Result.success();
    }

    @PutMapping("/{groupId}/transfer/{targetUserId}")
    public Result<Void> transferOwner(@PathVariable Long groupId, @PathVariable Long targetUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.transferOwner(userId, groupId, targetUserId);
        return Result.success();
    }

    @PutMapping("/{groupId}/dnd")
    public Result<Void> setDoNotDisturb(@PathVariable Long groupId, @RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.setDoNotDisturb(userId, groupId, body.get("enabled"));
        return Result.success();
    }

    @PostMapping("/message/send")
    public Result<GroupMessageVO> sendMessage(@Valid @RequestBody GroupMessageSendDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.sendMessage(userId, dto));
    }

    @GetMapping("/{groupId}/messages")
    public Result<PageResult<GroupMessageVO>> getGroupMessages(@PathVariable Long groupId, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "20") Integer pageSize, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.getGroupMessages(userId, groupId, pageNum, pageSize));
    }

    @PutMapping("/{groupId}/read")
    public Result<Void> markRead(@PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.markRead(userId, groupId);
        return Result.success();
    }

    @GetMapping("/unread")
    public Result<List<GroupMessageVO>> getUnreadMessages(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(groupService.getUnreadGroupMessages(userId));
    }
}