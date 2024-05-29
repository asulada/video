package com.asuala.mock.file.monitor.linux;

import com.asuala.mock.service.FileInfoService;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.FileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @description:
 * @create: 2024/05/20
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "watch", name = "open", havingValue = "true")
public class FileListener {

    private final FileInfoService fileInfoService;

    public static ExecutorService fixedThreadPool;
    //.swp .swx
    private static final List<String> exlude = new ArrayList<String>() {{
        add(".swp");
        add(".swx");
    }};


    @PostConstruct
    public void consumer() {
        fixedThreadPool = Executors.newFixedThreadPool(1);
        fixedThreadPool.execute(() -> {
            while (CacheUtils.watchFlag) {
                FileVo poll = (FileVo) CacheUtils.queue.poll();
                try {
                    if (null == poll) {
                        Thread.sleep(1000L);
                    } else {
                        int mask = poll.getCode();
                        boolean isDir = poll.isDir();

                        if (!isDir) {
                            if (poll.getName().endsWith("~")) {
                                continue;
                            }
                            if (poll.getName().contains(".")) {
                                String suffix = poll.getName().substring(poll.getName().lastIndexOf("."), poll.getName().length());
                                if (exlude.contains(suffix)) {
                                    continue;
                                }
                            } else if ("4913".equals(poll.getName())) {
                                continue;
                            }
                        }

                        FileInfo fileInfo;
                        switch (mask) {
                            case Constant.IN_CREATE:
                            case Constant.IN_MOVED_TO:
                                fileInfoService.insert(new File(poll.getFullPath()), poll.getSId());
                                break;
                            case Constant.IN_MODIFY:
                                if (!isDir) {
                                    fileInfo = fileInfoService.findFileInfo(poll.getName(), poll.getPath());
                                    if (null == fileInfo) {
                                        log.warn("{} 修改文件事件-没有文件", poll.getFullPath());
                                        fileInfoService.insert(new File(poll.getFullPath()), poll.getSId());
                                    } else {
                                        File file = new File(poll.getFullPath());
                                        fileInfo.setSize(file.length());
                                        fileInfo.setCreateTime(new Date(file.lastModified()));
                                        fileInfo.setUpdateTime(new Date());
                                        fileInfoService.updateById(fileInfo);
                                        fileInfoService.saveEs(fileInfo);
                                    }
                                }
                                break;
                            case Constant.IN_MOVED_FROM:
                            case Constant.IN_DELETE:
                            case Constant.IN_DELETE_SELF:
                                fileInfo = fileInfoService.findFileInfo(new File(poll.getFullPath()));
                                if (null != fileInfo) {
                                    fileInfoService.deleteByPrimaryKey(fileInfo.getId());
                                    fileInfoService.delEs(fileInfo);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    log.error("监听文件出错 {}", poll.getFullPath(), e);
                }

            }
        });

    }
}