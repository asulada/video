package com.asuala.mock.spider.controller;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.enums.state.RecordEnum;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.UserMapper;
import com.asuala.mock.service.RecordService;
import com.asuala.mock.utils.TimeUtils;
import com.asuala.mock.vo.Record;
import com.asuala.mock.vo.User;
import com.asuala.mock.vo.req.UrlReq;
import com.asuala.mock.vo.req.UserReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @create: 2024/05/31
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
public class DownloadController {

    private final RecordService recordService;
    private final UserMapper userMapper;

    @Value("${search.urlOptions}")
    private String urlOptions;

    @RequestMapping("login")
    @ResponseBody
    public SaResult doLogin(@RequestBody UserReq user) {
        User userVo = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getName, user.getName()));
        // 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对
        if (!BCrypt.checkpw(user.getPassword(), userVo.getPasswd())) {
            return SaResult.error("账号或密码错误");
        }
        log.info("{} 登录", user.getName());
        StpUtil.login(userVo.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return SaResult.data(tokenInfo);
    }

    @PostMapping("pass")
    @ResponseBody
    public SaResult pass(@RequestBody Map<String, String> map) {
        String pass = map.get("pass");
        String pw_hash = BCrypt.hashpw(pass, BCrypt.gensalt(10));
        log.info("pass {}", pw_hash);
        return SaResult.ok();
    }

    @PostMapping("analysis")
    public JSONObject analysis(@RequestBody UrlReq req) {
        JSONObject res = new JSONObject();
        res.put("code", 222);
        if (StringUtils.isBlank(req.getUrl())) {
            res.put("msg", "解析地址为空");
        } else {
            try {
                String cmd = "you-get -x 127.0.0.1:7890 -i " + req.getUrl();
                // 执行命令
                Process process = Runtime.getRuntime().exec(cmd);

                // 读取命令执行结果
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                log.info("打印命令执行结果 {}", output.toString());
                int exitVal = process.waitFor();
                log.info("获取命令执行返回值 {}", exitVal);
            } catch (IOException | InterruptedException e) {
                log.error("解析地址失败", e);
                res.put("msg", "解析地址失败");
            }
        }
        return res;
    }

    @PostMapping("repeat")
    public JSONObject repeat(@RequestBody UrlReq req) {
        JSONObject res = new JSONObject();
        res.put("code", 222);
        if (StringUtils.isBlank(req.getFileName())) {
            res.put("msg", "名称为空");
            return res;
        }
        if (StringUtils.isBlank(req.getAuthor())) {
            res.put("msg", "作者为空");
            return res;
        }
        req.setFileName(req.getFileName().trim());

        List<String> list = recordService.findQualityByAuthorAndName(req.getAuthor(), req.getFileName());
        if (list.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("已有");
            for (String record : list) {
                stringBuilder.append(" ").append(record);
            }
            stringBuilder.append(" 任务");
            res.put("msg", stringBuilder.toString());
            return res;
        }
        res.put("code", 200);
        return res;
    }


    @PostMapping("down")
    public JSONObject down(@RequestBody UrlReq req) {
        JSONObject res = new JSONObject();
        res.put("code", 222);
        if (StringUtils.isBlank(req.getUrl())) {
            res.put("msg", "地址为空");
            return res;
        }
        if (StringUtils.isBlank(req.getFileName())) {
            res.put("msg", "名称为空");
            return res;
        }
        req.setFileName(req.getFileName().trim());
        if (StringUtils.isBlank(req.getQuality())) {
            res.put("msg", "分辨率为空");
            return res;
        }
        if (StringUtils.isBlank(req.getPageUrl())) {
            res.put("msg", "页面地址为空");
            return res;
        }
        if (StringUtils.isBlank(req.getAuthor())) {
            res.put("msg", "作者为空");
            return res;
        }
        if (StringUtils.isBlank(req.getPicUrl())) {
            res.put("msg", "图片为空");
            return res;
        }
//        File file = new File(downDir + req.getAuthor() + MainConstant.FILESEPARATOR +"+ req.getFileName() + ".mp4");
//        if (file.exists()) {
//            res.put("msg", "文件已存在");
//            return res;
//        }
        if (recordService.count(new LambdaQueryWrapper<Record>().eq(Record::getState, RecordEnum.UNTREATED.getCode()).eq(Record::getName, req.getFileName()).eq(Record::getQuality, req.getQuality()).eq(Record::getAuthor, req.getAuthor())) > 0) {
            res.put("msg", "文件已存在下载列表");
            return res;
        }
        if (null == req.getIndex()) {
            log.debug("没有指定分片客户端 默认: {} 文件名: {}", Constant.index, req.getFileName());
            req.setIndex(Constant.index);
        }
//        Record result = recordService.getLastSameFile(req);
//        if (null != result) {
//            result.setUrl(req.getUrl());
//            result.setUpdateTime(new Date());
//            result.setDelFlag(0);
//            result.setPageUrl(req.getPageUrl());
//            result.setIndex(req.getIndex());
//            result.setPicUrl(req.getPicUrl());
//            recordService.updateByPrimaryKeySelective(result);
//        } else {
        //        down(req.getFileName(),req.getUrl());
        Record result = new Record();
        result.setName(req.getFileName());
        result.setUrl(req.getUrl());
        result.setCreateTime(new Date());
        result.setQuality(req.getQuality());
        result.setState(RecordEnum.UNTREATED.getCode());
        result.setAuthor(req.getAuthor());
        result.setPageUrl(req.getPageUrl());
        result.setIndex(req.getIndex());
        result.setPicUrl(req.getPicUrl());
        result.setTimeHum(TimeUtils.convertSecondsToHMS(req.getDuration()));
        recordService.save(result);
//        }

        res.put("code", 200);
        return res;
    }


    @PostMapping("downFbd")
    public JSONObject downFbd(@RequestBody UrlReq req) throws IOException {
        JSONObject res = new JSONObject();
        res.put("code", 222);
        if (StringUtils.isBlank(req.getFileName())) {
            res.put("msg", "名称为空");
            return res;
        }
        req.setFileName(req.getFileName().trim());
        if (StringUtils.isBlank(req.getAuthor())) {
            res.put("msg", "作者为空");
            return res;
        }
        if (recordService.count(new LambdaQueryWrapper<Record>().in(Record::getState, RecordEnum.TXT.getCode(), RecordEnum.TXT_DOWNLOADED.getCode()).eq(Record::getName, req.getFileName()).eq(Record::getAuthor, req.getAuthor())) > 0) {
            res.put("msg", "文件已存在下载列表");
            return res;
        }
        if (null == req.getIndex()) {
            log.debug("没有指定分片客户端 默认: {} 文件名: {}", Constant.index, req.getFileName());
            req.setIndex(Constant.index);
        }

        Record result = new Record();
        result.setName(req.getFileName());
        result.setCreateTime(new Date());
        result.setState(RecordEnum.TXT.getCode());
        result.setAuthor(req.getAuthor());
        result.setQuality("txt");
        result.setIndex(req.getIndex());
        recordService.save(result);

        res.put("code", 200);
        return res;
    }

    @GetMapping("openUrl")
    @ResponseBody
    public JSONObject openUrl() {
        return JSONObject.parseObject(urlOptions);
    }

}