package com.example.docconneting.domain.alarm.repository;

import com.example.docconneting.domain.alarm.entity.AlarmHistories;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.example.docconneting.domain.alarm.entity.QAlarmHistories.alarmHistories;

@RequiredArgsConstructor
public class AlarmHistoriesRepositoryQueryImpl implements AlarmHistoriesRepositoryQuery {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<AlarmHistories> findAlarmHistories(Long userId, Pageable pageable) {
        List<AlarmHistories> alarmHistoyList = jpaQueryFactory
                .select(alarmHistories)
                .from(alarmHistories)
                .where(alarmHistories.toId.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(alarmHistories.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(alarmHistories.count())
                .from(alarmHistories);

        return PageableExecutionUtils.getPage(alarmHistoyList, pageable, countQuery::fetchOne);
    }
}
