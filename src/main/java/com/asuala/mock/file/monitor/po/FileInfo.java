package com.asuala.mock.file.monitor.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    private String name;
    private String path;
    private int dir;
    private String suffix;
    private long size;
    private Date updateTime;
}