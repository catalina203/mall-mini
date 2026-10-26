package com.mallmini.storage.service;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private static final long MAX_SIZE = 2 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final AppProperties properties;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择图片文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅支持 jpg/png/webp 图片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片读取失败");
        }
        String extension = detectExtension(bytes);
        if (extension == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片内容格式不合法");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Path.of(properties.getStorage().getLocalDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            log.warn("failed to store upload: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filename)
                .toUriString();
    }

    private String detectExtension(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G') {
            return ".png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
