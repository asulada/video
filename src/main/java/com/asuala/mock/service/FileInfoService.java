package com.asuala.mock.service;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.vo.FileInfo;
@Service
public class FileInfoService{

    @Resource
    private FileInfoMapper fileInfoMapper;

    
    public int deleteByPrimaryKey(Long id) {
        return fileInfoMapper.deleteByPrimaryKey(id);
    }

    
    public int insertSelective(FileInfo record) {
        return fileInfoMapper.insertSelective(record);
    }

    
    public FileInfo selectByPrimaryKey(Long id) {
        return fileInfoMapper.selectByPrimaryKey(id);
    }

    
    public int updateByPrimaryKeySelective(FileInfo record) {
        return fileInfoMapper.updateByPrimaryKeySelective(record);
    }

    
    public int updateByPrimaryKey(FileInfo record) {
        return fileInfoMapper.updateByPrimaryKey(record);
    }

    
    public int updateBatch(List<FileInfo> list) {
        return fileInfoMapper.updateBatch(list);
    }

    
    public int updateBatchSelective(List<FileInfo> list) {
        return fileInfoMapper.updateBatchSelective(list);
    }

    
    public int batchInsert(List<FileInfo> list) {
        return fileInfoMapper.batchInsert(list);
    }

}
