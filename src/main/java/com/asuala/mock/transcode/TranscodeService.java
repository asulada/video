package com.asuala.mock.transcode;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @description:
 * @create: 2024/03/18
 **/
@Slf4j
@Service
public class TranscodeService {

    @Value("${transcode.pyPath}")
    private String pyPath;

    private static boolean flag = true;


    public void ranscodeVideo(File file) throws IOException {
        log.info("{} 开始转码", file.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder("python.exe", pyPath, file.getAbsolutePath());
//        Map<String, String> environment = processBuilder.environment();
        Process process = processBuilder.start();

        BufferedReader infoReader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("gbk")));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.forName("gbk")));

        flag = true;
        Thread infoThread = new Thread(() -> {
            try {
                String line;
                while (flag) {
                    while ((line = infoReader.readLine()) != null) {
                        log.debug(line);
                    }
                    Thread.sleep(1000L);
                }
            } catch (Exception e) {
                if (!"Stream closed".equals(e.getMessage())) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        Thread errorThread = new Thread(() -> {
            try {

                String line;
                while (flag) {
                    while ((line = errorReader.readLine()) != null) {
                        log.error(line);
                    }
                    Thread.sleep(1000L);
                }
            } catch (Exception e) {
                if (!"Stream closed".equals(e.getMessage())) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        infoThread.start();
        errorThread.start();
        int exitCode = -1;
        try {
            exitCode = process.waitFor();
            flag = false;
            infoThread.join();
            errorThread.join();
        } catch (InterruptedException e) {
            log.error("{} 执行失败", file.getAbsolutePath(), e);
        } finally {
            if (exitCode == 0) {
                log.info("{} 转码成功", file.getAbsolutePath());
            } else {
                log.error("{} 转码失败！退出码为:{}", file.getAbsolutePath(), exitCode);
            }
        }
    }
}