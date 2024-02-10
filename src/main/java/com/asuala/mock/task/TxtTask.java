package com.asuala.mock.task;

import com.asuala.mock.enums.state.RecordEnum;
import com.asuala.mock.service.RecordService;
import com.asuala.mock.vo.Record;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.asuala.mock.m3u8.utils.Constant.FILESEPARATOR;

/**
 * @description:
 * @create: 2024/01/27
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "txtRole", havingValue = "true")
public class TxtTask {

    private final RecordService recordService;


    @Value("${down.directory:'d:\\app\\'}")
    private String downDir;
    @Value("${down.search.pageSize:5}")
    private int pageSize;

    @Scheduled(cron = "*/15 * * * * ?")
    public void txt() {
        int pageNow = 1;
        List<Long> txtIds = new ArrayList<>();
        while (true) {
            Page<Record> page = new Page<>(pageNow++, pageSize);
            Page<Record> recordPage = recordService.page(page, new LambdaQueryWrapper<Record>().eq(Record::getState, RecordEnum.TXT.getCode()).orderByAsc(Record::getId));
            if (recordPage.getRecords().size() == 0) {
                break;
            }
            for (Record record : recordPage.getRecords()) {
                buildNotxt(record);
                txtIds.add(record.getId());
            }
        }
        if (txtIds.size() > 0) {
            recordService.update(new LambdaUpdateWrapper<Record>().set(Record::getState, RecordEnum.TXT_DOWNLOADED.getCode()).in(Record::getId, txtIds));
        }
    }

    private void buildNotxt(Record record) {
        StringBuilder builder = new StringBuilder(downDir);
        builder.append(record.getAuthor()).append(FILESEPARATOR).append(record.getName()).append(".txt");
        File file = new File(builder.toString());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
                log.debug("新建txt文件 {}", file.getName());
            } catch (IOException e) {
                log.error("创建txt文件失败", e);
            }
        }
    }
}