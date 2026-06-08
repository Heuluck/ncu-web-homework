package com.ncu.chat.controller;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.common.Result;
import com.ncu.chat.model.entity.Announcement;
import com.ncu.chat.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公告公开接口（无需管理员权限）
 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 获取已发布的公告列表
     */
    @GetMapping
    public Result<?> listPublished(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Announcement> pageResult = announcementService.listPublishedAnnouncements(pageNum, pageSize);
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("content", a.getContent());
            m.put("createTime", a.getCreateTime());
            m.put("updateTime", a.getUpdateTime());
            return m;
        }).collect(Collectors.toList());
        return Result.success(new PageResult<>(records, pageResult.getTotal(), pageNum, pageSize));
    }
}
