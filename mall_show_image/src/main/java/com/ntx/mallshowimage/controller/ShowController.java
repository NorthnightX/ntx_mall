package com.ntx.mallshowimage.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.ntx.mallshowimage.config.SystemConstant.IMAGE_UPLOAD_DIR_FIND;


@RestController
@RequestMapping("/image")
public class ShowController {
    /**
     * 图片请求回显
     * @param httpServletRequest
     * @return
     * @throws IOException
     */
    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> showImage(HttpServletRequest httpServletRequest) throws IOException {
        StringBuffer requestURL = httpServletRequest.getRequestURL();
        // 获取文件名部分
        String fileName = extractFileNameFromURL(requestURL.toString());
        // 拼接本地文件路径
        String localFilePath = IMAGE_UPLOAD_DIR_FIND + fileName;
        // 读取文件内容
        File file = new File(localFilePath);
        InputStream inputStream = Files.newInputStream(file.toPath());

        // 设置响应头，告知浏览器文件类型
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG); // 根据实际文件类型设置 MediaType

        // 将文件内容作为 InputStreamResource 返回
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }

    /**
     * 获取文件名
     * @param url
     * @return
     */
    private String extractFileNameFromURL(String url) {
        // 在这里解析出文件名，这里假设文件名是 URL 中最后一个斜杠后的部分
        int lastIndex = url.lastIndexOf('/');
        return url.substring(lastIndex + 1);
    }
}
