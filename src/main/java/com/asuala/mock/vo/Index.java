package com.asuala.mock.vo;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("`index`")
public class Index {
    private Long id;

    /**
     * cpuid
     */
    private String cpuId;

    /**
     * 系统名
     */
    @TableField("`system`")
    private String system;

    /**
     * 创建时间
     */
    private Date createTime;
    private Date updateTime;
    private Integer delFlag;
}