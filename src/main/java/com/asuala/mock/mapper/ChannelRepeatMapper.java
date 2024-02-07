package com.asuala.mock.mapper;

import com.asuala.mock.vo.ChannelRepeat;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface ChannelRepeatMapper extends BaseMapper<ChannelRepeat> {
    int deleteByPrimaryKey(Long id);

    int insertSelective(ChannelRepeat record);

    ChannelRepeat selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ChannelRepeat record);

    int updateByPrimaryKey(ChannelRepeat record);

    int updateBatch(List<ChannelRepeat> list);

    int updateBatchSelective(List<ChannelRepeat> list);

    int batchInsert(@Param("list") List<ChannelRepeat> list);
}