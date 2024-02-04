package com.asuala.mock.service;

import com.asuala.mock.file.monitor.FileChangeListener;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.FileInfoMapper;
import com.sun.jna.platform.FileMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "client", havingValue = "true")
public class ClientService {

    private final FileInfoMapper fileInfoMapper;

    @Value("${watch.dir}")
    private List<String> dirs;

    @Value("${down.http.addUrl}")
    private String addUrl;
    @Value("${down.http.delUrl}")
    private String delUrl;
    @Value("${down.http.salt}")
    private String salt;

    @PostConstruct
    public void initFileInfo() throws Exception {
        FileMonitor fileMonitor = FileMonitor.getInstance();

        for (String dir : dirs) {
            fileMonitor.addFileListener(new FileChangeListener(fileInfoMapper, dir, addUrl, delUrl, salt));
            File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
            fileMonitor.addWatch(file);

            Constant.volumeNos.add(dir.substring(0, dir.indexOf(":") + 1));
        }
    }
}