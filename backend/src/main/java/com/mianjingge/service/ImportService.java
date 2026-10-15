package com.mianjingge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianjingge.common.BusinessException;
import com.mianjingge.common.ErrorCode;
import com.mianjingge.dto.ImportConfirmRequest;
import com.mianjingge.dto.ImportInterviewInfo;
import com.mianjingge.dto.ImportItem;
import com.mianjingge.dto.ImportParseResponse;
import com.mianjingge.entity.Interview;
import com.mianjingge.entity.InterviewQuestion;
import com.mianjingge.entity.Question;
import com.mianjingge.enums.QuestionCategory;
import com.mianjingge.mapper.InterviewMapper;
import com.mianjingge.mapper.InterviewQuestionMapper;
import com.mianjingge.mapper.QuestionMapper;
import com.mianjingge.util.MdInterviewParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportService {

    /** Lang-Runtime 细分：标题含 JVM 关键词归 JVM，否则归并发 */
    private static final List<String> JVM_KEYWORDS = List.of(
            "jvm", "gc", "垃圾回收", "内存", "堆", "栈", "类加载", "双亲委派", "oom", "回收", "引用");

    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final QuestionMapper questionMapper;
    private final StatsService statsService;
    private final ReviewService reviewService;

    /** 解析 md 为预览数据（不落库）：场次信息 + 题目项（含复用匹配、覆盖勾选默认值） */
    public ImportParseResponse parse(String content) {
        MdInterviewParser.ParsedDoc doc = MdInterviewParser.parse(content);

        Map<String, Long> titleToId = questionMapper.selectList(
                        new LambdaQueryWrapper<Question>().select(Question::getId, Question::getTitle))
                .stream()
                .filter(q -> StringUtils.hasText(q.getTitle()))
                .collect(Collectors.toMap(Question::getTitle, Question::getId, (a, b) -> a));

        ImportInterviewInfo interview = new ImportInterviewInfo();
        interview.setCompany("模拟面试");
        interview.setPosition(StringUtils.hasText(doc.getPosition()) ? doc.getPosition() : "Java 后端");
        interview.setRound(doc.getRound());
        interview.setInterviewDate(doc.getInterviewDate());

        List<ImportItem> items = doc.getItems().stream().map(p -> {
            ImportItem item = new ImportItem();
            item.setTitle(p.getTitle());
            item.setCategory(mapCategory(p.getDocCategory(), p.getTitle()));
            item.setDifficulty(2);
            item.setScore(p.getScore());
            item.setAnswered(p.getAnswered() != null ? p.getAnswered() : 3);
            item.setDeduction(p.getDeduction());
            item.setMyAnswerSnapshot(p.getMyAnswerSnapshot());
            item.setStdAnswer(p.getStdAnswer());
            Long matched = titleToId.get(p.getTitle());
            item.setMatchedQuestionId(matched);
            // 复用且带参考答案时默认勾选覆盖（预置题答案普遍留空，正好填上）
            item.setOverwriteStdAnswer(matched != null && StringUtils.hasText(p.getStdAnswer()));
            return item;
        }).toList();
        int matchedCount = (int) items.stream().filter(i -> i.getMatchedQuestionId() != null).count();

        return new ImportParseResponse(interview, items, items.size(), matchedCount);
    }

    /** 确认导入：创建场次 + 题目复用/新建 + 题目记录，整体事务 */
    @Transactional
    public Long confirm(ImportConfirmRequest request) {
        ImportInterviewInfo info = request.getInterview();
        Interview interview = new Interview();
        interview.setCompany(info.getCompany());
        interview.setPosition(info.getPosition());
        interview.setRound(info.getRound());
        interview.setInterviewDate(info.getInterviewDate());
        interviewMapper.insert(interview);

        for (ImportItem item : request.getItems()) {
            QuestionCategory category = parseCategory(item.getCategory());
            Long questionId;
            Question matched = item.getMatchedQuestionId() == null
                    ? null
                    : questionMapper.selectById(item.getMatchedQuestionId());
            if (matched != null) {
                // 复用已有题：可选覆盖标准答案
                if (Boolean.TRUE.equals(item.getOverwriteStdAnswer()) && StringUtils.hasText(item.getStdAnswer())) {
                    matched.setStdAnswer(item.getStdAnswer());
                    questionMapper.updateById(matched);
                }
                questionId = matched.getId();
            } else {
                Question question = new Question();
                question.setTitle(item.getTitle());
                question.setCategory(category);
                question.setDifficulty(item.getDifficulty() != null ? item.getDifficulty() : 2);
                question.setStdAnswer(item.getStdAnswer());
                question.setSource(QuestionService.SOURCE_IMPORT);
                question.setReviewCount(0);
                questionMapper.insert(question);
                questionId = question.getId();
            }

            InterviewQuestion record = new InterviewQuestion();
            record.setInterviewId(interview.getId());
            record.setQuestionId(questionId);
            record.setQuestionSnapshot(item.getTitle());
            record.setScore(item.getScore());
            record.setAnswered(item.getAnswered());
            record.setDeduction(item.getDeduction());
            record.setMyAnswerSnapshot(item.getMyAnswerSnapshot());
            interviewQuestionMapper.insert(record);
            reviewService.autoEnqueue(questionId, item.getTitle(), item.getScore(), item.getAnswered());
        }
        statsService.invalidate();
        return interview.getId();
    }

    /** 文档分类 → 题库分类（映射失败返回 null，由预览强制选择） */
    static String mapCategory(String docCategory, String title) {
        if (!StringUtils.hasText(docCategory)) {
            return null;
        }
        return switch (docCategory.trim()) {
            case "Arch-Design" -> QuestionCategory.DISTRIBUTED.name();
            case "Database" -> QuestionCategory.MYSQL.name();
            case "Framework" -> QuestionCategory.SPRING.name();
            case "Behavior" -> QuestionCategory.BEHAVIOR.name();
            case "Lang-Runtime" -> looksLikeJvm(title) ? QuestionCategory.JVM.name() : QuestionCategory.CONCURRENCY.name();
            default -> null;
        };
    }

    private static boolean looksLikeJvm(String title) {
        if (title == null) {
            return false;
        }
        String t = title.toLowerCase();
        return JVM_KEYWORDS.stream().anyMatch(t::contains);
    }

    private QuestionCategory parseCategory(String code) {
        QuestionCategory category = QuestionCategory.fromCode(code);
        if (category == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法分类: " + code);
        }
        return category;
    }
}
