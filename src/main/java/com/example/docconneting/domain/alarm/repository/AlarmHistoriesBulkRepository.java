package com.example.docconneting.domain.alarm.repository;

import com.example.docconneting.domain.alarm.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AlarmHistoriesBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    public int[][] batchUpdate(List<Long> userIdList, AlarmType alarmType, String content) {
        int [][] insertCount = jdbcTemplate.batchUpdate(
                "INSERT INTO alarm_histories(content, to_id, alarm_type, created_at)" + "VALUES (?, ?, ?, ?)",
                userIdList,
                100,
                (PreparedStatement ps, Long userId) -> {
                    ps.setString(1, content);
                    ps.setString(2, String.valueOf(userId));
                    ps.setString(3, String.valueOf(alarmType));
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                });
        return insertCount;
    }

}
