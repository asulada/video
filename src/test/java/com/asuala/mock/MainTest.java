package com.asuala.mock;

import cn.hutool.core.util.IdUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static void main(String[] args) {
        Path path = Paths.get("D:\\web\\linux\\linux\\include\\uapi\\linux");
        System.out.println(path.getFileName().toString());
    }


}