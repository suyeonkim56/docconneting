package com.example.docconneting.domain.post.service;

import com.example.docconneting.common.enums.Major;
import com.example.docconneting.common.exception.constant.ErrorCode;
import com.example.docconneting.common.exception.object.ClientException;
import com.example.docconneting.common.response.PageInfo;
import com.example.docconneting.common.response.PageResult;
import com.example.docconneting.domain.alarm.service.AlarmService;
import com.example.docconneting.domain.auth.entity.AuthUser;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    EntityManager entityManager;

    @Mock
    PatientCouponService patientCouponService;

    @Mock
    PointService pointService;

    @Mock
    AlarmService alarmService;

    @InjectMocks
    PostService postService;

    @Test
    @DisplayName("무료로 게시물 등록")
    void freeCreatePost() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        AuthUser authUser = AuthUser.of(userId, UserRole.PATIENT);
        User user = User.of("test@example.com", "password", "username", 0, false, UserRole.PATIENT);

        PostCreateRequest request = new PostCreateRequest();
        ReflectionTestUtils.setField(request, "title", "title");
        ReflectionTestUtils.setField(request, "contents", "contents");
        ReflectionTestUtils.setField(request, "major", "ORTHOPEDICS");
        ReflectionTestUtils.setField(request, "payType", "FREE");

        Post savedPost = Post.of(
                user,
                "title",
                "content",
                Major.ORTHOPEDICS,
                PayType.FREE,
                false,
                false,
                LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(savedPost, "id", postId);

        given(userRepository.findUserByIdAndUserRole(userId, UserRole.PATIENT)).willReturn(Optional.of(user));

        // when
        PostCreateResponse response = postService.createPost(authUser, null, request);

        // then
        assertThat(response).isNotNull();
        assertThat(request.getTitle()).isEqualTo(response.getTitle());
    }

    @Test
    @DisplayName("쿠폰으로 유료 게시물 등록")
    void couponCreatePostTest() {
        // given
        Long userId = 1L;
        Long postId = 1L;
        Long couponId = 1L;

        AuthUser authUser = AuthUser.of(userId, UserRole.PATIENT);
        User user = User.of("test@example.com", "password", "username", 0, false, UserRole.PATIENT);
        ReflectionTestUtils.setField(user, "id", userId);

        PostCreateRequest request = new PostCreateRequest();
        ReflectionTestUtils.setField(request, "title", "title");
        ReflectionTestUtils.setField(request, "contents", "contents");
        ReflectionTestUtils.setField(request, "major", "ORTHOPEDICS");
        ReflectionTestUtils.setField(request, "payType", "COUPON");

        Post savedPost = Post.of(
                user,
                "title",
                "content",
                Major.ORTHOPEDICS,
                PayType.COUPON,
                false,
                false,
                LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(savedPost, "id", postId);

        given(userRepository.findUserByIdAndUserRole(userId, UserRole.PATIENT)).willReturn(Optional.of(user));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostCreateResponse response = postService.createPost(authUser, couponId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(request.getTitle()).isEqualTo(response.getTitle());
    }

    @Test
    @DisplayName("포인트로 유료 게시물 등록")
    void pointCreatePostTest() {
        // given
        Long userId = 1L;
        Long postId = 1L;
        int point = 1000;

        AuthUser authUser = AuthUser.of(userId, UserRole.PATIENT);
        User user = User.of("test@example.com", "password", "username", point, false, UserRole.PATIENT);
        ReflectionTestUtils.setField(user, "id", userId);

        PostCreateRequest request = new PostCreateRequest();
        ReflectionTestUtils.setField(request, "title", "title");
        ReflectionTestUtils.setField(request, "contents", "contents");
        ReflectionTestUtils.setField(request, "major", "ORTHOPEDICS");
        ReflectionTestUtils.setField(request, "payType", "POINT");

        Post savedPost = Post.of(
                user,
                "title",
                "content",
                Major.ORTHOPEDICS,
                PayType.POINT,
                false,
                false,
                LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(savedPost, "id", postId);

        given(userRepository.findUserByIdAndUserRole(userId, UserRole.PATIENT)).willReturn(Optional.of(user));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostCreateResponse response = postService.createPost(authUser, null, request);

        // then
        assertThat(response).isNotNull();
        assertThat(request.getTitle()).isEqualTo(response.getTitle());
    }

    @Test
    @DisplayName("게시글 등록 유저 조회 실패")
    void createPostUserNotFoundTest() {
        // given
        Long userId = 1L;

        AuthUser authUser = AuthUser.of(userId, UserRole.PATIENT);
        PostCreateRequest request = new PostCreateRequest();

        given(userRepository.findUserByIdAndUserRole(userId, UserRole.PATIENT)).willReturn(Optional.empty());

        // when, then
        ClientException thrown = assertThrows(ClientException.class, () -> postService.createPost(authUser, null, request));
        assertThat(HttpStatus.NOT_FOUND).isEqualTo(thrown.getErrorCode().getStatus());
        assertThat(ErrorCode.USER_NOT_FOUND.getMessage()).isEqualTo(thrown.getErrorCode().getMessage());
    }

    @Test
    @DisplayName("쿠폰으로 게시글 등록 시 쿠폰 아이디가 null일 때")
    void missingCouponIdBadRequestTest() {
        // given
        Long userId = 1L;

        AuthUser authUser = AuthUser.of(userId, UserRole.PATIENT);
        PostCreateRequest request = new PostCreateRequest();
        ReflectionTestUtils.setField(request, "title", "title");
        ReflectionTestUtils.setField(request, "contents", "contents");
        ReflectionTestUtils.setField(request, "major", "ORTHOPEDICS");
        ReflectionTestUtils.setField(request, "payType", "COUPON");

        User user = User.of("test@example.com", "password", "username", 0, false, UserRole.PATIENT);
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findUserByIdAndUserRole(userId, UserRole.PATIENT)).willReturn(Optional.of(user));

        // when, then
        ClientException thrown = assertThrows(ClientException.class, () -> postService.createPost(authUser, null, request));
        assertThat(HttpStatus.BAD_REQUEST).isEqualTo(thrown.getErrorCode().getStatus());
        assertThat(ErrorCode.MISSING_COUPON_ID.getMessage()).isEqualTo(thrown.getErrorCode().getMessage());
    }

    @Test
    @DisplayName("서비스에서 게시물 단건 조회시 게시물이 없을 때")
    void findPostByIdFailTest(){
        // given
        Long postId = 1L;

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.empty());

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.findPostById(postId));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 단건 조회시 게시물이 이미 삭제된 게시물 일 때")
    void findPostByIdAlreadyDeleteTest(){
        // given
        Long postId = 1L;

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", true);

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.findPostById(postId));

        assertThat(clientException).isInstanceOf(ClientException.class);
        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 단건 조회 성공")
    void findPostByIdSuccessTest(){
        // given
        Long postId = 1L;

        User user = User.of(null, null, null, null, null, null);
        ReflectionTestUtils.setField(user, "username", "username");

        Post savedPost = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(savedPost, "id", postId);
        ReflectionTestUtils.setField(savedPost, "patient", user);
        ReflectionTestUtils.setField(savedPost, "title", "title");
        ReflectionTestUtils.setField(savedPost, "contents", "content");
        ReflectionTestUtils.setField(savedPost, "major", Major.ORTHOPEDICS);
        ReflectionTestUtils.setField(savedPost, "isDeleted", false);
        ReflectionTestUtils.setField(savedPost, "isReplied", false);
        ReflectionTestUtils.setField(savedPost, "deadline", LocalDateTime.now());
        ReflectionTestUtils.setField(savedPost, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(savedPost, "modifiedAt", LocalDateTime.now());

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(savedPost));

        // when
        PostSingleResponse postSingleResponse = postService.findPostById(postId);

        // then
        assertThat(postSingleResponse.getId()).isEqualTo(postId);
        assertThat(savedPost.getPatient().getUsername()).isEqualTo(postSingleResponse.getPatientName());
        assertThat(postSingleResponse.getTitle()).isEqualTo("title");
        assertThat(postSingleResponse.getContents()).isEqualTo("content");
        assertThat(postSingleResponse.getMajor()).isEqualTo(Major.ORTHOPEDICS.name());
        assertThat(postSingleResponse.getIsReplied()).isEqualTo(false);
        assertThat(postSingleResponse.getDeadline()).isEqualTo(savedPost.getDeadline());
        assertThat(postSingleResponse.getCreatedAt()).isEqualTo(savedPost.getCreatedAt());
        assertThat(postSingleResponse.getModifiedAt()).isEqualTo(savedPost.getModifiedAt());

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 삭제시 유저가 PATIENT 가 아닐 때")
    void deletePostByIdNotPatientTest(){
        // given
        Long postId = 1L;

        Long userId = 1L;
        UserRole userRole = UserRole.ADMIN;
        AuthUser authUser = AuthUser.of(userId, userRole);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", false);

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.deletePostById(authUser, postId));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.PATIENT_ONLY_ACCESS);

        verify(postRepository, times(0)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 삭제시 게시물이 없을 떄")
    void deletePostByIdFailTest(){
        // given
        Long postId = 1L;

        Long userId = 1L;
        UserRole userRole = UserRole.PATIENT;
        AuthUser authUser = AuthUser.of(userId, userRole);

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.empty());

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.deletePostById(authUser, postId));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 삭제시 유저가 작성한 게시물이 아닐 때")
    void deletePostByIdNotUserPostTest(){
        // given
        Long postId = 1L;

        Long userId = 2L;
        UserRole userRole = UserRole.PATIENT;
        AuthUser authUser = AuthUser.of(userId, userRole);

        User user = User.of(null, null, null , null, null, null);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", false);
        ReflectionTestUtils.setField(post, "patient", user);

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.deletePostById(authUser, postId));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.ONLY_AUTHOR_CAN_UPDATE_OR_DELETED);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 삭제 성공")
    void deletePostByIdSuccessTest(){
        // given
        Long postId = 1L;

        Long userId = 1L;
        UserRole userRole = UserRole.PATIENT;
        AuthUser authUser = AuthUser.of(userId, userRole);

        User user = User.of(null, null, null , null, null, null);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", false);
        ReflectionTestUtils.setField(post, "patient", user);

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));

        // when, then
        postService.deletePostById(authUser, postId);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 수정시 유저가 PATIENT가 아닐 때")
    void updatePostUserRoleFailedTest(){
        // given
        Long postId = 1L;
        Long userId = 1L;
        UserRole userRole = UserRole.ADMIN;

        AuthUser authUser = AuthUser.of(userId, userRole);

        PostUpdateRequest postUpdateRequest = new PostUpdateRequest();
        ReflectionTestUtils.setField(postUpdateRequest, "title", "tile");
        ReflectionTestUtils.setField(postUpdateRequest, "contents", "content");

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.updatePost(authUser, postId, postUpdateRequest));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.PATIENT_ONLY_ACCESS);

        verify(postRepository, times(0)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 수정시 게시물이 없을 때")
    void updatePostFailedTest(){
        // given
        Long postId = 1L;
        Long userId = 1L;
        UserRole userRole = UserRole.PATIENT;

        AuthUser authUser = AuthUser.of(userId, userRole);

        PostUpdateRequest postUpdateRequest = new PostUpdateRequest();
        ReflectionTestUtils.setField(postUpdateRequest, "title", "tile");
        ReflectionTestUtils.setField(postUpdateRequest, "contents", "content");

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.empty());

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.updatePost(authUser, postId, postUpdateRequest));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 수정시 게시물이 삭제된 게시물 일 때")
    void updatePostAlreadyDeletedTest(){
        // given
        Long postId = 1L;
        Long userId = 1L;
        UserRole userRole = UserRole.PATIENT;

        AuthUser authUser = AuthUser.of(userId, userRole);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", true);

        PostUpdateRequest postUpdateRequest = new PostUpdateRequest();
        ReflectionTestUtils.setField(postUpdateRequest, "title", "tile");
        ReflectionTestUtils.setField(postUpdateRequest, "contents", "content");

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.updatePost(authUser, postId, postUpdateRequest));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 수정시 유저가 작성한 게시물이 아닐 때")
    void updatePostNotUsersPostTest(){
        // given
        Long postId = 1L;
        Long userId = 2L;
        UserRole userRole = UserRole.PATIENT;

        AuthUser authUser = AuthUser.of(userId, userRole);

        User user = User.of(null, null, null, null, null, null);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", false);
        ReflectionTestUtils.setField(post, "patient", user);

        PostUpdateRequest postUpdateRequest = new PostUpdateRequest();
        ReflectionTestUtils.setField(postUpdateRequest, "title", "tile");
        ReflectionTestUtils.setField(postUpdateRequest, "contents", "content");

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));

        // when, then
        ClientException clientException = assertThrows(ClientException.class, () -> postService.updatePost(authUser, postId, postUpdateRequest));

        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.ONLY_AUTHOR_CAN_UPDATE_OR_DELETED);

        verify(postRepository, times(1)).findByIdWithUser(postId);
    }

    @Test
    @DisplayName("서비스에서 게시물 수정 테스트")
    void updatePostTest(){
        // given
        Long postId = 1L;
        Long userId = 1L;
        UserRole userRole = UserRole.PATIENT;

        AuthUser authUser = AuthUser.of(userId, userRole);

        User user = User.of(null, null, null, null, null, null);
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", false);
        ReflectionTestUtils.setField(post, "id", postId);
        ReflectionTestUtils.setField(post, "patient", user);
        ReflectionTestUtils.setField(post, "title", "title");
        ReflectionTestUtils.setField(post, "contents", "content");
        ReflectionTestUtils.setField(post, "major", Major.ORTHOPEDICS);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "modifiedAt", LocalDateTime.now());

        PostUpdateRequest postUpdateRequest = new PostUpdateRequest();
        ReflectionTestUtils.setField(postUpdateRequest, "title", "updateTitle");
        ReflectionTestUtils.setField(postUpdateRequest, "contents", "updateContent");

        given(postRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));

        doNothing().when(entityManager).flush();

        // when
        PostUpdateResponse postUpdateResponse = postService.updatePost(authUser, postId, postUpdateRequest);

        // then
        assertThat(postUpdateResponse.getId()).isEqualTo(postId);
        assertThat(postUpdateResponse.getTitle()).isEqualTo(postUpdateRequest.getTitle());
        assertThat(postUpdateResponse.getContents()).isEqualTo(postUpdateRequest.getContents());
        assertThat(postUpdateResponse.getMajor()).isEqualTo(Major.ORTHOPEDICS.name());
        assertThat(postUpdateResponse.getCreatedAt()).isEqualTo(post.getCreatedAt());
        assertThat(postUpdateResponse.getModifiedAt()).isEqualTo(post.getModifiedAt());

        verify(postRepository, times(1)).findByIdWithUser(postId);
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("서비스에서 게시물 리스트 조회")
    void findAllPostsTest(){
        // given
        User user = User.of(null, null, null, null, null, null);
        ReflectionTestUtils.setField(user, "username", "testName");

        Pageable pageable = PageRequest.of(0, 10);
        String title = "title";
        String major = Major.values()[0].name();

        List<Post> content = new ArrayList<>();
        for(int i = 0; i < 50; i++){
            Post post = Post.of(user, title, "contents", Major.valueOf(major), false, false);
            content.add(post);
        }

        Page<Post> posts = new PageImpl<>(content, pageable, content.size());

        given(postRepository.findPosts(pageable, title, major)).willReturn(posts);

        // when
        PageResult<PostListResponse> pageResult = postService.findAllPosts(pageable, title, major);

        List<PostListResponse> getContent = pageResult.getContent();
        PageInfo pageInfo = pageResult.getPageInfo();

        // then
        assertThat(getContent.size()).isEqualTo(content.size());
        assertThat(pageInfo.getPageNum()).isEqualTo(pageable.getPageNumber());
        assertThat(pageInfo.getPageSize()).isEqualTo(pageable.getPageSize());
        assertThat(pageInfo.getTotalPage()).isEqualTo(posts.getTotalPages());
        assertThat(pageInfo.getTotalElement()).isEqualTo(posts.getTotalElements());

        verify(postRepository, times(1)).findPosts(pageable, title, major);
    }
}