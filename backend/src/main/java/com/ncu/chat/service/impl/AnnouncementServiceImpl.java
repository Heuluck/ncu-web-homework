package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.mapper.AnnouncementMapper;
import com.ncu.chat.model.entity.Announcement;
import com.ncu.chat.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public PageResult<Announcement> listAnnouncements(int pageNum, int pageSize) {
        Page<Announcement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getUpdateTime);
        Page<Announcement> result = announcementMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public Announcement createAnnouncement(Long publisherId, String title, String content, Integer isPublished) {
        Announcement announcement = new Announcement();
        announcement.setPublisherId(publisherId);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setIsPublished(isPublished);
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    public Announcement updateAnnouncement(Long id, String title, String content, Integer isPublished) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        if (title != null) announcement.setTitle(title);
        if (content != null) announcement.setContent(content);
        if (isPublished != null) announcement.setIsPublished(isPublished);
        announcementMapper.updateById(announcement);
        return announcement;
    }

    @Override
    public void deleteAnnouncement(Long id) {
        announcementMapper.deleteById(id);
    }
}
