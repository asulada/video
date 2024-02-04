package com.asuala.mock.vo.req;

import lombok.Data;

import java.util.Date;

/**
 * @description:
 * @create: 2024/02/02
 **/
@Data
public class FileInfoReq {

    private Long id;
    private String name;
    private String path;
    private String suffix;
    private Long size;
    private Date changeTime;
    private Integer index;
    private String sign;
}