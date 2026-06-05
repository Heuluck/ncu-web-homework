package com.ncu.chat.util;

import com.ncu.chat.common.BusinessException;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class FileUtil {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.allowed-types}")
    private String allowedTypes;

    private static final long COMPRESS_THRESHOLD = 500 * 1024; // 500KB
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1920;
    private static final float JPEG_QUALITY = 0.75f;

    @PostConstruct
    public void init() throws IOException {
        Path dir = Path.of(uploadPath).toAbsolutePath();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(extension)) {
            throw new BusinessException("不支持的文件类型: " + extension);
        }

        String filename = UUID.randomUUID().toString() + "." + extension;
        Path filePath = Path.of(uploadPath).toAbsolutePath().resolve(filename);

        // Image compression for large images
        if (isCompressibleImage(extension) && file.getSize() > COMPRESS_THRESHOLD) {
            byte[] compressed = compressImage(file);
            Files.write(filePath, compressed);
        } else {
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return "/uploads/" + filename;
    }

    private boolean isCompressibleImage(String ext) {
        return Arrays.asList("jpg", "jpeg", "png").contains(ext);
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            // Not a valid image, save as-is
            try (InputStream in = file.getInputStream()) {
                return in.readAllBytes();
            }
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Scale down if too large
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            double ratio = Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height);
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);
            Image scaled = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(scaled, 0, 0, null);
            g.dispose();
            originalImage = resized;
        }

        // Compress as JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "jpg", baos);
        byte[] compressed = baos.toByteArray();

        System.out.println("Compressed image: " + file.getSize() + " -> " + compressed.length + " bytes");
        return compressed;
    }
}
