package com.asuala.mock.spider.controller;

import com.asuala.mock.enums.state.ChannelEnum;
import com.asuala.mock.service.ChannelService;
import com.asuala.mock.vo.Channel;
import com.asuala.mock.vo.req.UrlReq;
import com.asuala.mock.vo.res.BaseResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.Date;

/**
 * @description:
 * @create: 2024/02/06
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("spider")
public class SpiderController {

    private final ChannelService channelService;

    @PostMapping("put")
    public BaseResponse put(@RequestBody UrlReq req) {
        if (StringUtils.isBlank(req.getUrl())) {
            return BaseResponse.err("url");
        }

        String name = getPath(req.getUrl());
//        if (!name.endsWith("/videos")) {
//            return BaseResponse.err("下载页面错误: ", name);
//        }
        if (channelService.count(new LambdaQueryWrapper<Channel>().eq(Channel::getName, name)) > 0) {
            return BaseResponse.err("重复: ", name);
        }
        channelService.save(Channel.builder().state(ChannelEnum.UNTREATED.getCode()).name(name).url(req.getUrl()).createTime(new Date()).build());
        return BaseResponse.ok();
    }


    public static String getPath(String url) {
        String path = "";
        try {
            URL urlObj = new URL(url);
            path = urlObj.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }
}