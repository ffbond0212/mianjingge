package com.mianjingge.service;

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
import com.mianjingge.vo.ApplicationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 状态机流转规则与时间线记录的单元测试 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private ApplicationTimelineMapper timelineMapper;
    @Mock
    private InterviewMapper interviewMapper;

    @InjectMocks
    private ApplicationService applicationService;

    private Application application(ApplicationStatus status) {
        Application app = new Application();
        app.setId(1L);
        app.setCompany("测试公司");
        app.setPosition("Java");
        app.setStatus(status);
        return app;
    }

    private ApplicationTransitionRequest transition(String toStatus) {
        ApplicationTransitionRequest request = new ApplicationTransitionRequest();
        request.setToStatus(toStatus);
        return request;
    }

    @Test
    void createStartsAtAppliedAndRecordsTimeline() {
        ApplicationSaveRequest request = new ApplicationSaveRequest();
        request.setCompany("测试公司");
        request.setPosition("Java");

        applicationService.create(request);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationMapper).insert(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.APPLIED);

        ArgumentCaptor<ApplicationTimeline> timelineCaptor = ArgumentCaptor.forClass(ApplicationTimeline.class);
        verify(timelineMapper).insert(timelineCaptor.capture());
        assertThat(timelineCaptor.getValue().getFromStatus()).isNull();
        assertThat(timelineCaptor.getValue().getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void transitionRejectsSkippingStages() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.APPLIED));

        assertThatThrownBy(() -> applicationService.transition(1L, transition("FIRST")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能推进到「笔试」");
        verify(applicationMapper, never()).updateById(any(Application.class));
    }

    @Test
    void transitionAdvancesOneStepAndRecordsTimeline() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.APPLIED));
        ApplicationTransitionRequest request = transition("WRITTEN");
        request.setRemark("笔试已约");

        applicationService.transition(1L, request);

        ArgumentCaptor<Application> appCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationMapper).updateById(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.WRITTEN);

        ArgumentCaptor<ApplicationTimeline> timelineCaptor = ArgumentCaptor.forClass(ApplicationTimeline.class);
        verify(timelineMapper).insert(timelineCaptor.capture());
        assertThat(timelineCaptor.getValue().getFromStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(timelineCaptor.getValue().getToStatus()).isEqualTo(ApplicationStatus.WRITTEN);
        assertThat(timelineCaptor.getValue().getRemark()).isEqualTo("笔试已约");
    }

    @Test
    void transitionAllowsRejectedFromAnyNonTerminalState() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.SECOND));

        ApplicationVO vo = applicationService.transition(1L, transition("REJECTED"));
        assertThat(vo.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void transitionRejectsMovingFromTerminalState() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.REJECTED));

        assertThatThrownBy(() -> applicationService.transition(1L, transition("FIRST")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已结束");
    }

    @Test
    void transitionRejectsSameStatusAndUnknownStatus() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.APPLIED));

        assertThatThrownBy(() -> applicationService.transition(1L, transition("APPLIED")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态未变化");

        assertThatThrownBy(() -> applicationService.transition(1L, transition("UNKNOWN")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法状态");
    }

    @Test
    void transitionValidatesLinkedInterview() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.APPLIED));
        when(interviewMapper.selectById(999L)).thenReturn(null);

        ApplicationTransitionRequest request = transition("WRITTEN");
        request.setInterviewId(999L);
        assertThatThrownBy(() -> applicationService.transition(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void deleteRemovesTimelineFirst() {
        when(applicationMapper.selectById(1L)).thenReturn(application(ApplicationStatus.APPLIED));

        applicationService.delete(1L);

        verify(timelineMapper).delete(any());
        verify(applicationMapper).deleteById(1L);
    }
}
