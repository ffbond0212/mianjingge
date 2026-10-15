package com.mianjingge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ImportParseResponse {

    private ImportInterviewInfo interview;

    private List<ImportItem> items;

    /** 解析出的题目总数 */
    private int total;

    /** 与题库已有题标题全等、可复用的数量 */
    private int matchedCount;
}
