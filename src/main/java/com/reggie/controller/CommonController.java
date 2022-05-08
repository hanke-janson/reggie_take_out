package com.reggie.controller;

import com.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {
    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     *
     * @param file 此处形参名需要和前端请求的参数name的值保持一致
     * @return 文件名, 用于前端显示
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        // 当前file是一个临时文件,需要转存到指定位置, 否则本次请求完成后, 临时文件就会删除
        log.info(file.toString());
        // 获取文件原始文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 使用UUID重新生成, 防止文件名称重复, 造成文件覆盖
        String fileName = UUID.randomUUID() + suffix;
        // 创建一个目录对象
        File dir = new File(basePath);
        if (!dir.isFile()) {
            // 目录不存在
            dir.mkdirs();
        }
        // 判断当前目录是否存在
        try {
            //将临时文件转存到指定位置
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.success(fileName);
    }

    /**
     * 文件下载, 浏览器显示图片
     *
     * @param name     图片文件名
     * @param response 用于获取输出流, 将图片文件写回浏览器
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        // 通过输入流读取文件内容
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));
            // 通过输出流将文件写回浏览器, 在浏览器上显示图片
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("image/jpeg");
            byte[] bytes = new byte[1024];
            int len;
            while ((len = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }
            outputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
