package com.asuala.mock.service;

import com.asuala.mock.mapper.UserMapper;
import com.asuala.mock.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserService{

    @Autowired
    private UserMapper userMapper;

    
    public int deleteByPrimaryKey(Long id) {
        return userMapper.deleteByPrimaryKey(id);
    }

    
    public int insertSelective(User record) {
        return userMapper.insertSelective(record);
    }

    
    public User selectByPrimaryKey(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }

    
    public int updateByPrimaryKeySelective(User record) {
        return userMapper.updateByPrimaryKeySelective(record);
    }

    
    public int updateByPrimaryKey(User record) {
        return userMapper.updateByPrimaryKey(record);
    }

    
    public int batchInsert(List<User> list) {
        return userMapper.batchInsert(list);
    }

}
