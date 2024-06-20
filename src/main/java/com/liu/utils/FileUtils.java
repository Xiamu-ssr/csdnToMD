package com.liu.utils;

import com.liu.parse.AbstractCrawler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUtils {
    //public static final String IMAGE_DIR = "images"+ File.separator;
    public static final String IMAGE_DIR = AbstractCrawler.imgDir;

    /**
     * 下载图片到本地
     *
     * @param imageSrc 图像src
     * @param mdName   md文件名称
     * @return {@link String}
     */
    public static String downloadImage(String imageSrc, String mdName){
        String fileExtension = ".png";
        String uniqueFileName = System.currentTimeMillis() + fileExtension;

        // 定义图片保存的路径
        Path outputPath = null;

        // 如果图片不分离存放，则在当前目录下创建一个名为"images"的目录
        // 否则，为每一个md文件创建同级同名文件夹，即在mdDir下
        if (!AbstractCrawler.imgIsolate){
            Path imagesDir = Paths.get(IMAGE_DIR);
            if (!Files.exists(imagesDir)) {
                try {
                    Files.createDirectories(imagesDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outputPath = imagesDir.resolve(uniqueFileName);
        }else {
            Path imagesDir = Paths.get(AbstractCrawler.mdDir + mdName);
            if (!Files.exists(imagesDir)) {
                try {
                    Files.createDirectories(imagesDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outputPath = imagesDir.resolve(uniqueFileName);
        }

        try {
            // 打开网络连接
            URL url = new URL(imageSrc);
            // 使用NIO将数据从网络复制到文件，同时为文件生成一个唯一的名称
            Files.copy(url.openStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Image downloaded successfully: " + outputPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //如果图片不分离，则返回相对路径
        //否则返回图片名
        if (!AbstractCrawler.imgIsolate){
            return "../"+IMAGE_DIR + uniqueFileName;
        }else {
            return uniqueFileName;
        }
    }
}
