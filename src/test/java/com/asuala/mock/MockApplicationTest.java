package com.asuala.mock;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.utils.MD5Utils;
import com.asuala.mock.vo.req.RebuildReq;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

/**
 * @description:
 * @create: 2023/12/27
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MockApplication.class)
@Slf4j
public class MockApplicationTest {

    @Test
    public void init() {

    }

}