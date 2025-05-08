package com.example.docconneting.domain.alarm.service;

import com.example.docconneting.common.response.PageResult;
import com.example.docconneting.domain.alarm.dto.AlarmResponse;
import com.example.docconneting.domain.alarm.entity.AlarmHistories;
import com.example.docconneting.domain.alarm.enums.AlarmType;
import com.example.docconneting.domain.alarm.repository.AlarmHistoriesRepository;
import com.example.docconneting.domain.auth.entity.AuthUser;
import com.example.docconneting.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock
    private AlarmHistoriesRepository alarmHistoriesRepository;

    @InjectMocks
    private AlarmService alarmService;

    private AlarmHistories histories1;
    private AlarmHistories histories2;

    @BeforeEach
    void setUp() {
        histories1 = AlarmHistories.of(
                "회원님의 글에 댓글이 달렸습니다",
                1L,
                AlarmType.COMMENT
        );
        ReflectionTestUtils.setField(histories1, "id", 1L);

        histories2 = AlarmHistories.of(
                "회원님의 글에 댓글이 달렸습니다",
                1L,
                AlarmType.COMMENT
        );
        ReflectionTestUtils.setField(histories2, "id", 2L);
    }

    @Test
    public void 알람_목록을_페이징을_적용하여_조회할_수_있다() {
        // given
        Long userId = 1L;
        UserRole userRole = UserRole.PATIENT;
        AuthUser authUser = AuthUser.of(userId, userRole);
        int page = 0;
        int size = 10;

        Pageable pageable = PageRequest.of(page, size);
        List<AlarmHistories> alarmHistories = new ArrayList<>();
        alarmHistories.add(histories1);
        alarmHistories.add(histories2);
        Page<AlarmHistories> alarmPage = new PageImpl<>(alarmHistories, pageable, alarmHistories.size());

        given(alarmHistoriesRepository.findAlarmHistories(userId, pageable)).willReturn(alarmPage);

        // when
        PageResult<AlarmResponse> result = alarmService.findAlarms(authUser, pageable);

        // then
        assertThat(result.getContent()).hasSize(alarmHistories.size());
        assertThat(result.getPageInfo().getPageNum()).isEqualTo(page);
        assertThat(result.getPageInfo().getPageSize()).isEqualTo(size);
        assertThat(result.getPageInfo().getTotalElement()).isEqualTo(alarmHistories.size());
        assertThat(result.getPageInfo().getTotalPage()).isEqualTo(1);
    }

}
