package com.example.docconneting.domain.payment.service;

import com.example.docconneting.common.config.TestAsyncConfig;
import com.example.docconneting.common.exception.constant.ErrorCode;
import com.example.docconneting.common.exception.object.ClientException;
import com.example.docconneting.domain.alarm.service.AlarmService;
import com.example.docconneting.domain.auth.entity.AuthUser;
import com.example.docconneting.domain.order.dto.request.OrderRequest;
import com.example.docconneting.domain.order.enums.OrderProduct;
import com.example.docconneting.domain.order.enums.OrderType;
import com.example.docconneting.domain.order.repository.OrderRepository;
import com.example.docconneting.domain.payment.dto.request.PaymentVerificationRequest;
import com.example.docconneting.domain.user.entity.User;
import com.example.docconneting.domain.user.enums.UserRole;
import com.example.docconneting.domain.user.repository.UserRepository;
import com.siot.IamportRestClient.response.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestAsyncConfig.class)
public class VerifyAndCreateOrderDistributedLockTest {

    @Autowired
    private PaymentApplicationService paymentApplicationService;

    @MockitoBean
    private PortOneService portOneService;

    @MockitoBean
    private AlarmService alarmService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private AuthUser authUser;

    private String merchantId;
    private String impUid;

    @BeforeEach
    void setUp() throws Exception {
        this.impUid = "imp_test_" + System.currentTimeMillis();
        this.merchantId = "kakao_test_" + System.currentTimeMillis();

        User user1 = User.of("test@test.com", "test123!", "테스트", 0, false, UserRole.PATIENT);
        userRepository.save(user1);
        authUser = AuthUser.of(user1.getId(), user1.getUserRole());

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPgProvider()).willReturn("kakao");
        given(mockPayment.getPayMethod()).willReturn("card");
        given(mockPayment.getPaidAt()).willReturn(new java.util.Date(1713776400L * 1000L));
        given(mockPayment.getStatus()).willReturn("paid");
        given(mockPayment.getAmount()).willReturn(new BigDecimal(3000));
        given(mockPayment.getImpUid()).willReturn(impUid);

        given(portOneService.getPayment(anyString())).willReturn(mockPayment);
    }

    @Test
    @DisplayName("동일 merchantId로 동시에 주문 생성 요청 시 하나만 성공 테스트")
    void distributedLockTest() throws InterruptedException {
        OrderRequest orderRequest = new OrderRequest();
        ReflectionTestUtils.setField(orderRequest, "orderType", OrderType.CHAT);
        ReflectionTestUtils.setField(orderRequest, "orderProduct", OrderProduct.CHAT_3000);
        ReflectionTestUtils.setField(orderRequest, "price", 3000);
        ReflectionTestUtils.setField(orderRequest, "doctorId", 2L);

        PaymentVerificationRequest verificationRequest = new PaymentVerificationRequest();
        ReflectionTestUtils.setField(verificationRequest, "impUid", impUid);
        ReflectionTestUtils.setField(verificationRequest, "merchantId", merchantId);
        ReflectionTestUtils.setField(verificationRequest, "userId", authUser.getId());
        ReflectionTestUtils.setField(verificationRequest, "orderRequest", orderRequest);

        User user2 = User.of("test@test.com", "test123!", "테스트", 0, false, UserRole.DOCTOR);
        userRepository.save(user2);

        int threadCount = 5; // 동시에 실행될 작업(스레드)
        ExecutorService executor = Executors.newFixedThreadPool(threadCount); // 멀티스레드 작업 도구
        CountDownLatch countDownLatch = new CountDownLatch(threadCount); // 스레드 동기화 위한 도구

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentApplicationService.verifyAndCreateOrder(verificationRequest);
                    successCount.incrementAndGet();
                } catch (ClientException e) {
                    failureCount.incrementAndGet();
                    if (ErrorCode.LOCK_ACQUISITION_FAILED.equals(e.getErrorCode())) {
                        e.printStackTrace(); // 락 실패 로그
                    } else {
                        System.err.println("[ClientException] " + e.getMessage());
                        e.printStackTrace(); // 그 외 클라이언트 예외
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("[기타 Exception 발생] " + e.getClass().getSimpleName() + " : " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        executor.shutdown();

        System.out.println("최종 성공 수: " + successCount.get());
        System.out.println("최종 실패 수: " + failureCount.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
    }
}
