package com.asuala.mock.spider.task;

import com.asuala.mock.enums.state.ChannelDetailsEnum;
import com.asuala.mock.enums.state.ChannelEnum;
import com.asuala.mock.enums.state.RecordEnum;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.mapper.*;
import com.asuala.mock.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2024/02/06
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "txtRole", havingValue = "true")
public class CommonSpiderTask {

    private final ChannelMapper channelMapper;
    private final ChannelDetailsMapper channelDetailsMapper;
    private final RecordMapper recordMapper;
    private final FileInfoMapper fileInfoMapper;
    private final ChannelRepeatMapper channelRepeatMapper;

    @Value("${down.proxy.ip:127.0.0.1}")
    private String proxyIp;

    @Value("${down.proxy.port:7890}")
    private int proxyPort;

    private static final String regexSpecial = "[\\\\/:*?\"<>|]";
    private static final String regexSpace = "\\s+";
    private static final String replacement = " ";

    private static boolean excuteFlag = true;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void excute() throws IOException {
        if (!excuteFlag) {
            return;
        }
        excuteFlag = false;
        try {
            List<Channel> list = channelMapper.selectList(new Page<Channel>(1, 10), new LambdaQueryWrapper<Channel>().eq(Channel::getState, ChannelEnum.UNTREATED.getCode()).orderByAsc(Channel::getId));
            if (list.size() > 0) {
                List<ChannelDetails> itemList = new ArrayList<>();
                List<Record> recordList = new ArrayList<>();
                Date date = new Date();
                for (Channel channel : list) {
                    Channel.ChannelBuilder builder = Channel.builder().id(channel.getId()).updateTime(date);
                    URL url = new URL(channel.getUrl());
                    String domian = url.getProtocol() + "://" + url.getHost();
                    try {
                        findItem(itemList, date, channel, domian, channel.getUrl(), recordList);
                        builder.state(ChannelEnum.HANDLED.getCode());
                    } catch (Exception e) {
                        builder.state(ChannelEnum.SPIDER_FAILED.getCode());
                        log.error("爬取内容异常: {}", channel.getName(), e);
                    } finally {
                        channelMapper.updateById(builder.build());
                    }
                }
                itemList = itemList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getName() + ";" + o.getAuthor()))), ArrayList::new));

                if (itemList.size() > 0) {
                    channelDetailsMapper.batchInsert(itemList);
                }
                recordList = recordList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getName() + ";" + o.getAuthor()))), ArrayList::new));

                if (recordList.size() > 0) {
                    recordMapper.batchInsert(recordList);
                }
            }
        } finally {
            excuteFlag = true;
        }

    }

    private void findItem(List<ChannelDetails> itemList, Date date, Channel channel, String domian, String urlStr, List<Record> recordList) throws IOException {

        Document document = Jsoup.parse(getHtml(urlStr));
        Elements elements = document.select(".pcVideoListItem");

        String tmpAuthor = "";
        Element titleEle = document.selectFirst(".nameSubscribe .name h1");
        if (null != titleEle) {
            tmpAuthor = titleEle.text();
        }
        if ("".equals(tmpAuthor)) {
//            titleEle = document.selectFirst(".header .title h1");
            tmpAuthor = document.selectFirst(".header").selectFirst(".title").selectFirst("h1").text();
        }
        if (elements.size() > 0) {
            for (Element element : elements) {

                Element titleA = element.selectFirst("span.title a");
                String picUrl = element.selectFirst("img.js-pop").attr("src");
                String href = titleA.attr("href");
                String title = removeSpecialCharacters(titleA.attr("title"));

                String authorLi = tmpAuthor;
                titleEle = element.selectFirst("div.usernameWrap a");
                if (null != titleEle) {
                    authorLi = titleEle.text();
                }

                ChannelDetails channelDetails = new ChannelDetails();
                channelDetails.setPId(channel.getId());
                channelDetails.setUrl(domian + href);
                channelDetails.setPicUrl(picUrl);
                channelDetails.setName(title);
                channelDetails.setAuthor(removeSpecialCharacters(authorLi));
                channelDetails.setCreateTime(date);
                channelDetails.setState(ChannelDetailsEnum.UNTREATED.getCode());
                Long rId = recordMapper.findIdByNameAndAuthor(title, authorLi);
                if (null != rId) {
                    channelDetails.setState(ChannelDetailsEnum.REPEAT_RECORD.getCode());//record重复
                    channelDetails.setRId(rId);
                } else {
                    List<FileInfo> fileInfos = fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>().select(FileInfo::getId, FileInfo::getName, FileInfo::getPath).likeRight(FileInfo::getName, title));
                    if (fileInfos.size() > 0) {
                        channelDetails.setState(ChannelDetailsEnum.REPEAT_FILE.getCode());//record重复
                        channelDetailsMapper.insert(channelDetails);
                        List<ChannelRepeat> list = new ArrayList<>();
                        for (FileInfo fileInfo : fileInfos) {
                            ChannelRepeat repeat = new ChannelRepeat();
                            repeat.setDetailId(channelDetails.getId());
                            repeat.setFId(fileInfo.getId());
                            repeat.setName(fileInfo.getName());
                            repeat.setPath(fileInfo.getPath());
                            list.add(repeat);
                        }
                        channelRepeatMapper.batchInsert(list);
                        continue;
                    } else {
                        Record record = new Record();
                        record.setName(title);
                        record.setCreateTime(date);
                        record.setState(RecordEnum.UNTREATED.getCode());
                        record.setDelFlag(1);
                        record.setPageUrl(domian + href);
                        record.setAuthor(authorLi);
                        record.setPicUrl(picUrl);
                        record.setIndex(Constant.index);
                        recordList.add(record);
                    }
                }
                itemList.add(channelDetails);
            }
            Element paginationEle = document.selectFirst(".pagination3");
            if (null != paginationEle) {
                Element nextPage = paginationEle.selectFirst(".page_next");
                if (null != nextPage && !nextPage.hasClass("disabled")) {
                    String nextHref = nextPage.selectFirst("a").attr("href");
                    findItem(itemList, date, channel, domian, domian + nextHref, recordList);
                }
            }
        }
    }

    private static String getHtml(String urlStr) {
        HttpURLConnection conn = null;
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlStr);   //创建URL对象

            conn = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)));  //创建HttpURLConnection对象
            conn.setConnectTimeout((int) 10000);
            conn.setUseCaches(false);
            conn.setReadTimeout((int) 10000);
            conn.setDoInput(true);

            //打印响应内容
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String str = null;
            while ((str = br.readLine()) != null) {
                sb.append(str).append("\n");
            }
        } catch (Exception e) {
            log.error("连接失败", e);
        } finally {
            if (null != conn) {
                conn.disconnect();//断开连接
            }
        }
        return sb.toString();
    }

    public static String getProtocolAndDomain(String url) {
        String protocolAndDomain = "";
        try {
            URL urlObj = new URL(url);
            protocolAndDomain = urlObj.getProtocol() + "://" + urlObj.getHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return protocolAndDomain;
    }

    private static String removeSpecialCharacters(String fileName) {
        fileName = fileName.replaceAll(regexSpecial, replacement);
        fileName = fileName.replaceAll(regexSpace, replacement);
        return fileName.trim();
    }
}