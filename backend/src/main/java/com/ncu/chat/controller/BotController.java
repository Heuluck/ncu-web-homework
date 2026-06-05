package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.CreateBotDTO;
import com.ncu.chat.model.dto.UpdateBotDTO;
import com.ncu.chat.model.vo.AiBotVO;
import com.ncu.chat.service.AiBotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotController {

    private final AiBotService aiBotService;

    @PostMapping("/create")
    public Result<AiBotVO> createBot(@Valid @RequestBody CreateBotDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(aiBotService.createBot(userId, dto));
    }

    @GetMapping("/my-bots")
    public Result<List<AiBotVO>> getMyBots(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(aiBotService.getMyBots(userId));
    }

    @PutMapping("/{botId}")
    public Result<AiBotVO> updateBot(@PathVariable Long botId, @RequestBody UpdateBotDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(aiBotService.updateBot(userId, botId, dto));
    }

    @DeleteMapping("/{botId}")
    public Result<Void> deleteBot(@PathVariable Long botId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        aiBotService.deleteBot(userId, botId);
        return Result.success(null);
    }

    @PostMapping("/{botId}/group/{groupId}")
    public Result<Void> addBotToGroup(@PathVariable Long botId, @PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        aiBotService.addBotToGroup(userId, groupId, botId);
        return Result.success(null);
    }

    @DeleteMapping("/{botId}/group/{groupId}")
    public Result<Void> removeBotFromGroup(@PathVariable Long botId, @PathVariable Long groupId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        aiBotService.removeBotFromGroup(userId, groupId, botId);
        return Result.success(null);
    }

    @GetMapping("/group/{groupId}")
    public Result<List<AiBotVO>> getGroupBots(@PathVariable Long groupId) {
        return Result.success(aiBotService.getGroupBots(groupId));
    }
}
