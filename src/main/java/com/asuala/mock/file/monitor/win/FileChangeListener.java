package com.asuala.mock.file.monitor.win;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.asuala.mock.file.monitor.win.enums.FileChangeEventEnum;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.service.FileInfoService;
import com.asuala.mock.utils.MD5Utils;
import com.asuala.mock.vo.FileInfo;
import com.asuala.mock.vo.req.FileInfoReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sun.jna.platform.FileMonitor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Slf4j
@AllArgsConstructor
public class FileChangeListener implements FileMonitor.FileListener {

    private FileInfoService fileInfoService;

    private String dir;

    private static FileInfo tmpFile;

    @Override
    public void fileChanged(FileMonitor.FileEvent e) {
        if (!e.getFile().getAbsolutePath().startsWith(dir)) {
            return;
        }
        FileChangeEventEnum eventEnum = FileChangeEventEnum.convert(e.getType());
        File file = e.getFile();
        FileInfo fileInfo = null;
        switch (eventEnum) {
            case FILE_CREATED:
                fileInfoService.insert(file);
                break;
            case FILE_MODIFIED:
                if (file.isFile()) {
                    fileInfo = fileInfoService.findFileInfo(file);
                    if (null == fileInfo) {
                        log.warn("{} 修改文件事件-没有文件", file.getName());
                        fileInfoService.insert(file);
                    } else {
                        fileInfo.setSize(file.length());
                        fileInfo.setCreateTime(new Date(file.lastModified()));
                        fileInfo.setUpdateTime(new Date());
                        fileInfoService.updateById(fileInfo);
                        fileInfoService.saveEs(fileInfo);
                    }
                }
//                    else {
//                        fileInfo = FileInfo.builder().id(fileInfo.getId()).changeTime(new Date(file.lastModified())).updateTime(new Date()).build();
//                    }

                break;
            case FILE_NAME_CHANGED_NEW:
                if (null != tmpFile) {
                    String suffix = FileUtil.getSuffix(file);
                    if (suffix.length() > 0) {
                        log.warn("文件后缀名过长: {}", file.getAbsolutePath());
                        break;
                    }
                    fileInfo.setSuffix(suffix);
                    fileInfo.setName(file.getName());
                    fileInfo.setPath(file.getAbsolutePath());
                    fileInfo.setChangeTime(new Date(file.lastModified()));
                    fileInfo.setUpdateTime(new Date());
                    fileInfoService.updateById(fileInfo);
                    fileInfoService.saveEs(fileInfo);
                } else {
                    log.warn("{} 新名称事件-没有旧文件", file.getName());
                    fileInfoService.insert(file);
                }
                break;
            case FILE_NAME_CHANGED_OLD:
                fileInfo = fileInfoService.findFileInfo(file);
                if (null != fileInfo) {
                    tmpFile = fileInfo;
                } else {
                    log.warn("{} 旧名称事件-没有找到此文件", file.getName());
                }
                break;
            case FILE_DELETED:
                fileInfo = fileInfoService.findFileInfo(file);
                if (null != fileInfo) {
                    fileInfoService.deleteByPrimaryKey(fileInfo.getId());
                    fileInfoService.delEs(fileInfo);
                }
                break;
            default:
                break;

        }
    }


}