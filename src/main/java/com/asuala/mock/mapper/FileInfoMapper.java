package com.asuala.mock.mapper;

import com.asuala.mock.vo.FileInfo;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface FileInfoMapper extends BaseMapper<FileInfo> {
    int deleteByPrimaryKey(Long id);

    int insertSelective(FileInfo record);

    FileInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FileInfo record);

    int updateByPrimaryKey(FileInfo record);

    int updateBatch(List<FileInfo> list);

    int updateBatchSelective(List<FileInfo> list);

    int batchInsert(@Param("list") List<FileInfo> list);

    List<String> findNameByIndex(@Param("index")Integer index);


}