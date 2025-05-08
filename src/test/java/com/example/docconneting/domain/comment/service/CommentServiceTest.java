package com.example.docconneting.domain.comment.service;

import com.example.docconneting.common.enums.Major;
import com.example.docconneting.common.exception.constant.ErrorCode;
import com.example.docconneting.common.exception.object.ClientException;
import com.example.docconneting.common.exception.object.ServerException;
import com.example.docconneting.common.response.PageInfo;
import com.example.docconneting.common.response.PageResult;
import com.example.docconneting.domain.alarm.service.AlarmService;
import com.example.docconneting.domain.comment.dto.request.CommentRequest;
import com.example.docconneting.domain.comment.dto.response.CommentListResponse;
import com.example.docconneting.domain.comment.dto.response.CommentResponse;
import com.example.docconneting.domain.comment.entity.Comment;
import com.example.docconneting.domain.comment.repository.CommentRepository;
import com.example.docconneting.domain.post.entity.Post;
import com.example.docconneting.domain.post.repository.PostRepository;
import com.example.docconneting.domain.user.entity.User;
import com.example.docconneting.domain.user.enums.UserRole;
import com.example.docconneting.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    AlarmService alarmService;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() throws Exception {
        user = User.of(
                "test@test.com",
                "test!123",
                "name",
                Major.INTERNAL_MEDICINE,   // 전공
                null,                      // image
                null,                      // startTime
                null,                      // endTime
                false,                     // isDeleted
                UserRole.DOCTOR            // 역할
        );

        ReflectionTestUtils.setField(user, "id", 1L);

        post = Post.of(null, null, null, null, null, null, null, null);
        Field postIdField = Post.class.getDeclaredField("id");
        ReflectionTestUtils.setField(post, "id", 1L);
    }

    @Nested
    @Order(1)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateCommentTest {

        @Test
        @Order(1)
        void 사용자가_없으면_예외() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "comments");

            ServerException ex = assertThrows(ServerException.class, () ->
                    commentService.createComment(1L, 1L, request)
            );

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @Order(2)
        void 게시글이_없으면_예외() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "comments");

            ServerException ex = assertThrows(ServerException.class, () ->
                    commentService.createComment(1L, 1L, request)
            );

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);
        }

        @Test
        @Order(3)
        void 정상적으로_댓글_생성() {
            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "comments");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                Field idField = Comment.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(c, 10L);
                return c;
            });

            CommentResponse response = commentService.createComment(1L, 1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getContents()).isEqualTo("comments");
            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @Order(4)
        void 의사가_아니면_댓글_작성_불가() {
            ReflectionTestUtils.setField(user, "userRole", UserRole.PATIENT);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "comments");

            ServerException ex = assertThrows(ServerException.class, () ->
                    commentService.createComment(1L, 1L, request)
            );

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ALLOWED_TO_COMMENT);
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UpdateCommentTest {

        private Comment comment;

        @BeforeEach
        void setUp() throws Exception {
            comment = Comment.of(user, post, "comments");
            Field commentIdField = Comment.class.getDeclaredField("id");
            commentIdField.setAccessible(true);
            commentIdField.set(comment, 100L);
        }

        @Test
        @Order(1)
        void 댓글이_없으면_예외() {
            when(commentRepository.findById(100L)).thenReturn(Optional.empty());

            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "updateComments");

            ServerException ex = assertThrows(ServerException.class, () ->
                    commentService.updateComment(1L, 100L, request)
            );

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @Order(2)
        void 작성자가_아니면_예외() throws Exception {
            // 현재 로그인한 사용자
            Long currentUserId = 1L;

            // 댓글의 작성자(다른 사람)
            User anotherUser = User.of(
                    "another@test.com",
                    "test!123",
                    "name",
                    Major.INTERNAL_MEDICINE,   // 전공
                    null,                      // image
                    null,                      // startTime
                    null,                      // endTime
                    false,                     // isDeleted
                    UserRole.DOCTOR            // 역할
            );

            // 다른 유저의 id(id = 99L)
            Field anotherUserIdField = User.class.getDeclaredField("id");
            anotherUserIdField.setAccessible(true);
            anotherUserIdField.set(user, 99L);

            Comment anotherComment = Comment.of(anotherUser, post, "anotherComments");
            Field commentIdField = Comment.class.getDeclaredField("id");
            commentIdField.setAccessible(true);
            commentIdField.set(comment, 100L);

            when(commentRepository.findById(100L)).thenReturn(Optional.of(anotherComment));

            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "updateComments");

            ServerException ex = assertThrows(ServerException.class, () ->
                    commentService.updateComment(1L, 100L, request)
            );

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_COMMENT_OWNER);

        }

        @Test
        @Order(3)
        void 정상적으로_댓글_수정() {
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

            CommentRequest request = new CommentRequest();
            ReflectionTestUtils.setField(request, "contents", "updateComments");

            CommentResponse response = commentService.updateComment(1L, 100L, request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getContents()).isEqualTo("updateComments");
        }
    }

    @Test
    @DisplayName("댓글 리스트 조회 게시물이 존재하지 않을 때")
    void findAllCommentsNoPostTest(){
        // given
        Long postId = 1L;

        Pageable pageable = PageRequest.of(0,10);

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when
        ClientException clientException = assertThrows(ClientException.class, () -> commentService.findAllComments(postId, pageable));

        // then
        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findById(postId);
        verify(commentRepository, times(0)).findPosts(postId, pageable);
    }

    @Test
    @DisplayName("댓글 리스트 조회 게시물이 삭제된 게시물 일 때")
    void findAllCommentsDeletedPostTest(){
        // given
        Long postId = 1L;

        Pageable pageable = PageRequest.of(0,10);

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", true);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        ClientException clientException = assertThrows(ClientException.class, () -> commentService.findAllComments(postId, pageable));

        // then
        assertThat(clientException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_POST);

        verify(postRepository, times(1)).findById(postId);
        verify(commentRepository, times(0)).findPosts(postId, pageable);
    }

    @Test
    @DisplayName("댓글 리스트 조회")
    void findAllCommentsTest(){
        // given
        Long postId = 1L;

        Pageable pageable = PageRequest.of(0,10);

        List<Comment> content = new ArrayList<>();
        for(int i=0;i<50;i++){
            Comment comment = Comment.of(null, null, null);
            content.add(comment);
        }

        Page<Comment> page = new PageImpl<>(content, pageable, content.size());

        Post post = Post.of(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(post, "isDeleted", false);

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        given(commentRepository.findPosts(postId, pageable)).willReturn(page);

        // when
        PageResult<CommentListResponse> pageResult = commentService.findAllComments(postId, pageable);

        List<CommentListResponse> getContent = pageResult.getContent();
        PageInfo pageInfo = pageResult.getPageInfo();

        // then
        assertThat(getContent.size()).isEqualTo(content.size());
        assertThat(pageInfo.getPageNum()).isEqualTo(pageable.getPageNumber());
        assertThat(pageInfo.getPageSize()).isEqualTo(pageable.getPageSize());
        assertThat(pageInfo.getTotalPage()).isEqualTo(page.getTotalPages());
        assertThat(pageInfo.getTotalElement()).isEqualTo(page.getTotalElements());

        verify(postRepository, times(1)).findById(postId);
        verify(commentRepository, times(1)).findPosts(postId, pageable);
    }
}
