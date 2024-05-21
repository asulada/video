package com.asuala.mock;

import com.asuala.mock.mapper.RecordMapper;
import com.asuala.mock.transcode.TranscodeService;
import com.asuala.mock.vo.Record;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @create: 2023/12/27
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MockApplication.class)
@Slf4j
public class MockApplicationTest {

    private static final String regexSpecial = "[\\\\/:*?\"<>|]";
    private static final String regexSpace = "\\s+";
    private static final String replacement = " ";

    private static String removeSpecialCharacters(String fileName) {
        fileName = fileName.replaceAll(regexSpecial, replacement);
        fileName = fileName.replaceAll(regexSpace, replacement);
        return fileName.trim();
    }

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private TranscodeService transcodeService;

    @Test
    public void init() {
        List<Record> recordList = recordMapper.selectList(new LambdaQueryWrapper<Record>().gt(Record::getId, 6346).eq(Record::getState, 0));
        List<Record> updates = new ArrayList<>();
        for (Record record : recordList) {
            Record record1 = new Record();
            record1.setId(record.getId());
            record1.setName(removeSpecialCharacters(record.getName()));
            record1.setAuthor(removeSpecialCharacters(record.getAuthor()));
            updates.add(record1);
        }
//        recordMapper.updateBatchSelective(updates);
    }

    @Test
    public void init1() throws IOException {
//        commonSpiderTask.excute();
//        List<Record> recordList = recordMapper.findByIdAndState(6346L, 0);
//        for (Record record : recordList) {
//            if (recordMapper.countByNameAndAuthorAndIdNot(record.getName(), record.getAuthor(), record.getId()) > 0) {
//                recordMapper.deleteByPrimaryKey(record.getId());
//            }
//        }
        File file = new File("");
    }

    @Test
    public void test02() throws IOException {
        transcodeService.ranscodeVideo(new File("D:\\"));
    }
}