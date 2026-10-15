package com.mianjingge.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MdInterviewParserTest {

    private static final String DOC = """
            # 模拟面试记录 · 第二轮（2026-10-14）
            > 岗位：Java 后端（秋招 2027 届）· 形式：视频面 · 共 3 题
            ## 一、成绩总览
            | # | 分类 | 题目 | 分数 |
            | 1 | Lang-Runtime | JVM 内存结构 | 5/10 |
            | 2 | Database | 索引失效场景 | — |
            | 3 | Behavior | 自我介绍 | 8/10 |
            ## 二、题目明细
            ### 第 1 题 · JVM 内存结构 —— 5/10
            **题目**：说说 JVM 内存结构
            **你的回答**：堆、栈、方法区
            补充：还有程序计数器
            **扣分点**：漏了本地方法栈
            **参考答案**：堆/栈/方法区/程序计数器/本地方法栈
            ### 第 2 题 · 索引失效场景 —— 2/10（修正后）
            **你的回答**：不知道
            **参考答案**：最左前缀等
            ### 第 3 题
            **你的回答**：基本情况介绍
            """;

    @Test
    void parseExtractsHeaderInfo() {
        MdInterviewParser.ParsedDoc doc = MdInterviewParser.parse(DOC);
        assertThat(doc.getRound()).isEqualTo("第二轮");
        assertThat(doc.getInterviewDate()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(doc.getPosition()).isEqualTo("Java 后端（秋招 2027 届）");
    }

    @Test
    void parseExtractsSummaryRowsWithScoreAndAnswered() {
        MdInterviewParser.ParsedDoc doc = MdInterviewParser.parse(DOC);
        assertThat(doc.getItems()).hasSize(3);
        assertThat(doc.getItems().get(0).getDocCategory()).isEqualTo("Lang-Runtime");
        assertThat(doc.getItems().get(0).getScore()).isEqualTo(5);
        assertThat(doc.getItems().get(0).getAnswered()).isEqualTo(1);
        // 分数为 — 的行：score/answered 被明细段分数后缀修正（见明细合并用例）
        assertThat(doc.getItems().get(1).getDocCategory()).isEqualTo("Database");
        // 明细段无分数后缀时保留总览分数
        assertThat(doc.getItems().get(2).getScore()).isEqualTo(8);
    }

    @Test
    void parseMergesDetailFieldsIntoAlignedItems() {
        MdInterviewParser.ParsedDoc doc = MdInterviewParser.parse(DOC);
        MdInterviewParser.ParsedItem first = doc.getItems().get(0);
        // 明细段标题覆盖总览标题；字段按标签落位；跨行内容累积
        assertThat(first.getTitle()).isEqualTo("说说 JVM 内存结构");
        assertThat(first.getMyAnswerSnapshot()).isEqualTo("堆、栈、方法区\n补充：还有程序计数器");
        assertThat(first.getDeduction()).isEqualTo("漏了本地方法栈");
        assertThat(first.getStdAnswer()).isEqualTo("堆/栈/方法区/程序计数器/本地方法栈");
        // 明细标题分数覆盖总览分数（含 "（修正后）" 后缀仍解析）
        assertThat(doc.getItems().get(1).getScore()).isEqualTo(2);
        // 无 —— 分数后缀的明细段保留总览分数
        assertThat(doc.getItems().get(2).getScore()).isEqualTo(8);
        assertThat(doc.getItems().get(2).getMyAnswerSnapshot()).isEqualTo("基本情况介绍");
    }

    @Test
    void parseToleratesMissingDetailSection() {
        String onlySummary = """
                # 模拟面试记录 · 第一轮（2026-10-01）
                ## 一、成绩总览
                | # | 分类 | 题目 | 分数 |
                | 1 | Framework | Spring IOC | 6/10 |
                | 2 | Database | 索引失效场景 | — |
                """;
        MdInterviewParser.ParsedDoc doc = MdInterviewParser.parse(onlySummary);
        assertThat(doc.getRound()).isEqualTo("第一轮");
        assertThat(doc.getPosition()).isNull();
        assertThat(doc.getItems()).hasSize(2);
        assertThat(doc.getItems().get(0).getDocCategory()).isEqualTo("Framework");
        assertThat(doc.getItems().get(0).getTitle()).isNull();
        // 无明细覆盖时：— 行的 score 为空，answered 判未答上
        assertThat(doc.getItems().get(1).getScore()).isNull();
        assertThat(doc.getItems().get(1).getAnswered()).isEqualTo(3);
    }
}
