package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.mapper.FileResourceMapper;
import com.ncu.chat.model.entity.FileResource;
import com.ncu.chat.service.FileService;
import com.ncu.chat.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件资源服务实现类
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileUtil fileUtil;
    private final FileResourceMapper fileResourceMapper;

    @Override
    public FileResource uploadFile(MultipartFile file, Long uploaderId, Integer category) throws IOException {
        // 使用 FileUtil 上传文件
        String url = fileUtil.upload(file);
        
        // 创建文件资源记录
        FileResource resource = new FileResource();
        resource.setFileName(file.getOriginalFilename());
        resource.setFileUrl(url);
        resource.setFilePath(url.replace("/uploads/", ""));
        resource.setFileSize(file.getSize());
        resource.setFileType(file.getContentType());
        resource.setUploaderId(uploaderId);
        resource.setCategory(category != null ? category : 0);
        
        fileResourceMapper.insert(resource);
        return resource;
    }

    @Override
    public FileResource getByUrl(String fileUrl) {
        LambdaQueryWrapper<FileResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileResource::getFileUrl, fileUrl);
        return fileResourceMapper.selectOne(wrapper);
    }

    @Override
    public List<FileResource> getUserFiles(Long uploaderId, Integer category, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<FileResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileResource::getUploaderId, uploaderId);
        if (category != null) {
            wrapper.eq(FileResource::getCategory, category);
        }
        wrapper.orderByDesc(FileResource::getCreateTime);
        
        Page<FileResource> page = new Page<>(pageNum, pageSize);
        Page<FileResource> result = fileResourceMapper.selectPage(page, wrapper);
        return result.getRecords();
    }
}
