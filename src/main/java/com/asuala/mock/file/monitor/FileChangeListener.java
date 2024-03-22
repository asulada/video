package com.asuala.mock.file.monitor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.asuala.mock.file.monitor.enums.FileChangeEventEnum;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.FileInfoMapper;
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

    private FileInfoMapper fileInfoMapper;

    private String dir;
    private String addUrl;
    private String delUrl;
    private String salt;

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
                insert(file);
                break;
            case FILE_MODIFIED:
                if (file.isFile()) {
                    fileInfo = findFileInfo(file);
                    if (null == fileInfo) {
                        log.warn("{} 修改文件事件-没有文件", file.getName());
                        insert(file);
                    } else {
                        fileInfo.setSize(file.length());
                        fileInfo.setCreateTime(new Date(file.lastModified()));
                        fileInfo.setUpdateTime(new Date());
                        fileInfoMapper.updateById(fileInfo);
                        saveEs(fileInfo);
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
                    fileInfoMapper.updateById(fileInfo);
                    saveEs(fileInfo);
                } else {
                    log.warn("{} 新名称事件-没有旧文件", file.getName());
                    insert(file);
                }
                break;
            case FILE_NAME_CHANGED_OLD:
                fileInfo = findFileInfo(file);
                if (null != fileInfo) {
                    tmpFile = fileInfo;
                } else {
                    log.warn("{} 旧名称事件-没有找到此文件", file.getName());
                }
                break;
            case FILE_DELETED:
                fileInfo = findFileInfo(file);
                if (null != fileInfo) {
                    fileInfoMapper.deleteByPrimaryKey(fileInfo.getId());
                    delEs(fileInfo);
                }
                break;
            default:
                break;

        }
    }

    private FileInfo findFileInfo(File file) {
        List<FileInfo> list = fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getName, file.getName()).eq(FileInfo::getIndex, Constant.index));
        FileInfo fileInfo = null;
        for (FileInfo info : list) {
            if (info.getPath().equals(file.getAbsolutePath())) {
                fileInfo = info;
                break;
            }
        }
        return fileInfo;
    }

    private void insert(File file) {
        FileInfo.FileInfoBuilder builder = FileInfo.builder().name(file.getName()).path(file.getAbsolutePath()).createTime(new Date()).index(Constant.index).changeTime(new Date(file.lastModified()));
        if (file.isFile()) {
            String suffix = FileUtil.getSuffix(file);
            if (suffix.length() > 20) {
                log.warn("文件后缀名过长: {}", file.getAbsolutePath());
                return;
            }
            builder.size(file.length()).suffix(suffix).dir(0);
        } else {
            builder.dir(1);
        }
        FileInfo fileInfo = builder.build();
        fileInfoMapper.insert(fileInfo);
        saveEs(fileInfo);

    }

    private void saveEs(FileInfo fileInfo) {
        FileInfoReq req = new FileInfoReq();
        req.setId(fileInfo.getId());
        req.setName(fileInfo.getName());
        req.setPath(fileInfo.getPath());
        req.setSuffix(fileInfo.getSuffix());
        req.setSize(fileInfo.getSize());
        req.setChangeTime(fileInfo.getChangeTime());
        req.setIndex(fileInfo.getIndex());
        req.setSign(MD5Utils.getSaltMD5(fileInfo.getName(), salt));
        try {
            HttpUtil.post(addUrl, JSON.toJSONString(req));
        } catch (Exception e) {
            log.error("发送es请求失败 文件id: {}", fileInfo.getId(), e);
        }
    }

    private void delEs(FileInfo fileInfo) {
        FileInfoReq req = new FileInfoReq();
        req.setId(fileInfo.getId());
        req.setSign(MD5Utils.getSaltMD5(fileInfo.getName(), salt));
        req.setName(fileInfo.getName());
        try {
            HttpUtil.post(delUrl, JSON.toJSONString(req));
        } catch (Exception e) {
            log.error("发送es请求失败 文件id: {}", fileInfo.getId(), e);
        }
    }
}