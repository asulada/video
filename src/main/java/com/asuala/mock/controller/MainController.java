package com.asuala.mock.controller;

import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.service.RecordService;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.Record;
import com.asuala.mock.vo.UrlReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import static com.asuala.mock.m3u8.utils.Constant.FILESEPARATOR;

/**
 * @description:
 * @create: 2023/08/09
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
public class MainController {

    private final RecordService recordService;

    //down:
    //  directory: ""
    @Value("${down.directory:'d:\\app\\'}")
    private String downDir;

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
        req.setFileName(req.getFileName().trim());
        List<Record> list = recordService.list(new LambdaQueryWrapper<Record>().eq(Record::getName, req.getFileName()).eq(Record::getAuthor, req.getAuthor()));
        if (list.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("已有");
            for (Record record : list) {
                stringBuilder.append(" ").append(record.getQuality());
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
//        File file = new File(downDir + req.getAuthor() + Constant.FILESEPARATOR +"+ req.getFileName() + ".mp4");
//        if (file.exists()) {
//            res.put("msg", "文件已存在");
//            return res;
//        }
        if (recordService.count(new LambdaQueryWrapper<Record>().eq(Record::getState, 0).eq(Record::getName, req.getFileName()).eq(Record::getQuality, req.getQuality()).eq(Record::getAuthor, req.getAuthor())) > 0) {
            res.put("msg", "文件已存在下载列表");
            return res;
        }
        if (null == req.getIndex()) {
            log.debug("没有指定分片客户端 默认: {} 文件名: {}", CacheUtils.index, req.getFileName());
            req.setIndex(CacheUtils.index);
        }
        Record result = recordService.getLastSameFile(req);
        if (null != result) {
            result.setUrl(req.getUrl());
            result.setUpdateTime(new Date());
            result.setDelFlag(0);
            result.setPageUrl(req.getPageUrl());
            result.setIndex(req.getIndex());
            recordService.updateByPrimaryKeySelective(result);
        } else {
            //        down(req.getFileName(),req.getUrl());
            result = new Record();
            result.setName(req.getFileName());
            result.setUrl(req.getUrl());
            result.setCreateTime(new Date());
            result.setQuality(req.getQuality());
            result.setState(0);
            result.setAuthor(req.getAuthor());
            result.setPageUrl(req.getPageUrl());
            result.setIndex(req.getIndex());
            recordService.save(result);
        }

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
        if (recordService.count(new LambdaQueryWrapper<Record>().in(Record::getState, 2, 3).eq(Record::getName, req.getFileName()).eq(Record::getAuthor, req.getAuthor())) > 0) {
            res.put("msg", "文件已存在下载列表");
            return res;
        }
        if (null == req.getIndex()) {
            log.debug("没有指定分片客户端 默认: {} 文件名: {}", CacheUtils.index, req.getFileName());
            req.setIndex(CacheUtils.index);
        }

        Record result = new Record();
        result.setName(req.getFileName());
        result.setCreateTime(new Date());
        result.setState(2);
        result.setAuthor(req.getAuthor());
        result.setQuality("txt");
        result.setIndex(req.getIndex());
        recordService.save(result);

        res.put("code", 200);
        return res;
    }


    private String getFilName(String fileName) {
        File file = new File(downDir, fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file.getAbsolutePath();
    }

}