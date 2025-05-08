package com.example.docconneting.domain.alarm.repository;

import com.example.docconneting.domain.alarm.entity.AlarmHistories;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmHistoriesRepository extends JpaRepository<AlarmHistories, Long>, AlarmHistoriesRepositoryQuery {
}
