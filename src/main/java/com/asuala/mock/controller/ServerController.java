package com.asuala.mock.controller;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.enums.state.RecordEnum;
import com.asuala.mock.es.Es8Client;
import com.asuala.mock.es.entity.FileInfoEs;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.service.FileInfoService;
import com.asuala.mock.service.RecordService;
import com.asuala.mock.service.ServerService;
import com.asuala.mock.utils.MD5Utils;
import com.asuala.mock.utils.TimeUtils;
import com.asuala.mock.vo.FileInfo;
import com.asuala.mock.vo.Record;
import com.asuala.mock.vo.req.FileInfoReq;
import com.asuala.mock.vo.req.RebuildReq;
import com.asuala.mock.vo.req.SearchReq;
import com.asuala.mock.vo.req.UrlReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @description:
 * @create: 2023/08/09
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "server", havingValue = "true")
public class ServerController {

    private final RecordService recordService;
    private final ServerService serverService;
    private final FileInfoService fileInfoService;

    @Autowired(required = false)
    private Es8Client es8Client;

    //down:
    //  directory: ""
    @Value("${down.directory:'d:\\app\\'}")
    private String downDir;
    @Value("${down.http.salt}")
    private String salt;

    private static final List<String> fields = new ArrayList<String>() {{
        add("name");
    }};

    @PostConstruct
    public void init() {
        //TODO 2022-09-18: ElasticSearch 创建索引
        try {
            es8Client.createIndexSettingsMappings(FileInfoEs.class);
        } catch (Exception e) {
            log.error("连接es创建索引失败", e);
        }
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
//        File file = new File(downDir + req.getAuthor() + Constant.FILESEPARATOR +"+ req.getFileName() + ".mp4");
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

    @PostMapping("search")
    public JSONObject search(@RequestBody SearchReq req) throws IOException {
        JSONObject res = new JSONObject();
        res.put("code", 222);

        if (StringUtils.isBlank(req.getKey())) {
            res.put("msg", "关键字为空");
            return res;
        }
        Page<FileInfo> page = new Page<>(req.getPageNum(), req.getPageSize());
        List<FileInfo> list = fileInfoService.list(page, new LambdaQueryWrapper<FileInfo>().likeRight(FileInfo::getName, req.getKey()).orderByDesc(FileInfo::getChangeTime));
        if (list.size() > 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", page.getTotal());
            res.put("data", result);
            res.put("code", 200);
            return res;
        }
//        Query query = Query.of(q -> q.wildcard(w -> w.field("name").value(key)));
        Query query = Query.of(q -> q.matchPhrase(m -> m.query(req.getKey()).field("name").slop(6)));
//        Query query = Query.of(q -> q.match(m -> m.query(key).field("name")));
        Map<String, Object> map = es8Client.complexQueryHighlight(query, FileInfoEs.class, fields, req.getPageNum(), req.getPageSize());
        res.put("data", map);
        res.put("code", 200);
        return res;
    }

    @PostMapping("saveEs")
    public JSONObject saveEs(@RequestBody FileInfoReq req) {
        JSONObject res = new JSONObject();
        res.put("code", 222);
        if (StringUtils.isBlank(req.getSign())) {
            return res;
        }
        if (!MD5Utils.getSaltverifyMD5(req.getName(), salt, req.getSign())) {
            return res;
        }
        if (StringUtils.isBlank(req.getName())) {
            return res;
        }
        if (StringUtils.isBlank(req.getPath())) {
            return res;
        }
        if (null == req.getIndex()) {
            return res;
        }
        if (null == req.getId()) {
            return res;
        }
        FileInfoEs fileInfoEs = convertEs(req);

        es8Client.addData(fileInfoEs, false);
        res.put("code", 200);
        return res;
    }

    private FileInfoEs convertEs(FileInfoReq req) {
        FileInfoEs fileInfoEs = new FileInfoEs();
        fileInfoEs.setId(req.getId());
        fileInfoEs.setName(req.getName());
        fileInfoEs.setPath(req.getPath());
        fileInfoEs.setSuffix(req.getSuffix());
        fileInfoEs.setSize(req.getSize());
        fileInfoEs.setChangeTime(req.getChangeTime());
        fileInfoEs.setIndex(req.getIndex());
        return fileInfoEs;
    }

    @PostMapping("delEs")
    public JSONObject delEs(@RequestBody FileInfoReq req) throws IOException {
        JSONObject res = new JSONObject();
        res.put("code", 222);
        if (StringUtils.isBlank(req.getSign())) {
            return res;
        }
        if (!MD5Utils.getSaltverifyMD5(req.getName(), salt, req.getSign())) {
            return res;
        }
        if (null == req.getId()) {
            return res;
        }

        es8Client.delDocId(req.getId().toString(), FileInfoEs.class);
        res.put("code", 200);
        return res;
    }

    @PostMapping("rebuildData")
    public void rebuildData(@RequestBody RebuildReq req) throws IOException {
        if (StringUtils.isBlank(req.getSign())) {
            return;
        }
        if (null == req.getIndex()) {
            return;
        }
        if (!MD5Utils.getSaltverifyMD5(req.getIndex().toString(), salt, req.getSign())) {
            return;
        }
        new Thread(() -> {
            try {
                serverService.rebuildData(req);
            } catch (IOException e) {
                log.error("重构文件数据失败 index: {}", req.getIndex(), e);
            }
        }).start();
    }

    private String getFilName(String fileName) {
        File file = new File(downDir, fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file.getAbsolutePath();
    }

}