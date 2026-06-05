package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 消息中心控制器（聊天记录查询、搜索、导出）
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 搜索私聊聊天记录
     */
    @GetMapping("/search/private")
    public Result<List<PrivateMessageVO>> searchPrivate(
            @RequestParam Long targetId,
            @RequestParam String keyword,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<PrivateMessageVO> messages = messageService.searchPrivateMessages(userId, targetId, keyword);
        return Result.success(messages);
    }

    /**
     * 按时间范围筛选私聊记录
     */
    @GetMapping("/filter/private")
    public Result<List<PrivateMessageVO>> filterPrivate(
            @RequestParam Long targetId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<PrivateMessageVO> messages = messageService.filterPrivateMessages(userId, targetId, startTime, endTime);
        return Result.success(messages);
    }

    /**
     * 导出私聊记录为 TXT
     */
    @GetMapping("/export/private/txt")
    public void exportPrivateTxt(
            @RequestParam Long targetId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        String content = messageService.exportPrivateMessagesAsTxt(userId, targetId);
        downloadFile(response, content, "chat_history_" + targetId + ".txt");
    }

    /**
     * 导出私聊记录为 CSV
     */
    @GetMapping("/export/private/csv")
    public void exportPrivateCsv(
            @RequestParam Long targetId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        String content = messageService.exportPrivateMessagesAsCsv(userId, targetId);
        downloadFile(response, content, "chat_history_" + targetId + ".csv");
    }

    /**
     * 搜索群聊聊天记录
     */
    @GetMapping("/search/group")
    public Result<List<PrivateMessageVO>> searchGroup(
            @RequestParam Long groupId,
            @RequestParam String keyword) {
        List<PrivateMessageVO> messages = messageService.searchGroupMessages(groupId, keyword);
        return Result.success(messages);
    }

    /**
     * 按时间范围筛选群聊记录
     */
    @GetMapping("/filter/group")
    public Result<List<PrivateMessageVO>> filterGroup(
            @RequestParam Long groupId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        List<PrivateMessageVO> messages = messageService.filterGroupMessages(groupId, startTime, endTime);
        return Result.success(messages);
    }

    /**
     * 导出群聊记录为 TXT
     */
    @GetMapping("/export/group/txt")
    public void exportGroupTxt(
            @RequestParam Long groupId,
            HttpServletResponse response) throws IOException {
        String content = messageService.exportGroupMessagesAsTxt(groupId);
        downloadFile(response, content, "group_chat_" + groupId + ".txt");
    }

    /**
     * 导出群聊记录为 CSV
     */
    @GetMapping("/export/group/csv")
    public void exportGroupCsv(
            @RequestParam Long groupId,
            HttpServletResponse response) throws IOException {
        String content = messageService.exportGroupMessagesAsCsv(groupId);
        downloadFile(response, content, "group_chat_" + groupId + ".csv");
    }

    // --- 辅助方法 ---

    /**
     * 下载文件（带 BOM 头，防止中文乱码）
     */
    private void downloadFile(HttpServletResponse response, String content, String filename) throws IOException {
        // 添加 UTF-8 BOM 头：EF BB BF
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] allBytes = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, allBytes, 0, bom.length);
        System.arraycopy(contentBytes, 0, allBytes, bom.length, contentBytes.length);

        response.reset();
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()));
        response.setContentLength(allBytes.length);
        response.getOutputStream().write(allBytes);
        response.getOutputStream().flush();
    }
}
