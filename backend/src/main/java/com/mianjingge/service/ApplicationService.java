package com.mianjingge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianjingge.common.BusinessException;
import com.mianjingge.common.ErrorCode;
import com.mianjingge.dto.ApplicationSaveRequest;
import com.mianjingge.dto.ApplicationTransitionRequest;
import com.mianjingge.entity.Application;
import com.mianjingge.entity.ApplicationTimeline;
import com.mianjingge.enums.ApplicationStatus;
import com.mianjingge.mapper.ApplicationMapper;
import com.mianjingge.mapper.ApplicationTimelineMapper;
import com.mianjingge.mapper.InterviewMapper;
import com.mianjingge.vo.ApplicationTimelineVO;
import com.mianjingge.vo.ApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 投递看板：状态机 APPLIED→WRITTEN→FIRST→SECOND→THIRD→HR→OFFER 固定顺序推进，
 * 任意非终态可转 REJECTED；每次状态变更自动记时间线。
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final ApplicationTimelineMapper timelineMapper;
    private final InterviewMapper interviewMapper;

    /** 看板全量列表（前端按状态分列），按更新时间倒序 */
    public List<ApplicationVO> list() {
        return applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                        .orderByDesc(Application::getUpdatedAt)
                        .orderByDesc(Application::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public ApplicationVO create(ApplicationSaveRequest request) {
        Application app = new Application();
        apply(app, request);
        app.setStatus(ApplicationStatus.APPLIED);
        applicationMapper.insert(app);
        insertTimeline(app.getId(), null, ApplicationStatus.APPLIED, "创建投递");
        return toVO(app);
    }

    /** 编辑基本信息（不动状态与时间线） */
    public ApplicationVO update(Long id, ApplicationSaveRequest request) {
        Application app = requireApplication(id);
        apply(app, request);
        applicationMapper.updateById(app);
        return toVO(app);
    }

    /** 删除投递：级联删除时间线 */
    public void delete(Long id) {
        requireApplication(id);
        timelineMapper.delete(new LambdaQueryWrapper<ApplicationTimeline>()
                .eq(ApplicationTimeline::getApplicationId, id));
        applicationMapper.deleteById(id);
    }

    /** 状态流转：只允许按主流程推进一步或转 REJECTED，可关联面试场次 */
    public ApplicationVO transition(Long id, ApplicationTransitionRequest request) {
        Application app = requireApplication(id);
        ApplicationStatus from = app.getStatus();
        ApplicationStatus to = ApplicationStatus.fromCode(request.getToStatus());
        if (to == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法状态: " + request.getToStatus());
        }
        if (from.isTerminal()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该投递已结束（" + from.getLabel() + "），不可再流转");
        }
        if (to == from) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态未变化");
        }
        if (to != ApplicationStatus.REJECTED && to != from.next()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "只能推进到「" + from.next().getLabel() + "」或标记「" + ApplicationStatus.REJECTED.getLabel() + "」");
        }
        if (request.getInterviewId() != null) {
            if (interviewMapper.selectById(request.getInterviewId()) == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "关联的面试场次不存在");
            }
            app.setInterviewId(request.getInterviewId());
        }
        app.setStatus(to);
        applicationMapper.updateById(app);
        insertTimeline(app.getId(), from, to, request.getRemark());
        return toVO(app);
    }

    /** 该投递的状态时间线（按发生顺序） */
    public List<ApplicationTimelineVO> timeline(Long id) {
        requireApplication(id);
        return timelineMapper.selectList(new LambdaQueryWrapper<ApplicationTimeline>()
                        .eq(ApplicationTimeline::getApplicationId, id)
                        .orderByAsc(ApplicationTimeline::getId))
                .stream()
                .map(t -> {
                    ApplicationTimelineVO vo = new ApplicationTimelineVO();
                    vo.setId(t.getId());
                    vo.setFromStatus(t.getFromStatus());
                    vo.setFromStatusLabel(t.getFromStatus() == null ? null : t.getFromStatus().getLabel());
                    vo.setToStatus(t.getToStatus());
                    vo.setToStatusLabel(t.getToStatus().getLabel());
                    vo.setRemark(t.getRemark());
                    vo.setCreatedAt(t.getCreatedAt());
                    return vo;
                })
                .toList();
    }

    // ---------- 私有工具 ----------

    private void apply(Application app, ApplicationSaveRequest request) {
        app.setCompany(request.getCompany());
        app.setPosition(request.getPosition());
        app.setChannel(request.getChannel());
        app.setApplyDate(request.getApplyDate());
        app.setCity(request.getCity());
        app.setSalary(request.getSalary());
        app.setJdUrl(request.getJdUrl());
        app.setRemark(request.getRemark());
    }

    private void insertTimeline(Long applicationId, ApplicationStatus from, ApplicationStatus to, String remark) {
        ApplicationTimeline timeline = new ApplicationTimeline();
        timeline.setApplicationId(applicationId);
        timeline.setFromStatus(from);
        timeline.setToStatus(to);
        timeline.setRemark(remark);
        timelineMapper.insert(timeline);
    }

    private Application requireApplication(Long id) {
        Application app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "投递记录不存在");
        }
        return app;
    }

    private ApplicationVO toVO(Application app) {
        ApplicationVO vo = new ApplicationVO();
        vo.setId(app.getId());
        vo.setCompany(app.getCompany());
        vo.setPosition(app.getPosition());
        vo.setChannel(app.getChannel());
        vo.setApplyDate(app.getApplyDate());
        vo.setCity(app.getCity());
        vo.setSalary(app.getSalary());
        vo.setJdUrl(app.getJdUrl());
        vo.setStatus(app.getStatus());
        vo.setStatusLabel(app.getStatus() == null ? null : app.getStatus().getLabel());
        vo.setInterviewId(app.getInterviewId());
        vo.setRemark(app.getRemark());
        vo.setCreatedAt(app.getCreatedAt());
        vo.setUpdatedAt(app.getUpdatedAt());
        return vo;
    }
}
