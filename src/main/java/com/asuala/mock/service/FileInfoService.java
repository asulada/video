package com.asuala.mock.service;

import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.vo.FileInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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


    public int updateBatch(List<FileInfo> list) {
        return baseMapper.updateBatch(list);
    }


    public int updateBatchSelective(List<FileInfo> list) {
        return baseMapper.updateBatchSelective(list);
    }


    public int batchInsert(List<FileInfo> list) {
        lock.lock();
        int i = 0;
        try {
            i = baseMapper.batchInsert(list);
        } finally {
            lock.unlock();
        }
        return i;
    }

}
