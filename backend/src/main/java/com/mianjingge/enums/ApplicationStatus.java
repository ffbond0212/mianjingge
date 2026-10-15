package com.mianjingge.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 投递状态机：固定主流程顺序推进，任意非终态可转 REJECTED。
 * 数据库存枚举名。
 */
@Getter
@AllArgsConstructor
public enum ApplicationStatus {

    APPLIED("已投递"),
    WRITTEN("笔试"),
    FIRST("一面"),
    SECOND("二面"),
    THIRD("三面"),
    HR("HR面"),
    OFFER("Offer"),
    REJECTED("已挂/已拒");

    private final String label;

    public static ApplicationStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /** 终态：Offer / 已挂，不可再流转 */
    public boolean isTerminal() {
        return this == OFFER || this == REJECTED;
    }

    /** 主流程下一状态（终态无下一步） */
    public ApplicationStatus next() {
        return switch (this) {
            case APPLIED -> WRITTEN;
            case WRITTEN -> FIRST;
            case FIRST -> SECOND;
            case SECOND -> THIRD;
            case THIRD -> HR;
            case HR -> OFFER;
            case OFFER, REJECTED -> null;
        };
    }
}
