package com.asuala.mock.service;

import com.asuala.mock.mapper.RecordMapper;
import com.asuala.mock.vo.Record;
import com.asuala.mock.vo.UrlReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @description:
 * @create: 2024/01/14
 **/
@Service
@Slf4j
public class RecordService extends ServiceImpl<RecordMapper, Record> {

    public void success(Long id) {
        Record record = new Record();
        record.setId(id);
        record.setState(1);
        record.setUpdateTime(new Date());

        baseMapper.updateById(record);
    }

    public void deleteRecord(Long id, String fileName) {
        log.error("删除id {}, {}", id, fileName);
        Record record = new Record();
        record.setId(id);
        record.setDelFlag(1);
        record.setUpdateTime(new Date());
        baseMapper.updateDelAndTime(record);
//        baseMapper.updateById(record);
    }

    public Record getLastSameFile(UrlReq req) {
        return baseMapper.findLastSameFile(req);
    }

    public void updateByPrimaryKeySelective(Record record){
        baseMapper.updateByPrimaryKeySelective(record);
    }

    public List<Record> pagePageUrl(Page<Record> page, int index) {
        return baseMapper.pagePageUrl(page,index);
    }

    public void updateBatchSelective(List<Record> list){
        baseMapper.updateBatchSelective(list);
    }
}