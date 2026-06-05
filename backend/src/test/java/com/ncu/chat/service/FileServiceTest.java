package com.ncu.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.mapper.FileResourceMapper;
import com.ncu.chat.model.entity.FileResource;
import com.ncu.chat.service.impl.FileServiceImpl;
import com.ncu.chat.util.FileUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 单元测试")
class FileServiceTest {

    @Mock
    private FileUtil fileUtil;

    @Mock
    private FileResourceMapper fileResourceMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    private FileResource sampleFileResource;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, FileResource.class);
    }

    @BeforeEach
    void setUp() {
        sampleFileResource = new FileResource();
        sampleFileResource.setId(1L);
        sampleFileResource.setFileName("test.png");
        sampleFileResource.setFileUrl("/uploads/test.png");
        sampleFileResource.setFilePath("test.png");
        sampleFileResource.setFileSize(1024L);
        sampleFileResource.setFileType("image/png");
        sampleFileResource.setUploaderId(1L);
        sampleFileResource.setCategory(1); // 图片
    }

    @Test
    @DisplayName("上传图片文件")
    void uploadImageFile_Success() throws IOException {
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test content".getBytes()
        );

        when(fileUtil.upload(mockFile)).thenReturn("/uploads/test.png");
        when(fileResourceMapper.insert(any(FileResource.class))).thenAnswer(invocation -> {
            FileResource fr = invocation.getArgument(0);
            fr.setId(1L);
            return 1;
        });

        FileResource result = fileService.uploadFile(mockFile, 1L, 1);

        assertNotNull(result);
        assertEquals("test.png", result.getFileName());
        assertEquals("/uploads/test.png", result.getFileUrl());
        assertEquals(1L, result.getUploaderId());
        assertEquals(1, result.getCategory());
        verify(fileUtil, times(1)).upload(mockFile);
        verify(fileResourceMapper, times(1)).insert(any(FileResource.class));
    }

    @Test
    @DisplayName("上传通用文件")
    void uploadGenericFile_Success() throws IOException {
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(fileUtil.upload(mockFile)).thenReturn("/uploads/document.pdf");
        when(fileResourceMapper.insert(any(FileResource.class))).thenAnswer(invocation -> {
            FileResource fr = invocation.getArgument(0);
            fr.setId(2L);
            return 1;
        });

        FileResource result = fileService.uploadFile(mockFile, 1L, 0);

        assertNotNull(result);
        assertEquals("document.pdf", result.getFileName());
        assertEquals(0, result.getCategory());
    }

    @Test
    @DisplayName("上传音频文件")
    void uploadAudioFile_Success() throws IOException {
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "voice.mp3",
                "audio/mpeg",
                "audio content".getBytes()
        );

        when(fileUtil.upload(mockFile)).thenReturn("/uploads/voice.mp3");
        when(fileResourceMapper.insert(any(FileResource.class))).thenAnswer(invocation -> {
            FileResource fr = invocation.getArgument(0);
            fr.setId(3L);
            return 1;
        });

        FileResource result = fileService.uploadFile(mockFile, 1L, 2);

        assertNotNull(result);
        assertEquals("voice.mp3", result.getFileName());
        assertEquals(2, result.getCategory());
    }

    @Test
    @DisplayName("根据 URL 获取文件资源")
    void getByUrl_Success() {
        when(fileResourceMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(sampleFileResource);

        FileResource result = fileService.getByUrl("/uploads/test.png");

        assertNotNull(result);
        assertEquals("test.png", result.getFileName());
        assertEquals("/uploads/test.png", result.getFileUrl());
    }

    @Test
    @DisplayName("根据 URL 获取文件资源 - 不存在")
    void getByUrl_NotFound() {
        when(fileResourceMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        FileResource result = fileService.getByUrl("/uploads/not-exist.png");

        assertNull(result);
    }

    @Test
    @DisplayName("获取用户文件列表 - 分页")
    void getUserFiles_Success() {
        Page<FileResource> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleFileResource));
        page.setTotal(1);

        when(fileResourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        List<FileResource> result = fileService.getUserFiles(1L, 1, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.png", result.get(0).getFileName());
    }

    @Test
    @DisplayName("获取用户文件列表 - 不指定分类")
    void getUserFiles_NoCategory() {
        Page<FileResource> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleFileResource));
        page.setTotal(1);

        when(fileResourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        List<FileResource> result = fileService.getUserFiles(1L, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取用户文件列表 - 空结果")
    void getUserFiles_Empty() {
        Page<FileResource> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(fileResourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        List<FileResource> result = fileService.getUserFiles(1L, null, 1, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
