package com.asuala.mock.vo;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
    * 文件信息
    */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileInfo {
    private Long id;

    /**
    * 名称
    */
    private String name;

    /**
    * 路径
    */
    private String path;

    /**
    * 后缀名
    */
    private String suffix;

    /**
    * 大小
    */
    private Long size;

    /**
    * 更新时间
    */
    private Date updateTime;

    /**
    * 0 文件 1 文件夹 
    */
    private Integer dir;

    private Date createTime;

    /**
    * 文件修改时间
    */
    private Date changeTime;

    @TableField("`index`")
    private Integer index;
}