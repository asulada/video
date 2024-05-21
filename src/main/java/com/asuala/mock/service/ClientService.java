package com.asuala.mock.service;

import com.asuala.mock.file.monitor.linux.InotifyLibraryUtil;
import com.asuala.mock.file.monitor.win.FileChangeListener;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.utils.CPUUtils;
import com.asuala.mock.vo.Index;
import com.sun.jna.platform.FileMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "client", havingValue = "true")
public class ClientService {

    private final FileInfoService fileInfoService;

    @Value("${watch.dir}")
    private Set<String> dirs;


    public void initFileInfo(Index cpuId) throws Exception {
        if (cpuId.getSystem().contains("LINUX")){
            InotifyLibraryUtil.init(dirs);
        }else {
            FileMonitor fileMonitor = FileMonitor.getInstance();
            for (String dir : dirs) {
                fileMonitor.addFileListener(new FileChangeListener(fileInfoService, dir));
                File file = new File(dir);
                if (!file.exists()) {
                    file.mkdirs();
                }
                fileMonitor.addWatch(file);

                Constant.volumeNos.add(dir.substring(0, dir.indexOf(":") + 1));
            }
        }

    }
}