package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.*;
import com.ncu.chat.model.vo.*;
import com.ncu.chat.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /** POST /api/friend/search — 搜索用户 */
    @PostMapping("/search")
    public Result<List<SearchUserVO>> search(@RequestBody SearchUserDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(friendService.searchUsers(userId, dto.getKeyword()));
    }

    /** POST /api/friend/request — 发送好友申请 */
    @PostMapping("/request")
    public Result<Void> sendRequest(@RequestBody SendFriendRequestDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.sendRequest(userId, dto);
        return Result.success();
    }

    /** GET /api/friend/requests/received — 收到的申请 */
    @GetMapping("/requests/received")
    public Result<List<FriendRequestVO>> receivedRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(friendService.getReceivedRequests(userId));
    }

    /** GET /api/friend/requests/sent — 发出的申请 */
    @GetMapping("/requests/sent")
    public Result<List<FriendRequestVO>> sentRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(friendService.getSentRequests(userId));
    }

    /** PUT /api/friend/request/{friendshipId}/accept — 接受申请 */
    @PutMapping("/request/{friendshipId}/accept")
    public Result<Void> acceptRequest(@PathVariable Long friendshipId,
                                      @RequestBody(required = false) Map<String, Long> body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long groupId = body != null ? body.get("groupId") : null;
        friendService.acceptRequest(userId, friendshipId, groupId);
        return Result.success();
    }

    /** PUT /api/friend/request/{friendshipId}/reject — 拒绝申请 */
    @PutMapping("/request/{friendshipId}/reject")
    public Result<Void> rejectRequest(@PathVariable Long friendshipId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.rejectRequest(userId, friendshipId);
        return Result.success();
    }

    /** DELETE /api/friend/{friendshipId} — 删除好友 */
    @DeleteMapping("/{friendshipId}")
    public Result<Void> deleteFriend(@PathVariable Long friendshipId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.deleteFriend(userId, friendshipId);
        return Result.success();
    }

    /** GET /api/friend/list — 好友列表（按分组） */
    @GetMapping("/list")
    public Result<List<FriendGroupVO>> friendList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(friendService.getFriendList(userId));
    }

    /** POST /api/friend/group — 创建分组 */
    @PostMapping("/group")
    public Result<FriendGroupVO> createGroup(@Valid @RequestBody CreateFriendGroupDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(friendService.createGroup(userId, dto));
    }

    /** PUT /api/friend/group/{groupId} — 重命名分组 */
    @PutMapping("/group/{groupId}")
    public Result<FriendGroupVO> updateGroup(@PathVariable Long groupId,
                                              @Valid @RequestBody UpdateFriendGroupDTO dto,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(friendService.updateGroup(userId, groupId, dto));
    }

    /** DELETE /api/friend/group/{groupId} — 删除分组 */
    @DeleteMapping("/group/{groupId}")
    public Result<Void> deleteGroup(@PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.deleteGroup(userId, groupId);
        return Result.success();
    }

    /** PUT /api/friend/{friendshipId}/move-group — 移动好友到分组 */
    @PutMapping("/{friendshipId}/move-group")
    public Result<Void> moveFriend(@PathVariable Long friendshipId,
                                    @RequestBody MoveFriendDTO dto,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.moveFriend(userId, friendshipId, dto);
        return Result.success();
    }

    /** PUT /api/friend/{friendshipId}/block — 拉黑 */
    @PutMapping("/{friendshipId}/block")
    public Result<Void> blockFriend(@PathVariable Long friendshipId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.blockFriend(userId, friendshipId);
        return Result.success();
    }

    /** PUT /api/friend/{friendshipId}/unblock — 取消拉黑 */
    @PutMapping("/{friendshipId}/unblock")
    public Result<Void> unblockFriend(@PathVariable Long friendshipId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.unblockFriend(userId, friendshipId);
        return Result.success();
    }
}
