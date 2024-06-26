package com.asuala.mock.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.enums.state.RecordEnum;
import com.asuala.mock.vo.MediaDefinition;
import com.asuala.mock.vo.Record;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2024/01/21
 **/
@Slf4j
@Component
public class AnalysisDownUrlUtils {

    @Value("${down.keywordPre:var flashvars_}")
    private String key;

    @Value("${down.html.video.list}")
    private String videoList;

    @Value("${down.html.video.duration}")
    private String duration;

    @Value("${down.proxy.ip:127.0.0.1}")
    private String proxyIp;

    @Value("${down.proxy.port:7890}")
    private int proxyPort;

    private static final Set<Integer> varSet = new LinkedHashSet<Integer>() {{
        add(36);
        add(37);
        add(38);
        add(39);
        add(42);
        add(47);
    }};

    public Record analysisUrl(Record record, Date date) throws IOException {
        for (int i = 0; i < 2; i++) {
            URL url = null;
            try {
                url = new URL(record.getPageUrl());   //创建URL对象
            } catch (Exception e) {
                log.error("{} {} 获取地址失败!", record.getId(), record.getName(), e);
                continue;
            }
            if (null != url) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, proxyPort)));  //创建HttpURLConnection对象
                conn.setConnectTimeout((int) 10000);
                conn.setUseCaches(false);
                conn.setReadTimeout((int) 10000);
                conn.setDoInput(true);
                if (conn.getResponseCode() != 200) {
                    log.error("{} 打开地址失败 {}", record.getName(), record.getPageUrl());
                    continue;
                }
                //打印响应内容
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String str = null;
                while ((str = br.readLine()) != null) {
                    sb.append(str).append("\n");
                }
                conn.disconnect();//断开连接

                Document parse = Jsoup.parse(sb.toString());
                Elements script = parse.select("script");
                Element element = null;
                element = findScprit(record.getName(), script);
                if (null != element) {
                    int startIndex = element.data().indexOf("{");
                    String substring = element.data().substring(startIndex, element.data().indexOf("\n", startIndex) - 1);
                    JSONObject object = JSON.parseObject(substring);
                    JSONArray mediaDefinitions = object.getJSONArray(videoList);
                    long videoDuration = object.getLongValue(duration, 0L);
                    String imageUrl = object.getString("image_url");

                    if (mediaDefinitions.size() > 0) {
                        String author = "";
                        List<MediaDefinition> javaList = mediaDefinitions.toJavaList(MediaDefinition.class);
                        javaList = javaList.stream().filter(item -> item.getQuality().matches("-?\\d+(\\.\\d+)?")).sorted((p1, p2) -> Integer.parseInt(p2.getQuality()) - Integer.parseInt(p1.getQuality())).collect(Collectors.toList());
                        MediaDefinition downMedia = javaList.get(0);
                        //TODO-asuala 2024-02-23: 获取作者 .userInfo  first
                        Element userInfo = parse.selectFirst(".userInfo");
                        if (null != userInfo) {
                            Element usernameWrap = userInfo.selectFirst(".usernameWrap");
                            if (null != usernameWrap) {
                                author = ConstantMethodUtils.removeSpecialCharacters(usernameWrap.selectFirst("a").text());
                            }
                        }


//                List<MediaDefinition> quality = javaList.stream()
//                        .sorted(Comparator.comparingInt(MediaDefinition::getQuality).reversed()).collect(Collectors.toList());
                        return Record.builder().id(record.getId()).author(author).url(downMedia.getVideoUrl()).picUrl(imageUrl).quality(downMedia.getQuality()).state(RecordEnum.UNTREATED.getCode()).updateTime(date).timeHum(TimeUtils.convertSecondsToHMS(videoDuration)).build();
                    }
                }
                log.error("{} 未解析到下载地址\n{}", record.getName(), sb.toString());
            }
        }

        return Record.builder().id(record.getId()).state(RecordEnum.PARSE_DOWNLOAD_FAIL.getCode()).delFlag(0).updateTime(date).timeHum("-").build();
    }

    private Element findScprit(String name, Elements script) {
        for (Integer index : varSet) {
            if (index > script.size() - 1) {
                break;
            }
            if (script.get(index).data().contains(key)) {
                log.debug("{} 发现变量在 {}", name, index);
                return script.get(index);
            }
        }
//        if (script.get(68).data().contains(key)) {
//            log.debug("{} 发现变量在 68", name);
//            return script.get(68);
//        }
        for (int i = 0; i < script.size(); i++) {
            Element element = script.get(i);
            if (element.data().contains(key)) {
                log.debug("{} 发现变量在 {}", name, i);
                return element;
            }
        }
        return null;
    }


}