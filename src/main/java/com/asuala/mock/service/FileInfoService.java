package com.asuala.mock.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.utils.FileUtils;
import com.asuala.mock.utils.MD5Utils;
import com.asuala.mock.vo.FileInfo;
import com.asuala.mock.vo.req.FileInfoReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class FileInfoService extends ServiceImpl<FileInfoMapper, FileInfo> {

    private static final Lock lock = new ReentrantLock();

    public int deleteByPrimaryKey(Long id) {
        return baseMapper.deleteByPrimaryKey(id);
    }


    public int insertSelective(FileInfo record) {
        return baseMapper.insertSelective(record);
    }


    public FileInfo selectByPrimaryKey(Long id) {
        return baseMapper.selectByPrimaryKey(id);
    }


    public int updateByPrimaryKeySelective(FileInfo record) {
        return baseMapper.updateByPrimaryKeySelective(record);
    }


    public int updateByPrimaryKey(FileInfo record) {
        return baseMapper.updateByPrimaryKey(record);
    }

}
