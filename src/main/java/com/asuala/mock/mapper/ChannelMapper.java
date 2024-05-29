package com.asuala.mock.mapper;

import com.asuala.mock.vo.Channel;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface ChannelMapper extends BaseMapper<Channel> {
    int deleteByPrimaryKey(Long id);

    int insertSelective(Channel record);

    Channel selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Channel record);

    int updateByPrimaryKey(Channel record);

    int updateBatch(List<Channel> list);

    int updateBatchSelective(List<Channel> list);

    int batchInsert(@Param("list") List<Channel> list);
}