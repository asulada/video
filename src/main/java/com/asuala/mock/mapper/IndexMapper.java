package com.asuala.mock.mapper;
import java.util.Date;

import com.asuala.mock.vo.Index;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface IndexMapper extends BaseMapper<Index> {
    int deleteByPrimaryKey(Long id);

    int insertSelective(Index record);

    Index selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Index record);

    int updateByPrimaryKey(Index record);

    int updateBatch(List<Index> list);

    int updateBatchSelective(List<Index> list);

    int batchInsert(@Param("list") List<Index> list);

    Index findByCpuId(@Param("cpuId") String cpuId);


    int updateUpdateTimeById(@Param("updatedUpdateTime")Date updatedUpdateTime,@Param("id")Long id);


}