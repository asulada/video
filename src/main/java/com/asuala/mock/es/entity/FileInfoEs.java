package com.asuala.mock.es.entity;

import com.asuala.mock.es.annotation.DocId;
import com.asuala.mock.es.annotation.EsClass;
import com.asuala.mock.es.annotation.EsField;
import com.asuala.mock.es.enums.EsDataType;
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
@EsClass
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoEs {
    @DocId
    private Long id;
    @EsField(type = EsDataType.TEXT, analyzer = "ngram_analyzer")
    private String name;
    @EsField(type = EsDataType.KEYWORD)
    private String path;
    @EsField(type = EsDataType.KEYWORD)
    private String suffix;
    @EsField(type = EsDataType.LONG)
    private Long size;
    @EsField(type = EsDataType.DATE)
    private Date changeTime;
    @EsField(type = EsDataType.INTEGER)
    private Integer index;

}