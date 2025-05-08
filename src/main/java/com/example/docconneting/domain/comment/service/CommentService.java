package com.example.docconneting.domain.comment.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final AlarmService alarmService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServerException(ErrorCode.USER_NOT_FOUND));

        if (!UserRole.DOCTOR.equals(user.getUserRole())) {
            throw new ServerException(ErrorCode.NOT_ALLOWED_TO_COMMENT);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServerException(ErrorCode.NOT_FOUND_POST));

        Comment comment = Comment.of(user, post, request.getContents());

        Comment saved = commentRepository.save(comment);

        // 게시물 작성자에게 알람 전송
        alarmService.sendCommentCompletedMessage(post.getPatient());

        CommentResponse response = CommentResponse.of(
                comment.getId(),
                comment.getContents(),
                comment.getCreatedAt(),
                comment.getModifiedAt());

        return response;
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest request) {

        Comment findComment = commentRepository.findById(commentId)
                .orElseThrow(()->new ServerException(ErrorCode.COMMENT_NOT_FOUND));

        validateCommentPermission(userId, findComment);

        findComment.updateContents(request.getContents());

        CommentResponse response = CommentResponse.of(
                findComment.getId(),
                findComment.getContents(),
                findComment.getCreatedAt(),
                findComment.getModifiedAt());

        return response;
    }

    @Transactional(readOnly = true)
    public PageResult<CommentListResponse> findAllComments(Long postId, Pageable pageable){

        Post findPost = postRepository.findById(postId).orElseThrow(() -> new ClientException(ErrorCode.NOT_FOUND_POST));

        if (findPost.getIsDeleted()){
            throw new ClientException(ErrorCode.NOT_FOUND_POST);
        }

        Page<Comment> posts = commentRepository.findPosts(postId, pageable);

        List<Comment> content = posts.getContent();
        Pageable commentsPageable = posts.getPageable();

        List<CommentListResponse> commentListResponses = CommentListResponse.toCommentListResponses(content);

        PageInfo pageInfo = PageInfo.builder()
                .pageNum(commentsPageable.getPageNumber())
                .pageSize(commentsPageable.getPageSize())
                .totalElement(posts.getTotalElements())
                .totalPage(posts.getTotalPages())
                .build();

        entityManager.flush();

        return new PageResult<>(commentListResponses, pageInfo);
    }

    private void validateCommentPermission(Long userId, Comment findComment) {
        if (!userId.equals(findComment.getUser().getId())) {
            throw new ServerException(ErrorCode.NOT_COMMENT_OWNER);
        }
    }
}
