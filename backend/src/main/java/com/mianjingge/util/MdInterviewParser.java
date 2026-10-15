package com.mianjingge.util;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面试记录 md 解析器。
 * 支持的结构化格式（以《面试问答记录-第二轮》为基准）：
 * <pre>
 * # 模拟面试记录 · 第二轮（2026-10-14）
 * > 岗位：Java 后端（秋招 2027 届）· 形式：... · 共 14 题
 * ## 一、成绩总览
 * | # | 分类 | 题目 | 分数 |
 * | 1 | Arch-Design | ... | 5/10 |
 * ## 二、题目明细
 * ### 第 1 题 · 标题 —— 5/10
 * **题目**：...
 * **你的回答**：...
 * **扣分点**：...
 * **参考答案**：...
 * </pre>
 */
public final class MdInterviewParser {

    private static final Pattern FIELD_PATTERN = Pattern.compile("^\\*\\*([^*]+)\\*\\*[:：](.*)$");
    private static final Pattern SUMMARY_ROW =
            Pattern.compile("^\\|\\s*(\\d+)\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([^|]+?)\\s*\\|$");
    private static final Pattern ROUND_PATTERN = Pattern.compile("第\\s*([一二三四五六七八九十\\d]+)\\s*轮");
    private static final Pattern DATE_PATTERN = Pattern.compile("[（(](\\d{4}-\\d{2}-\\d{2})[）)]");
    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d+)");

    private MdInterviewParser() {
    }

    public static ParsedDoc parse(String content) {
        ParsedDoc doc = new ParsedDoc();
        // 状态：inSummary 成绩总览区 / inDetail 题目明细区
        boolean inSummary = false;
        boolean inDetail = false;
        String currentField = null;
        StringBuilder currentText = new StringBuilder();
        ParsedItem current = null;

        for (String raw : content.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 大标题：轮次 + 日期
            if (line.startsWith("# ") && !line.startsWith("##")) {
                Matcher m = ROUND_PATTERN.matcher(line);
                if (m.find()) {
                    doc.setRound("第" + m.group(1) + "轮");
                }
                m = DATE_PATTERN.matcher(line);
                if (m.find()) {
                    doc.setInterviewDate(LocalDate.parse(m.group(1)));
                }
                continue;
            }
            // 岗位行
            if (line.startsWith("> 岗位：")) {
                doc.setPosition(line.substring("> 岗位：".length()).split("[·，,]")[0].trim());
                continue;
            }
            // 分区切换
            if (line.startsWith("## 一、")) {
                inSummary = true;
                inDetail = false;
                continue;
            }
            if (line.startsWith("## 二、")) {
                inSummary = false;
                inDetail = true;
                continue;
            }

            if (inSummary) {
                // 成绩总览表行：| # | 分类 | 题目 | 分数 |
                Matcher row = SUMMARY_ROW.matcher(line);
                if (row.matches()) {
                    ParsedItem item = new ParsedItem();
                    item.setDocCategory(row.group(2));
                    item.setScore(parseScore(row.group(4)));
                    item.setAnswered(item.getScore() == null ? 3 : 1);
                    doc.getItems().add(item);
                }
                continue;
            }
            if (!inDetail) {
                continue;
            }

            // 题目明细：### 第 N 题 · 标题 —— 分数
            if (line.startsWith("### 第 ")) {
                flushField(current, currentField, currentText);
                current = currentOrNew(doc, line);
                currentField = null;
                continue;
            }
            // 字段标签行：**题目**：xxx（标签后内容跨行累积）
            Matcher fm = FIELD_PATTERN.matcher(line);
            if (fm.matches()) {
                flushField(current, currentField, currentText);
                currentField = fm.group(1);
                currentText = new StringBuilder(fm.group(2));
                continue;
            }
            // 字段内容延续行（含段内加粗等，不属于新标签行）
            if (currentField != null) {
                if (currentText.length() > 0) {
                    currentText.append('\n');
                }
                currentText.append(line);
            }
        }
        flushField(current, currentField, currentText);
        return doc;
    }

    /** 解析 "5/10"、"2/10（修正后）"；"—" 返回 null */
    private static Integer parseScore(String raw) {
        Matcher m = SCORE_PATTERN.matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /**
     * 明细段的题目标题优先于总览表标题（总览常带 "(修正后)" 等后缀）。
     * 若明细段未识别出标题（格式不符），新建一个 item 而不是覆盖总览行。
     */
    private static ParsedItem currentOrNew(ParsedDoc doc, String line) {
        String body = line.substring("###".length()).trim();
        int dot = body.indexOf('·');
        if (dot >= 0) {
            body = body.substring(dot + 1).trim();
        }
        int dash = body.lastIndexOf("——");
        String title;
        if (dash >= 0) {
            title = body.substring(0, dash).trim();
        } else {
            title = body;
        }
        // 明细段与总览表按题号对齐时更新同一 item；题号对不上则追加新 item
        Matcher m = Pattern.compile("第\\s*(\\d+)\\s*题").matcher(line);
        int index = m.find() ? Integer.parseInt(m.group(1)) - 1 : -1;
        ParsedItem item = index >= 0 && index < doc.getItems().size() ? doc.getItems().get(index) : null;
        if (item == null) {
            item = new ParsedItem();
            doc.getItems().add(item);
        }
        item.setTitle(title);
        if (dash >= 0) {
            item.setScore(parseScore(body.substring(dash + 2)));
            item.setAnswered(item.getScore() == null ? 3 : 1);
        }
        return item;
    }

    private static void flushField(ParsedItem item, String field, StringBuilder text) {
        if (item == null || field == null) {
            return;
        }
        String value = text.toString().trim();
        if (value.isEmpty()) {
            return;
        }
        switch (field) {
            case "题目" -> item.setTitle(value);
            case "你的回答" -> item.setMyAnswerSnapshot(value);
            case "扣分点" -> item.setDeduction(value);
            case "参考答案" -> item.setStdAnswer(value);
            default -> {
                // 其他加粗段落（如总结）不导入
            }
        }
    }

    @Data
    public static class ParsedDoc {
        private String round;
        private LocalDate interviewDate;
        private String position;
        private List<ParsedItem> items = new ArrayList<>();
    }

    @Data
    public static class ParsedItem {
        /** 文档中的分类（Arch-Design 等），导入前映射 */
        private String docCategory;
        private String title;
        private Integer score;
        /** 1 答上 2 部分答上 3 未答上（score 为空初判未答上，预览可改） */
        private Integer answered;
        private String myAnswerSnapshot;
        private String deduction;
        private String stdAnswer;
    }
}
