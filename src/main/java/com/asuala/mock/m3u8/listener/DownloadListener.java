package com.asuala.mock.m3u8.listener;

public interface DownloadListener {

    void start(String fileName);

    void process(String downloadUrl, int finished, int sum, float percent);

    void speed(String fileName,String speedPerSecond);

    void end();

}
