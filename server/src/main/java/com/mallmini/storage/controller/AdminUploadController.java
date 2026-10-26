package com.mallmini.storage.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.storage.service.StorageService;
import com.mallmini.storage.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    private final StorageService storageService;

    @PostMapping
    public ApiResponse<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(new UploadVO(storageService.store(file)));
    }
}
