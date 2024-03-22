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

    private static final String regexSpecial = "[\\\\/:*?\"<>|]";
    private static final String regexSpace = "\\s+";
    private static final String replacement = " ";

    private static String removeSpecialCharacters(String fileName) {
        fileName = fileName.trim();
        fileName = fileName.replaceAll(regexSpecial, replacement);
        fileName = fileName.replaceAll(regexSpace, replacement);
        return fileName.trim();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
    }


}