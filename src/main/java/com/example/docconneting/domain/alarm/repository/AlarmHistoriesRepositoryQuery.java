package com.example.docconneting.domain.alarm.repository;

import com.example.docconneting.domain.alarm.entity.AlarmHistories;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlarmHistoriesRepositoryQuery {

    Page<AlarmHistories> findAlarmHistories(Long userId, Pageable pageable);
}
