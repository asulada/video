package com.asuala.mock.file.monitor.linux;

import lombok.Data;

/**
 * @description:
 * @create: 2024/05/10
 **/
@Data
public class FileVo {
    private String fullPath;
    private String path;
    private String name;
    private int code;
    private boolean isDir;
}