package com.ncu.chat.service;

import com.ncu.chat.model.entity.FileResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件资源服务
 */
public interface FileService {

    /**
     * 上传文件并记录到数据库
     * @param file 上传的文件
     * @param uploaderId 上传者 ID
     * @param category 文件分类：0-通用文件 1-图片 2-音频
     * @return 文件资源信息
     */
    FileResource uploadFile(MultipartFile file, Long uploaderId, Integer category) throws IOException;

    /**
     * 根据 URL 获取文件资源
     */
    FileResource getByUrl(String fileUrl);

    /**
     * 获取用户的文件列表
     */
    List<FileResource> getUserFiles(Long uploaderId, Integer category, Integer pageNum, Integer pageSize);
}
