package com.mianjingge.vo;

import com.mianjingge.entity.Question;
import com.mianjingge.enums.QuestionCategory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionVO {

    private Long id;

    private String title;

    /** 分类枚举名 */
    private String category;

    /** 分类中文名 */
    private String categoryLabel;

    private String tags;

    /** 1 简单 2 中等 3 困难 */
    private Integer difficulty;

    private String stdAnswer;

    private String myAnswer;

    /** PRESET 预置 / IMPORT 导入 / MANUAL 手动 */
    private String source;

    private Integer reviewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static QuestionVO from(Question q) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setTitle(q.getTitle());
        vo.setCategory(q.getCategory().name());
        vo.setCategoryLabel(q.getCategory().getLabel());
        vo.setTags(q.getTags());
        vo.setDifficulty(q.getDifficulty());
        vo.setStdAnswer(q.getStdAnswer());
        vo.setMyAnswer(q.getMyAnswer());
        vo.setSource(q.getSource());
        vo.setReviewCount(q.getReviewCount());
        vo.setCreatedAt(q.getCreatedAt());
        vo.setUpdatedAt(q.getUpdatedAt());
        return vo;
    }
}
