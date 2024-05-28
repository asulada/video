package com.asuala.mock.service;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import com.asuala.mock.vo.UPath;
import com.asuala.mock.mapper.UPathMapper;
@Service
public class UPathService{

    @Autowired
    private UPathMapper uPathMapper;

    
    public int deleteByPrimaryKey(Long id) {
        return uPathMapper.deleteByPrimaryKey(id);
    }

    
    public int insertSelective(UPath record) {
        return uPathMapper.insertSelective(record);
    }

    
    public UPath selectByPrimaryKey(Long id) {
        return uPathMapper.selectByPrimaryKey(id);
    }

    
    public int updateByPrimaryKeySelective(UPath record) {
        return uPathMapper.updateByPrimaryKeySelective(record);
    }

    
    public int updateByPrimaryKey(UPath record) {
        return uPathMapper.updateByPrimaryKey(record);
    }

    
    public int batchInsert(List<UPath> list) {
        return uPathMapper.batchInsert(list);
    }

}
