package com.asuala.mock.mapper;

import com.asuala.mock.vo.Record;
import com.asuala.mock.vo.req.UrlReq;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecordMapper extends BaseMapper<Record> {
    int deleteByPrimaryKey(Long id);

    int insertSelective(Record record);

    Record selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Record record);

    int updateByPrimaryKey(Record record);

    int updateBatch(List<Record> list);

    int updateBatchSelective(List<Record> list);

    int batchInsert(@Param("list") List<Record> list);

    void updateDelAndTime(Record record);

    Record findLastSameFile(UrlReq req);

    List<Record> pagePageUrl(Page<Record> page, @Param("index") int index);

}