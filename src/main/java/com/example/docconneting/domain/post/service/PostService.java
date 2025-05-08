package com.example.docconneting.domain.post.service;

import com.example.docconneting.common.enums.Major;
import com.example.docconneting.common.exception.constant.ErrorCode;
import com.example.docconneting.common.exception.object.ClientException;
import com.example.docconneting.common.response.PageInfo;
import com.example.docconneting.common.response.PageResult;
import com.example.docconneting.domain.alarm.service.AlarmService;
import com.example.docconneting.domain.auth.entity.AuthUser;
import com.example.docconneting.domain.coupon.entity.PatientCoupon;
import com.example.docconneting.domain.coupon.service.PatientCouponService;
import com.example.docconneting.domain.point.service.PointService;
import com.example.docconneting.domain.post.dto.reponse.PostCreateResponse;
import com.example.docconneting.domain.post.dto.reponse.PostListResponse;
import com.example.docconneting.domain.post.dto.reponse.PostSingleResponse;
import com.example.docconneting.domain.post.dto.reponse.PostUpdateResponse;
import com.example.docconneting.domain.post.dto.request.PostCreateRequest;
import com.example.docconneting.domain.post.dto.request.PostUpdateRequest;
import com.example.docconneting.domain.post.entity.Post;
import com.example.docconneting.domain.post.enums.PayType;
import com.example.docconneting.domain.post.repository.PostRepository;
import com.example.docconneting.domain.user.entity.User;
import com.example.docconneting.domain.user.enums.UserRole;
import com.example.docconneting.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PointService pointService;
    private final PatientCouponService patientCouponService;
    private final AlarmService alarmService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final EntityManager entityManager;

    // 게시글 등록
    @Transactional
    public PostCreateResponse createPost(AuthUser authUser, Long couponId, PostCreateRequest request) {

        User user = userRepository.findUserByIdAndUserRole(authUser.getId(), UserRole.PATIENT)
                .orElseThrow(() -> new ClientException(ErrorCode.USER_NOT_FOUND));

        PayType payType = PayType.of(request.getPayType());
        Major major = Major.of(request.getMajor());

        Post post = Post.of(
                user,
                request.getTitle(),
                request.getContents(),
                major,
                false,
                false);

        switch (payType) {
            case COUPON -> {
                if (couponId == null) {
                    throw new ClientException(ErrorCode.MISSING_COUPON_ID);
                }

                post.updatePayType(PayType.COUPON);
                Post savedPost = postRepository.save(post);

                patientCouponService.useCoupon(user, couponId, savedPost.getId());

                // 유료 게시물 등록 완료 후, 의사들에게 알람 전송
                alarmService.sendPostUploadCompletedMessage(major);
            }

            case POINT -> {
                post.updatePayType(PayType.POINT);
                Post savedPost = postRepository.save(post);

                pointService.usePoint(user.getId(), savedPost.getId());

                // 유료 게시물 등록 완료 후, 의사들에게 알람 전송
                alarmService.sendPostUploadCompletedMessage(major);
                pointService.usePoint(user.getId(), savedPost.getId());
            }

            case FREE -> {
                post.changePayTypeAndDeadline();
                postRepository.save(post);
            }

            default -> throw new ClientException(ErrorCode.INVALID_PAY_TYPE);
        }

        return PostCreateResponse.of(
                    post.getId(),
                    post.getTitle(),
                    post.getContents(),
                    post.getMajor().toString(),
                    post.getCreatedAt());
    }

    // 게시물 단건 조회
    @Transactional(readOnly = true)
    public PostSingleResponse findPostById(Long postId){
        Post findPost = postRepository.findByIdWithUser(postId).orElseThrow(() -> new ClientException(ErrorCode.NOT_FOUND_POST));

        if (findPost.getIsDeleted()){
            throw new ClientException(ErrorCode.NOT_FOUND_POST);
        }

        return PostSingleResponse.of(
                findPost.getId(),
                findPost.getPatient().getUsername(),
                findPost.getTitle(),
                findPost.getContents(),
                findPost.getMajor().name(),
                findPost.getIsReplied(),
                findPost.getDeadline(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt());
    }

    @Transactional(readOnly = true)
    public PageResult<PostListResponse> findAllPosts(Pageable pageable, String title, String major){

        Page<Post> posts = postRepository.findPosts(pageable, title, major);

        List<Post> content = posts.getContent();
        Pageable postsPageable = posts.getPageable();

        List<PostListResponse> postsListResponses = PostListResponse.toPostListResponses(content);

        PageInfo pageInfo = PageInfo.builder()
                .pageNum(postsPageable.getPageNumber())
                .pageSize(postsPageable.getPageSize())
                .totalElement(posts.getTotalElements())
                .totalPage( posts.getTotalPages())
                .build();

        return new PageResult<>(postsListResponses, pageInfo);
    }

    // 게시물 수정
    @Transactional
    public PostUpdateResponse updatePost(AuthUser authUser, Long postId, PostUpdateRequest postUpdateRequest){

        if (authUser.getUserRole() != UserRole.PATIENT){
            throw new ClientException(ErrorCode.PATIENT_ONLY_ACCESS);
        }

        Post findPost = postRepository.findByIdWithUser(postId).orElseThrow(() -> new ClientException(ErrorCode.NOT_FOUND_POST));

        if (findPost.getIsDeleted()){
            throw new ClientException(ErrorCode.NOT_FOUND_POST);
        }

        if (authUser.getId() != findPost.getPatient().getId()){
            throw new ClientException(ErrorCode.ONLY_AUTHOR_CAN_UPDATE_OR_DELETED);
        }

        findPost.updateTitle(postUpdateRequest.getTitle());
        findPost.updateContents(postUpdateRequest.getContents());

        // flush() 시점에 @LastModifiedDate 이 작동한다
        entityManager.flush();

        return PostUpdateResponse.of(
                findPost.getId(),
                findPost.getTitle(),
                findPost.getContents(),
                findPost.getMajor().name(),
                findPost.getCreatedAt(),
                findPost.getModifiedAt());
    }

    // 게시물 삭제
    @Transactional
    public void deletePostById(AuthUser authUser, Long postId){

        if (authUser.getUserRole() != UserRole.PATIENT){
            throw new ClientException(ErrorCode.PATIENT_ONLY_ACCESS);
        }

        Post findPost = postRepository.findByIdWithUser(postId).orElseThrow(() -> new ClientException(ErrorCode.NOT_FOUND_POST));

        if (authUser.getId() != findPost.getPatient().getId()){
            throw new ClientException(ErrorCode.ONLY_AUTHOR_CAN_UPDATE_OR_DELETED);
        }

        findPost.delete();
    }

    // 회원 쿠폰의 사용가능 횟수가 0이하인지 검증
    private void validateCouponAvailableCount(PatientCoupon patientCoupon) {
        if (patientCoupon.getAvailableCount() <= 0) {
            throw new ClientException(ErrorCode.EXHAUSTED_COUPON);
        }
    }

    // 회원쿠폰의 만료일이 지났는지 검증
    private void validateCouponExpired(PatientCoupon patientCoupon) {
        if (patientCoupon.getEndDate().isBefore(LocalDateTime.now())) {
            throw new ClientException(ErrorCode.EXPIRED_COUPON);
        }
    }
}
