package com.ntx.malluploadimage.controller;

import cn.hutool.core.util.StrUtil;
import com.ntx.mallcommon.domain.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.ntx.malluploadimage.common.SystemContent.IMAGE_UPLOAD_DIR;
import static com.ntx.malluploadimage.common.SystemContent.IMAGE_UPLOAD_DIR_BLOG;


@RestController
@RequestMapping("/upload")
public class UploadController {

    /**
     * 上传文件
     * @param image
     * @return
     */
    @PostMapping("/uploadImage")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 生成新文件名
            String fileName = createNewFileName(originalFilename);
            // 保存文件
            image.transferTo(new File(IMAGE_UPLOAD_DIR, fileName));
            // 返回结果
            return Result.success("http://localhost:10100/image" + fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 上传文件
     * @param image
     * @return
     */
    @PostMapping("/uploadMallImage")
    public Result uploadBlogImage(@RequestBody MultipartFile image) {
        try {
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 生成新文件名
            String fileName = createNewFileName(originalFilename);
            // 保存文件
            image.transferTo(new File(IMAGE_UPLOAD_DIR, fileName));
            // 返回结果
            return Result.success("http://localhost:10100/image" + fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 创建文件
     * @param originalFilename
     * @return
     */
    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        // 判断目录是否存在
        File dir = new File(IMAGE_UPLOAD_DIR, StrUtil.format(IMAGE_UPLOAD_DIR_BLOG));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("/" + IMAGE_UPLOAD_DIR_BLOG +"/{}.{}", name, suffix);
    }
}
