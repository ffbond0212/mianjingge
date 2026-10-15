package com.mianjingge.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    void mainFlowAdvancesOneStepAtATime() {
        assertThat(ApplicationStatus.APPLIED.next()).isEqualTo(ApplicationStatus.WRITTEN);
        assertThat(ApplicationStatus.WRITTEN.next()).isEqualTo(ApplicationStatus.FIRST);
        assertThat(ApplicationStatus.FIRST.next()).isEqualTo(ApplicationStatus.SECOND);
        assertThat(ApplicationStatus.SECOND.next()).isEqualTo(ApplicationStatus.THIRD);
        assertThat(ApplicationStatus.THIRD.next()).isEqualTo(ApplicationStatus.HR);
        assertThat(ApplicationStatus.HR.next()).isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    void terminalStatesHaveNoNextAndAreTerminal() {
        assertThat(ApplicationStatus.OFFER.next()).isNull();
        assertThat(ApplicationStatus.REJECTED.next()).isNull();
        assertThat(ApplicationStatus.OFFER.isTerminal()).isTrue();
        assertThat(ApplicationStatus.REJECTED.isTerminal()).isTrue();
        assertThat(ApplicationStatus.APPLIED.isTerminal()).isFalse();
        assertThat(ApplicationStatus.HR.isTerminal()).isFalse();
    }

    @Test
    void fromCodeIsCaseInsensitiveAndNullForUnknown() {
        assertThat(ApplicationStatus.fromCode("applied")).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(ApplicationStatus.fromCode("FIRST")).isEqualTo(ApplicationStatus.FIRST);
        assertThat(ApplicationStatus.fromCode("UNKNOWN")).isNull();
        assertThat(ApplicationStatus.fromCode(null)).isNull();
    }
}
