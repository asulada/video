package com.asuala.mock.task;

import com.asuala.mock.m3u8.download.M3u8DownloadFactory;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.service.IndexService;
import com.asuala.mock.service.RecordPageService;
import com.asuala.mock.service.RecordService;
import com.asuala.mock.utils.AnalysisDownUrlUtils;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.MediaDefinition;
import com.asuala.mock.vo.Record;
import com.asuala.mock.vo.RecordPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2024/01/14
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "client", havingValue = "true")
public class CommonTask {
    private final RecordService recordService;
    private final RecordPageService recordPageService;
    private final IndexService indexService;
    private final AnalysisDownUrlUtils analysisDownUrlUtils;

    @Value("${down.directory:'d:\\app\\'}")
    private String downDir;
    @Value("${down.directoryPage:'d:\\app\\xyz\\'}")
    private String downPageDir;

    @Value("${down.task.num:3}")
    private int etistCount;

    @Value("${down.task.childthread:1}")
    private int childthread;

    @Value("${down.task.pauseSecond:60}")
    private int pauseSecond;

    @Value("${down.search.pageSize:5}")
    private int pageSize;

    @Value("${down.proxy.ip:127.0.0.1}")
    private String proxyIp;

    @Value("${down.proxy.port:7890}")
    private int proxyPort;

    private static boolean flag = true;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Map<String, M3u8DownloadFactory.M3u8Download> downloads = new HashMap<>();

    @PostConstruct
    public void mkdirs() {
//        File file = new File(downPageDir);
//        if (!file.exists()) {
//            file.mkdirs();
//        }
        CacheUtils.pauseSecond = pauseSecond;
    }

    @Scheduled(cron = "${down.addTaskCron}")
    public void addTask() throws IOException {
        if (CacheUtils.getCacheRecord().size() < (pageSize / 2)) {
            List<Record> list = new ArrayList<>();
            List<Long> deleteIds = new ArrayList<>();
            getList(1, new Date().getTime() - 60000 * 59, list, deleteIds);
            if (deleteIds.size() > 0) {
                recordService.removeByIds(deleteIds);
            }
            if (list.size() == 0) {
                if (checkDelete()) {
                    addTask();
                }
            }
            if (list.size() > 0) {
                CacheUtils.addCacheRecord(list);
            }
        }
    }

    @Scheduled(cron = "${down.downCron}")
    public void down() {
        if (!(flag && CacheUtils.downFlag(null))) {
            return;
        }
        flag = false;
        try {
            if (CacheUtils.getCacheRecord().size() != 0) {
                if (downloads.size() == 0) {
                    CacheUtils.setLastId(CacheUtils.setLastId(null));
                }
                addTask(CacheUtils.getCacheRecord().values());
            }
        } catch (Exception e) {
            log.error("定时调度失败", e);
        } finally {
            flag = true;
        }
    }

    private void addTask(Collection<Record> list) {
        LocalDateTime oldTime = LocalDateTime.now().minusSeconds(300);

        for (Map.Entry<String, M3u8DownloadFactory.M3u8Download> entry : downloads.entrySet()) {
            M3u8DownloadFactory.M3u8Download download = entry.getValue();
            if (oldTime.isAfter(download.getStartTime())) {
                download.stopDown();
            }
        }
        for (int i = downloads.size(); i < etistCount; i++) {
            for (Record record : list) {
                String fileName = record.getName() + "-" + record.getQuality() + "p";
                if (downloads.containsKey(fileName) || CacheUtils.cache(null, null).containsKey(fileName)) {
                    i--;
                    break;
                }
                CacheUtils.setLastId(record.getId());
                downloads.put(fileName, down(fileName, record));
                break;
            }

        }
    }


    private void getList(int pageNow, long time, List<Record> list, List<Long> deleteIds) {
        Page<Record> page = new Page<>(pageNow++, pageSize);
        Page<Record> recordPage = recordService.page(page, new LambdaQueryWrapper<Record>().eq(Record::getState, 0).eq(Record::getIndex, Constant.index).gt(Record::getId, CacheUtils.getLastCacheRecordKey()).orderByAsc(Record::getId));

        if (recordPage.getRecords().size() == 0) {
            return;
        }

        for (Record record : recordPage.getRecords()) {
            long end = 0L;
            if (null != record.getUpdateTime()) {
                end = record.getUpdateTime().getTime();
            } else {
                end = record.getCreateTime().getTime();
            }
            if (time > end) {
                deleteIds.add(record.getId());
            } else {
                list.add(record);
            }
        }
        if (list.size() < pageSize) {
            getList(pageNow, time, list, deleteIds);
        }
    }


    private M3u8DownloadFactory.M3u8Download down(String fileName, Record record) {

        log.info("开始下载: 《{}》", fileName);
        M3u8DownloadFactory.M3u8Download m3u8Download = new M3u8DownloadFactory().getInstance(record.getUrl());
        //设置生成目录
        m3u8Download.setDir(downDir + record.getAuthor() + Constant.FILESEPARATOR);
        m3u8Download.setPageDir(downPageDir + record.getAuthor() + Constant.FILESEPARATOR);
        //设置视频名称
        m3u8Download.setFileName(fileName);
        //设置线程数
        m3u8Download.setThreadCount(childthread);
        //设置重试次数
        m3u8Download.setRetryCount(100);
        //设置连接超时时间（单位：毫秒）
        m3u8Download.setTimeoutMillisecond(10000L);

        m3u8Download.setId(record.getId());
        m3u8Download.setRecordService(recordService);
        m3u8Download.setRecordPageService(recordPageService);
//        m3u8Download.setAnalysisDownUrlUtils(analysisDownUrlUtils);
        m3u8Download.setStartTime(LocalDateTime.now());
        m3u8Download.setOverPage(recordPageService.list(new LambdaQueryWrapper<RecordPage>().select(RecordPage::getNum).eq(RecordPage::getPId, record.getId())).stream().map(RecordPage::getNum).collect(Collectors.toSet()));
        m3u8Download.setPicUrl(record.getPicUrl());
        m3u8Download.setFailNum(record.getFailNum());
        //添加额外请求头
      /*  Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("Content-Type", "text/html;charset=utf-8");
        m3u8Download.addRequestHeaderMap(headersMap);*/
        //如果需要的话设置http代理
        m3u8Download.setProxy(proxyIp, proxyPort);
        //开始下载
        m3u8Download.start();
        return m3u8Download;
    }

    @Scheduled(cron = "*/30 * * * * ?")
    public void checkExpire() {
        CacheUtils.cache(null, LocalDateTime.now());
    }

    //    @Scheduled(cron = "0 */5 * * * ?")
    public boolean checkDelete() throws IOException {
        int num = 0;
        int pageNow = 1;
        List<Record> list = new ArrayList<>();
        while (num < pageSize) {
            Page<Record> page = new Page<>(pageNow++, pageSize);
            List<Record> recordPage = recordService.pagePageUrl(page, Constant.index);
            if (recordPage.size() == 0) {
                break;
            }
            Date date = new Date();
            for (Record record : recordPage) {
                if (StringUtils.isNotBlank(record.getPageUrl())) {
                    Record result = analysisDownUrlUtils.analysisUrl(record, 0, date);
                    list.add(result);
                    if (null == result.getState()) {
                        num++;
                    }
                }
            }
        }
        if (list.size() > 0) {
            recordService.updateBatchSelective(list);
            return true;
        }
        return false;
    }

    @Scheduled(cron = "0 0/20 * * * ?")
    public void exsit() {
        indexService.updateUpdateTimeById(Constant.index);
    }
}