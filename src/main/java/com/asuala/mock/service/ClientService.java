package com.asuala.mock.service;

import cn.hutool.core.collection.ListUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.asuala.mock.es.Es8Client;
import com.asuala.mock.es.entity.FileInfoEs;
import com.asuala.mock.file.monitor.FileChangeListener;
import com.asuala.mock.file.monitor.utils.MonitorFileUtil;
import com.asuala.mock.file.monitor.vo.FileTreeNode;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.FileInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.jna.platform.FileMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "downRole", havingValue = "false")
public class ClientService {

    private final FileInfoMapper fileInfoMapper;

    @Value("${down.directory:'d:\\app\\'}")
    private String downDir;

    @Value("${down.http.addUrl}")
    private String addUrl;
    @Value("${down.http.delUrl}")
    private String delUrl;
    @Value("${down.http.salt}")
    private String salt;

    @PostConstruct
    public void initFileInfo() throws Exception {
        FileMonitor fileMonitor = FileMonitor.getInstance();
        fileMonitor.addFileListener(new FileChangeListener(fileInfoMapper, addUrl, delUrl, salt));
        File file = new File(downDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        fileMonitor.addWatch(file);

    }
}