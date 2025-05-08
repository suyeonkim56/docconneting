package com.example.docconneting.domain.payment.service;

import com.example.docconneting.common.config.annotation.DistributedLock;
import com.example.docconneting.common.exception.constant.ErrorCode;
import com.example.docconneting.common.exception.object.ClientException;
import com.example.docconneting.domain.alarm.service.AlarmService;
import com.example.docconneting.domain.auth.entity.AuthUser;
import com.example.docconneting.domain.chatting.dto.response.ChattingRoomCreateResponse;
import com.example.docconneting.domain.chatting.service.ChattingRoomService;
import com.example.docconneting.domain.order.dto.response.OrderResponse;
import com.example.docconneting.domain.order.entity.Order;
import com.example.docconneting.domain.order.repository.OrderRepository;
import com.example.docconneting.domain.order.service.ChattingRoomAsyncService;
import com.example.docconneting.domain.order.service.OrderService;
import com.example.docconneting.domain.payment.dto.request.PaymentVerificationRequest;
import com.example.docconneting.domain.payment.dto.request.PaymentWebhookRequest;
import com.example.docconneting.domain.payment.dto.response.PortOnePaymentResponse;
import com.example.docconneting.domain.payment.enums.PaymentMethod;
import com.example.docconneting.domain.payment.enums.PaymentStatus;
import com.example.docconneting.domain.user.entity.User;
import com.example.docconneting.domain.user.enums.UserRole;
import com.example.docconneting.domain.user.repository.UserRepository;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentService paymentService;
    private final PortOneService portOneService;
    private final OrderService orderService;
    private final ChattingRoomService chattingRoomService;
    private final AlarmService alarmService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ChattingRoomAsyncService chattingRoomAsyncService;

    @DistributedLock(value = "#request.merchantId")
    @Transactional
    public OrderResponse verifyAndCreateOrder(PaymentVerificationRequest request) {
        try {
            // PG 결제 검증
            Payment payment = portOneService.getPayment(request.getImpUid());
            log.info("PG 결제 정보 조회 완료 | impUid={}, amount={}, status={}",
                    payment.getImpUid(), payment.getAmount(), payment.getStatus());

            int paidAmount = payment.getAmount().intValue();
            int expectedAmount = request.getOrderRequest().getPrice();

            if (paidAmount != expectedAmount) {
                log.warn("결제 금액 불일치 | paidAmount={}, expectedAmount={}", paidAmount, expectedAmount);
                throw new ClientException(ErrorCode.INVALID_PAYMENT_AMOUNT);
            }

            if (!"paid".equals(payment.getStatus())) {
                log.warn("결제 미완료 상태 | status={}", payment.getStatus());
                throw new ClientException(ErrorCode.PAYMENT_NOT_COMPLETED);
            }

            if (orderRepository.existsByMerchantUid(request.getMerchantId())) {
                log.warn("이미 주문이 존재함 | merchantId={}", request.getMerchantId());
                throw new ClientException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            // 주문 생성
            AuthUser authUser = AuthUser.of(request.getUserId(), UserRole.PATIENT);
            log.info("주문 생성 시작 | userId={}, merchantId={}", request.getUserId(), request.getMerchantId());
            Order order = orderService.createOrder(request.getOrderRequest(), request.getMerchantId(), authUser);
            log.info("주문 생성 완료 | orderId={}", order.getId());

            // 결제 완료 처리
            PaymentMethod paymentMethod = PaymentMethod.of(payment.getPgProvider(), payment.getPayMethod());
            LocalDateTime approvedAt = payment.getPaidAt().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            paymentService.completePayment(order, payment.getImpUid(), paymentMethod, approvedAt);
            log.info("결제 완료 처리 완료 | orderId={}, paymentMethod={}, approvedAt={}",
                    order.getId(), paymentMethod, approvedAt);

            // 채팅 주문 후처리 (비동기)
            if (order.isChatOrder()) {
                log.info("채팅 주문으로 채팅방 생성 시도 | doctorId={}", order.getDoctorId());
                ChattingRoomCreateResponse response = chattingRoomService.createdChattingRoom(authUser, order.getDoctorId());
                order.assignChattingRoomId(response.getId());
                log.info("채팅방 생성 완료 | chattingRoomId={}", response.getId());

                // 채팅 생성이 완료되면 의사에게 알람 전송
                User patient = userRepository.findById(authUser.getId()).orElseThrow(() -> new ClientException(ErrorCode.USER_NOT_FOUND));
                User doctor = userRepository.findById(order.getDoctorId()).orElseThrow(() -> new ClientException(ErrorCode.DOCTOR_NOT_FOUND));
                alarmService.sendMedicalRequestMessage(patient, doctor);
                log.info("채팅 주문으로 채팅방 비동기 생성 시도 | doctorId={}", order.getDoctorId());
                chattingRoomAsyncService.createChattingRoom(order);
            }

            log.info("[END] verifyAndCreateOrder 완료 | orderId={}", order.getId());

            return OrderResponse.of(
                    order.getId(),
                    order.getOrderType(),
                    order.getOrderStatus(),
                    order.getPaymentStatus(),
                    order.getPaymentMethod(),
                    order.getOrderProduct(),
                    order.getPrice(),
                    order.getChattingRoomId(),
                    order.getApprovedAt()
            );

        } catch (IamportResponseException | IOException e) {
            log.error("PG 서버 에러 발생 | merchantId={}", request.getMerchantId(), e);
            throw new ClientException(ErrorCode.PAYMENT_SERVER_ERROR);
        }
    }

    @Transactional
    public PortOnePaymentResponse handleWebhook(PaymentWebhookRequest request) throws IamportResponseException, IOException {
        Order order = orderRepository.findByMerchantUid(request.getMerchantUid())
                .orElseThrow(() -> new ClientException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = portOneService.getPayment(request.getImpUid());

        PaymentMethod paymentMethod = PaymentMethod.of(payment.getPgProvider(), payment.getPayMethod());
        LocalDateTime approvedAt = payment.getPaidAt().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();

        if (PaymentStatus.COMPLETED.equals(PaymentStatus.of(request.getPaymentStatus()))) {
            if (!order.isCompleted()) {
                paymentService.completePayment(order, payment.getImpUid(), paymentMethod, approvedAt);
            } else {
                log.info("이미 완료된 주문입니다: {}", order.getId());
            }
        } else {
            paymentService.failPayment(order);
        }

        return PortOnePaymentResponse.of(
                order.getImpUid(),
                order.getMerchantUid(),
                order.getPrice(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getApprovedAt()
        );
    }
}

