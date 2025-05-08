package com.example.docconneting.domain.alarm.service;

import com.example.docconneting.common.enums.Major;
import com.example.docconneting.common.response.PageInfo;
import com.example.docconneting.common.response.PageResult;
import com.example.docconneting.domain.alarm.dto.AlarmResponse;
import com.example.docconneting.domain.alarm.entity.AlarmHistories;
import com.example.docconneting.domain.alarm.enums.AlarmType;
import com.example.docconneting.domain.alarm.repository.AlarmHistoriesBulkRepository;
import com.example.docconneting.domain.alarm.repository.AlarmHistoriesRepository;
import com.example.docconneting.domain.auth.entity.AuthUser;
import com.example.docconneting.domain.user.entity.User;
import com.example.docconneting.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmSenderService alarmSenderService;
    private final UserRepository userRepository;
    private final AlarmHistoriesRepository alarmHistoriesRepository;
    private final AlarmHistoriesBulkRepository alarmHistoriesBulkRepository;

    /*
     * 알람 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResult<AlarmResponse> findAlarms(AuthUser authUser, Pageable pageable) {
        Page<AlarmHistories> result = alarmHistoriesRepository.findAlarmHistories(authUser.getId(), pageable);
        List<AlarmResponse> alarms = AlarmResponse.toAlarmResponse(result.getContent());

        PageInfo pageInfo = PageInfo.builder()
                .pageNum(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElement(result.getTotalElements())
                .totalPage(result.getTotalPages())
                .build();

        return new PageResult<>(alarms, pageInfo);
    }

    /*
     * 사용자가 유료 게시물을 올렸을 떄 해당 전공에 해당되는 의사들에게 알람 전송
     */
    @Transactional
    public void sendPostUploadCompletedMessage(Major major) {
        List<User> users = userRepository.findByMajor(major);
        List<String> fcmTokenList = users.stream()
                .map(User::getFcmToken)
                .toList();

        List<Long> userIdList = users.stream()
                .map(User::getId)
                .toList();

        AlarmType alarmType = AlarmType.POST_UPLOAD;
        String alarmMessage = "새로운 유료 질문이 들어왔습니다.";


        List<List<String>> fcmTokenBatches = new ArrayList<>();
        for (int i = 0; i < fcmTokenList.size(); i += 100) {
            fcmTokenBatches.add(fcmTokenList.subList(i, Math.min(fcmTokenList.size(), i + 100)));
        }
        for (List<String> fcmTokenBatche : fcmTokenBatches) {
            alarmSenderService.sendMulticastAlarm(fcmTokenBatche, alarmMessage);
        }
        alarmHistoriesBulkRepository.batchUpdate(userIdList, alarmType, alarmMessage);
    }

    /*
     * 게시물에 의사가 댓글을 달았을 때 게시물 작성자에게 알람 전송
     */
    @Transactional
    public void sendCommentCompletedMessage(User user) {
        String fcmToken = user.getFcmToken();
        Long userId = user.getId();
        AlarmType alarmType = AlarmType.COMMENT;
        String alarmMessage = "회원님의 게시물에 의사가 댓글을 달았습니다.";

        alarmSenderService.sendAlarm(fcmToken, alarmMessage);
        saveAlarmHistories(alarmMessage, userId, alarmType);
    }

    /*
     * 채팅 진료 결제가 완료 됐을 때 해당 의사에게 알람 전송
     */
    @Transactional
    public void sendMedicalRequestMessage(User patient, User doctor) {
        String fcmToken = doctor.getFcmToken();
        Long userId = doctor.getId();
        String alarmMessage = patient.getUsername() + "님이 채팅 진료를 요청 했습니다";

        alarmSenderService.sendAlarm(fcmToken, alarmMessage);
        saveAlarmHistories(alarmMessage, userId, AlarmType.MEDICAL_REQUEST);
    }

    /*
     * 단건 알람 히스토리 저장
     */
    private void saveAlarmHistories(String content, Long id, AlarmType alarmType) {
        AlarmHistories alarmHistories = AlarmHistories.of(content, id, alarmType);
        alarmHistoriesRepository.save(alarmHistories);
    }
}
