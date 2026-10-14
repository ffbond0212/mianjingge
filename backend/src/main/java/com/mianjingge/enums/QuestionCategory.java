package com.mianjingge.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 题目分类（固定十大类）：数据库存枚举名，接口返回 code + label
 */
@Getter
@AllArgsConstructor
public enum QuestionCategory {

    JAVA_BASIC("Java基础"),
    CONCURRENCY("并发"),
    JVM("JVM"),
    MYSQL("MySQL"),
    REDIS("Redis"),
    SPRING("Spring"),
    NETWORK("网络"),
    OS("操作系统"),
    DISTRIBUTED("分布式"),
    BEHAVIOR("行为面");

    private final String label;

    public static QuestionCategory fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
