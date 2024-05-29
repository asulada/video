package com.asuala.mock.mapper;

import com.asuala.mock.vo.ChannelDetails;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface ChannelDetailsMapper extends BaseMapper<ChannelDetails> {
    int deleteByPrimaryKey(Long id);

    int insertSelective(ChannelDetails record);

    ChannelDetails selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ChannelDetails record);

    int updateByPrimaryKey(ChannelDetails record);

    int updateBatch(List<ChannelDetails> list);

    int updateBatchSelective(List<ChannelDetails> list);

    int batchInsert(@Param("list") List<ChannelDetails> list);
}