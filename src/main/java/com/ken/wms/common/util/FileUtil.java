package com.ken.wms.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 文件操作工具类
 *
 * @author Ken
 * @since 2017/4/22.
 */
public class FileUtil {

    /**
     * 将 org.springframework.web.multipart.MultipartFile 类型的文件转换为 java.io.File 类型的文件
     *
     * @param multipartFile org.springframework.web.multipart.MultipartFile 类型的文件
     * @return 返回转换后的 java.io.File 类型的文件
     * @throws IOException IOException
     */
    public static File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File convertedFile = new File(multipartFile.getOriginalFilename());
        multipartFile.transferTo(convertedFile);
        return convertedFile;
    }
}
