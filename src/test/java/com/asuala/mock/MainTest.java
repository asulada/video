package com.asuala.mock;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.vo.MediaDefinition;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2023/08/23
 **/
public class MainTest {
    private static String sql = "";


    public static void main(String[] args) throws InterruptedException, IOException {

//        HttpConnTest();
        BufferedReader br = new BufferedReader(new FileReader("d:\\tmp\\1213.html"));
        StringBuilder sb = new StringBuilder();

        String str = null;
        while ((str = br.readLine()) != null) {
            sb.append(str).append("\n");
        }
        Document parse = Jsoup.parse(sb.toString());
        Elements script = parse.select("script");
        int i = 0;
        List<JSONObject> list = new ArrayList<>();
        for (Element element : script) {
            //第42个elemetn 68 47
            if (element.data().contains("var flashvars_")) {
                int startIndex = element.data().indexOf("{");
                String substring = element.data().substring(startIndex, element.data().indexOf("\n", startIndex) - 1);
                System.out.println(substring);
                JSONObject object = JSON.parseObject(substring);
                JSONArray mediaDefinitions = object.getJSONArray("mediaDefinitions");
                List<MediaDefinition> javaList = mediaDefinitions.toJavaList(MediaDefinition.class);

                javaList = javaList.stream().filter(item -> item.getQuality().matches("-?\\d+(\\.\\d+)?")).sorted((p1,p2)->Integer.parseInt(p2.getQuality())-Integer.parseInt(p1.getQuality())).collect(Collectors.toList());

//                List<MediaDefinition> quality = javaList.stream()
//                        .sorted(Comparator.comparingInt(MediaDefinition::getQuality).reversed()).collect(Collectors.toList());
                System.out.println(JSON.toJSONString(javaList));
                break;
            }
            i++;
        }
    }


}