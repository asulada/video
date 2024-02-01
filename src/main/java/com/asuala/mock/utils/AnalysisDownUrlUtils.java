package com.asuala.mock.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.vo.MediaDefinition;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
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

    @Value("${down.proxy.ip:127.0.0.1}")
    private String proxyIp;

    @Value("${down.proxy.port:7890}")
    private int proxyPort;

    public String analysisUrl(String name, String pageUrl) throws IOException {
        URL url = new URL(pageUrl);   //创建URL对象

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, proxyPort)));  //创建HttpURLConnection对象
        conn.setConnectTimeout((int) 10000);
        conn.setUseCaches(false);
        conn.setReadTimeout((int) 10000);
        conn.setDoInput(true);
        if (conn.getResponseCode() != 200) {
            log.error("{} 打开地址失败 {}", name, pageUrl);
            return null;
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
        for (int i = 0; i < 4; i++) {
            element = findScprit(name, script);
            if (null != element) {
                break;
            }
        }

        if (null != element) {
            int startIndex = element.data().indexOf("{");
            String substring = element.data().substring(startIndex, element.data().indexOf("\n", startIndex) - 1);
            JSONObject object = JSON.parseObject(substring);
            JSONArray mediaDefinitions = object.getJSONArray("mediaDefinitions");

            if (mediaDefinitions.size() > 0) {

                List<MediaDefinition> javaList = mediaDefinitions.toJavaList(MediaDefinition.class);
                javaList = javaList.stream().filter(item -> item.getQuality().matches("-?\\d+(\\.\\d+)?")).sorted((p1,p2)->Integer.parseInt(p2.getQuality())-Integer.parseInt(p1.getQuality())).collect(Collectors.toList());

//                List<MediaDefinition> quality = javaList.stream()
//                        .sorted(Comparator.comparingInt(MediaDefinition::getQuality).reversed()).collect(Collectors.toList());
                return javaList.get(0).getVideoUrl();
            }
        }
        log.error("{} 未解析到下载地址\n{}", name, sb.toString());
        return null;
    }

    private Element findScprit(String name, Elements script) {
        if (script.get(42).data().contains(key)) {
            log.debug("{} 发现变量在 42", name);
            return script.get(42);
        }
        if (script.get(47).data().contains(key)) {
            log.debug("{} 发现变量在 47", name);

            return script.get(47);
        }
        if (script.get(68).data().contains(key)) {
            log.debug("{} 发现变量在 68", name);

            return script.get(68);
        }
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