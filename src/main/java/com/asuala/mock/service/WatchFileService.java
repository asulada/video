package com.asuala.mock.service;

import com.asuala.mock.file.monitor.linux.InotifyLibraryUtil;
import com.asuala.mock.file.monitor.win.FileChangeListener;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.UPathMapper;
import com.asuala.mock.utils.FileIdUtils;
import com.asuala.mock.vo.Index;
import com.asuala.mock.vo.UPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sun.jna.platform.FileMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "watch", name = "open", havingValue = "true")
public class WatchFileService {

//    private final FileInfoService fileInfoService;
//    @Value("${watch.dir}")
//    private Set<String> dirs;



}