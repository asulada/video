package com.asuala.mock.config;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.asuala.mock.file.monitor.linux.InotifyLibraryUtil;
import com.asuala.mock.file.monitor.win.utils.MonitorFileUtil;
import com.asuala.mock.file.monitor.win.vo.FileTreeNode;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.service.WatchFileService;
import com.asuala.mock.service.FileInfoService;
import com.asuala.mock.service.IndexService;
import com.asuala.mock.task.CommonTask;
import com.asuala.mock.utils.CPUUtils;
import com.asuala.mock.utils.MD5Utils;
import com.asuala.mock.vo.FileInfo;
import com.asuala.mock.vo.Index;
import com.asuala.mock.vo.req.RebuildReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @description:
 * @create: 2020/07/16
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationRunnerConfig implements ApplicationRunner {

    private final IndexService indexService;

    private final FileInfoMapper fileInfoMapper;
    private final FileInfoService fileInfoService;
    private final WatchFileService clientService;
    @Autowired(required = false)
    private CommonTask commonTask;


    @Value("${watch.rebuldFlag:false}")
    private boolean rebuldFlag;
    @Value("${down.client}")
    private boolean client;
    @Value("${down.server}")
    private boolean server;
    @Value("${down.http.salt}")
    private String salt;
    @Value("${down.http.rebuildUrl}")
    private String rebuildUrl;
    @Value("${watch.deleteLimit:1000000}")
    private int deleteLimit;
    @Value("${watch.open}")
    private boolean watchOpen;
    @Value("${watch.dir}")
    private Set<String> dirs;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Index index = CPUUtils.getCpuId();
        MainConstant.systemInfo = index;
        if (watchOpen) {
            addIndex(index);

            if (rebuldFlag) {
                //TODO-asuala 2024-02-02: 删除表数据
                Long count = fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getIndex, Constant.index));
                if (count > deleteLimit) {
                    fileInfoMapper.dropIndex(Constant.index);
                } else {
                    while (count > 0) {
                        count = fileInfoMapper.deleteLimit(Constant.index);
                    }
                }
                clientService.initFileInfo(index);

                if (index.getSystem().contains("LINUX")) {
                    CopyOnWriteArrayList<FileInfo>[][] array = InotifyLibraryUtil.rebuild(dirs);
                    //保存文件信息
                    for (CopyOnWriteArrayList<FileInfo>[] fileDirArray : array) {
                        for (CopyOnWriteArrayList<FileInfo> fileInfos : fileDirArray) {
                            List<List<FileInfo>> split = CollectionUtil.split(fileInfos, 5000);
                            for (List<FileInfo> infos : split) {
                                fileInfoMapper.batchInsert(infos);
                            }
                        }
                    }
                } else {
                    MonitorFileUtil.fileInfoServicel = fileInfoService;
                    for (String volumeNo : Constant.volumeNos) {
                        TreeMap<Long, FileTreeNode> map = MonitorFileUtil.buildFileInfo(volumeNo);
                        MonitorFileUtil.getFileInfo(map, volumeNo);
                    }
                }

                RebuildReq req = new RebuildReq();
                req.setIndex(Constant.index);
                req.setSign(MD5Utils.getSaltMD5(String.valueOf(Constant.index), salt));
                try {
                    HttpUtil.post(rebuildUrl, JSON.toJSONString(req));
                } catch (Exception e) {
                    log.error("发送重建数据请求失败id: {}", Constant.index, e);
                }
            } else {
                clientService.initFileInfo(index);
            }

        }

        if (client) {
            addIndex(index);
            commonTask.addTask();
        }
        if (server) {
            LocalDateTime now = LocalDateTime.now();
            List<Index> list = indexService.list(new LambdaQueryWrapper<Index>().ge(Index::getUpdateTime, now.minusMinutes(40).format(formatter)));
            if (list.size() > 0) {
                Constant.index = Math.toIntExact(list.get(0).getId());
                log.debug("客户端信息 {}", Constant.index);
            } else {
                log.error("没有取到存活的客户端信息 !!!");
            }
        }


        log.info("初始化结束 客户端号: {}", Constant.index);

    }

    private void addIndex(Index index) {
        try {
            Index one = indexService.findByCpuId(index.getCpuId());
            Date now = new Date();
            if (null == one) {
                index.setCreateTime(now);
                index.setUpdateTime(now);
                indexService.save(index);
                one = index;
            } else {
                one.setDelFlag(0);
                one.setUpdateTime(now);
                indexService.updateById(one);
            }
            Constant.index = Integer.parseInt(one.getId().toString());
        } catch (Exception e) {
            log.error("获取cpuid失败", e);
        }
    }


}