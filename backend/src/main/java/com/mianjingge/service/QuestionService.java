package com.mianjingge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mianjingge.common.BusinessException;
import com.mianjingge.common.ErrorCode;
import com.mianjingge.common.PageResult;
import com.mianjingge.dto.QuestionQueryRequest;
import com.mianjingge.dto.QuestionSaveRequest;
import com.mianjingge.entity.Question;
import com.mianjingge.enums.QuestionCategory;
import com.mianjingge.mapper.QuestionMapper;
import com.mianjingge.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    /** 题目来源常量 */
    public static final String SOURCE_PRESET = "PRESET";
    public static final String SOURCE_IMPORT = "IMPORT";
    public static final String SOURCE_MANUAL = "MANUAL";

    private final QuestionMapper questionMapper;
    private final StatsService statsService;

    /** 分页查询：关键词模糊匹配 标题/标准答案/个人话术 */
    public PageResult<QuestionVO> page(QuestionQueryRequest request) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        String keyword = request.getKeyword();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Question::getTitle, keyword)
                    .or().like(Question::getStdAnswer, keyword)
                    .or().like(Question::getMyAnswer, keyword));
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Question::getCategory, parseCategory(request.getCategory()));
        }
        if (StringUtils.hasText(request.getSource())) {
            wrapper.eq(Question::getSource, request.getSource());
        }
        if (request.getDifficulty() != null) {
            wrapper.eq(Question::getDifficulty, request.getDifficulty());
        }
        wrapper.orderByDesc(Question::getId);

        Page<Question> page = questionMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        List<QuestionVO> records = page.getRecords().stream().map(QuestionVO::from).toList();
        return PageResult.of(page.getTotal(), records);
    }

    public QuestionVO getById(Long id) {
        return QuestionVO.from(requireQuestion(id));
    }

    public QuestionVO create(QuestionSaveRequest request) {
        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setCategory(parseCategory(request.getCategory()));
        question.setTags(request.getTags());
        question.setDifficulty(request.getDifficulty());
        question.setStdAnswer(request.getStdAnswer());
        question.setMyAnswer(request.getMyAnswer());
        question.setSource(SOURCE_MANUAL);
        question.setReviewCount(0);
        questionMapper.insert(question);
        statsService.invalidate();
        return QuestionVO.from(question);
    }

    public QuestionVO update(Long id, QuestionSaveRequest request) {
        Question question = requireQuestion(id);
        question.setTitle(request.getTitle());
        question.setCategory(parseCategory(request.getCategory()));
        question.setTags(request.getTags());
        question.setDifficulty(request.getDifficulty());
        question.setStdAnswer(request.getStdAnswer());
        question.setMyAnswer(request.getMyAnswer());
        questionMapper.updateById(question);
        statsService.invalidate();
        return QuestionVO.from(question);
    }

    public void delete(Long id) {
        requireQuestion(id);
        questionMapper.deleteById(id);
        statsService.invalidate();
    }

    private Question requireQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    /** 校验分类码，非法则抛参数错误 */
    private QuestionCategory parseCategory(String code) {
        QuestionCategory category = QuestionCategory.fromCode(code);
        if (category == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法分类: " + code);
        }
        return category;
    }
}
