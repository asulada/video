package com.asuala.mock.m3u8.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 常量
 *
 * @author liyaling
 * @email ts_liyaling@qq.com
 * @date 2019/12/23 10:11
 */

public class Constant {

    public static int index;

    public static Map<String, Long> volumeNos = new HashMap<>();

    //文件分隔符，在window中为\\，在linux中为/
    public static final String FILESEPARATOR = System.getProperty("file.separator");

    //因子
    public static final float FACTOR = 1.15F;

    //默认文件每次读取字节数
    public static final int BYTE_COUNT = 40960;

    //日志级别 控制台不输出
    public static final int NONE = 0X453500;

    //日志级别 控制台输出所有信息
    public static final int INFO = 0X453501;

    //日志级别 控制台输出调试和错误信息
    public static final int DEBUG = 0X453502;

    //日志级别 控制台只输出错误信息
    public static final int ERROR = 0X453503;


    //停止下载的code
    public static final int STOP_CDDE = 472;
    public static final int SKIP_CDDE = 429;
    public static final int FORBIDDEN_CDDE = 403;
    public static final int SERVICE_NOT_AVALIABLE_CDDE = 503;

}
