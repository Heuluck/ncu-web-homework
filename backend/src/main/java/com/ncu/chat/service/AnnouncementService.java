package com.ncu.chat.service;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.model.entity.Announcement;

public interface AnnouncementService {
    PageResult<Announcement> listAnnouncements(int pageNum, int pageSize);
    PageResult<Announcement> listPublishedAnnouncements(int pageNum, int pageSize);
    Announcement createAnnouncement(Long publisherId, String title, String content, Integer isPublished);
    Announcement updateAnnouncement(Long id, String title, String content, Integer isPublished);
    void deleteAnnouncement(Long id);
}
