package com.example.docconneting.domain.alarm.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AlarmSenderService {

    /*
     * 다건 알람 전송
     */
    @Async("fcmExecutor")
    public void sendMulticastAlarm(List<String> fcmTokenBatche, String content) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle("Docconneting")
                            .setBody(content)
                            .build())
                    .addAllTokens(fcmTokenBatche)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            int successCount = response.getSuccessCount();
            int failureCount = response.getFailureCount();

            log.info("알림 전송 완료 - 성공횟수: {}, 실패횟수: {}", successCount, failureCount);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 단건 알람 전송
     */
    @Async("fcmExecutor")
    public void sendAlarm(String fcmToken, String content) {
        try {
            com.google.firebase.messaging.Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("Docconneting")
                            .setBody(content)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("알림 전송 완료 - 메시지 ID: {}", response);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
