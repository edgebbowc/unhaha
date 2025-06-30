package commu.unhaha.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.CommentDto;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final UserLikeCommentRepository userLikeCommentRepository;
    private final CommentImageRepository commentImageRepository;
    private final GCSFileStore gcsFileStore;

    public static final int DEFAULT_COMMENT_PAGE_SIZE = 2;

    /**
     * 댓글 작성
     */
    public Comment createComment(User user, Long articleId, String content,
                                 List<String> imageUrls, Long parentId) {
        // 1. 엔티티 조회
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment parent = null;
        /* N + 1 문제 발생
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));
        }*/
        if (parentId != null) {
            // Fetch Join을 사용한 부모 댓글 조회
            parent = commentRepository.findParentWithRelations(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        }

        // 2. 이미지가 있는 경우 콘텐츠에 마크다운 형식으로 추가
        String finalContent = content;
        if (imageUrls != null && !imageUrls.isEmpty()) {
            finalContent = addImagesToContent(content, imageUrls);
        }

        // 3. 댓글 객체 생성 및 저장
        Comment comment = Comment.builder()
                .content(finalContent)
                .user(user)
                .article(article)
                .parent(parent)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 4. 이미지가 있는 경우 댓글에 연결
        if (imageUrls != null && !imageUrls.isEmpty()) {
            try {
                attachImagesToComment(savedComment, imageUrls);
            } catch (Exception e) {
                log.warn("이미지 연결 중 오류 발생: {}", e.getMessage());
                // 이미지 연결 실패해도 댓글은 저장됨 (트랜잭션 롤백 방지)
            }
        }

        return savedComment;
    }

    /**
     * 댓글내용에 이미지 마크다운 추가
     */
    private String addImagesToContent(String content, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return content;
        }

        StringBuilder contentWithImages = new StringBuilder(content);

        for (String url : imageUrls) {
            if (url != null && !url.trim().isEmpty()) {
                contentWithImages.append("\n![이미지](").append(url).append(")");
            }
        }

        return contentWithImages.toString();
    }

    /**
     * 댓글에 이미지 연결
     */
    private void attachImagesToComment(Comment comment, List<String> imageUrls) {
        List<CommentImage> tempImages = commentImageRepository.findByUrlInAndStatus(
                imageUrls, ImageStatus.TEMP);

        if (tempImages.isEmpty() && !imageUrls.isEmpty()) {
            throw new IllegalStateException("요청한 이미지를 찾을 수 없습니다.");
        }

        // URL 집합으로 빠른 조회를 위한 맵 생성
        Map<String, CommentImage> imageMap = tempImages.stream()
                .collect(Collectors.toMap(CommentImage::getUrl, image -> image));

        // 이미지 URL 순서대로 댓글에 연결
        for (String url : imageUrls) {
            CommentImage image = imageMap.get(url);
            if (image != null) {
                image.attachToComment(comment);
                commentImageRepository.save(image);
                log.debug("새 이미지를 댓글에 연결: {}", url);
            }
        }
    }

    /**
     * 댓글 ID로 조회
     * @param commentId
     * @return Comment
     */
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));
    }

    /**
     * 댓글 수정
     * @param commentId
     * @param content
     * @param newImageUrls
     */
    public void updateComment(Long commentId, String content, List<String> newImageUrls, String userEmail) {
        // 1. 댓글 조회
        Comment comment = findById(commentId);

        // 2. 소유자 검증 추가
        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        // 3. 새 이미지 URL 목록이 있으면 마크다운 형식으로 내용에 추가
        String finalContent = content;
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            finalContent = addImagesToContent(content, newImageUrls);
        }

        // 4. 현재 댓글에 연결된 모든 이미지 조회
        List<CommentImage> existingImages = commentImageRepository.findByCommentId(comment.getId());

        // 5. 이미지 처리
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            // 기존 이미지 URL 추출 (비교용)
            Set<String> existingImageUrls = existingImages.stream()
                    .map(CommentImage::getUrl)
                    .collect(Collectors.toSet());

            // 새 이미지 URL을 Set으로 변환 (검색 최적화)
            Set<String> newImageUrlSet = new HashSet<>(newImageUrls);

            // 5.1. 삭제할 이미지 처리: 기존 이미지 중 새 이미지 URL 목록에 없는 것들
            for (CommentImage existingImage : existingImages) {
                if (!newImageUrlSet.contains(existingImage.getUrl())) {
                    existingImage.markAsTemp();
                    log.debug("이미지를 TEMP 상태로 변경: {}", existingImage.getUrl());
                }
            }

            // 5.2. 추가할 이미지 처리: 새 이미지 URL 중 기존에 없는 것들
            for (String newUrl : newImageUrls) {
                if (!existingImageUrls.contains(newUrl)) {
                    // TEMP 상태인 이미지 조회
                    CommentImage tempImage = commentImageRepository.findByUrlAndStatus(newUrl, ImageStatus.TEMP)
                            .orElse(null);

                    if (tempImage != null) {
                        // 댓글에 연결하고 ACTIVE 상태로 변경
                        tempImage.attachToComment(comment);
                        commentImageRepository.save(tempImage);
                        log.debug("새 이미지를 댓글에 연결: {}", newUrl);
                    } else {
                        log.warn("요청한 이미지를 찾을 수 없음: {}", newUrl);
                    }
                }
            }
        } else {
            // 이미지 URL이 없는 경우, 모든 기존 이미지를 TEMP로 변경
            for (CommentImage existingImage : existingImages) {
                existingImage.markAsTemp();
                log.debug("모든 이미지를 TEMP 상태로 변경 (이미지 없는 수정)");
            }
        }

        // 5. 댓글 내용 업데이트
        comment.changeContent(finalContent);
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId, String userEmail ) {
        // 1. 댓글 조회
        Comment comment = findById(commentId);

        // 2. 소유자 검증 추가
        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        // 3. 모든 이미지 URL 수집 (현재 댓글 + 모든 자식 댓글)
        List<String> imageUrls = collectAllImageUrls(comment);

        // 4. GCS에서 이미지 파일 삭제
        deleteImagesFromGCS(imageUrls);

        // 5. 댓글 엔티티 삭제 (자식 댓글과 이미지 DB 레코드는 cascade로 자동 삭제됨)
        commentRepository.delete(comment);

        log.info("댓글 ID {} 및 관련 자식 댓글, 이미지 {}개 삭제 완료", commentId, imageUrls.size());
    }

    /**
     * 댓글과 모든 자식 댓글의 이미지 URL을 수집
     */
    private List<String> collectAllImageUrls(Comment comment) {

        // 댓글 ID 계층 구조 수집
        Set<Long> commentIds = new HashSet<>();
        collectCommentIds(comment, commentIds);

        // 수집된 댓글 ID에 해당하는 모든 이미지 URL 조회
        return commentImageRepository.findUrlsByCommentIdIn(commentIds);
    }

    /**
     * 댓글과 모든 자식 댓글의 ID를 재귀적으로 수집
     */
    private void collectCommentIds(Comment comment, Set<Long> commentIds) {
        commentIds.add(comment.getId());

        for (Comment child : comment.getChildren()) {
            collectCommentIds(child, commentIds);
        }
    }

    /**
     * GCS에서 이미지 파일들을 삭제
     */
    private void deleteImagesFromGCS(List<String> imageUrls) {
        if (imageUrls.isEmpty()) {
            return;
        }

        log.info("GCS에서 이미지 {}개 삭제 시작", imageUrls.size());

        int successCount = 0;
        int failCount = 0;

        for (String imageUrl : imageUrls) {
            try {
                gcsFileStore.deleteFile(imageUrl);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("GCS 이미지 삭제 실패: {}", imageUrl, e);
                // 개별 이미지 삭제 실패해도 전체 프로세스는 계속 진행
            }
        }

        log.info("GCS 이미지 삭제 결과 - 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 댓글 페이징
     */
    public Page<CommentDto> commentPageList(Long articleId, int page) {
        Pageable pageable = PageRequest.of(page, DEFAULT_COMMENT_PAGE_SIZE, Sort.by("createdDate").ascending());

        // 1) 페이징된 루트 댓글
        Page<Comment> rootPage =
                commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable);

        if (rootPage.isEmpty()) {
            // 루트 댓글이 없으면 빈 페이지 반환
            return Page.empty(pageable);
        }

        // 2) 이 페이지의 루트 댓글 ID 집합
        Set<Long> rootIds = rootPage.getContent().stream()
                .map(Comment::getId)
                .collect(Collectors.toSet());

        // 3) 이 루트 댓글들에 딸린 모든 자식 댓글(깊이 무관)을 시간순으로 한 번에 조회
        List<Comment> allReplies = commentRepository
                .findRepliesWithUserByArticleId(articleId)
                .stream()
                .filter(reply -> rootIds.contains(findRootCommentId(reply)))
                .collect(Collectors.toList());

        // 4) DTO 맵 및 parentId 설정
        List<CommentDto> rootDtos = rootPage.getContent().stream()
                .map(CommentDto::new)
                .collect(Collectors.toList());

        List<CommentDto> replyDtos = allReplies.stream()
                .map(CommentDto::new)
                .collect(Collectors.toList());

        // 5. 평면화된 목록 생성
        List<CommentDto> flatList = createFlatCommentList(rootDtos, replyDtos);

        return new PageImpl<>(flatList, pageable, rootPage.getTotalElements());
    }
    // new!
    private List<CommentDto> createFlatCommentList(List<CommentDto> rootDtos, List<CommentDto> replyDtos) {
        // 1. 전체 댓글 Map 생성
        Map<Long, CommentDto> commentMap = Stream.concat(rootDtos.stream(), replyDtos.stream())
                .collect(Collectors.toMap(CommentDto::getId, Function.identity()));

        // 2. 대댓글을 루트 ID별로 그룹핑 (한 번만 수행)
        Map<Long, List<CommentDto>> repliesByRoot = replyDtos.stream()
                .collect(Collectors.groupingBy(reply -> {
                    Long rootId = findRootIdOptimized(reply, commentMap);
                    return rootId != null ? rootId : -1L; // null 방지
                }));

        // 3. 평면화된 리스트 생성
        List<CommentDto> result = new ArrayList<>();
        for (CommentDto root : rootDtos) {
            result.add(root);

            // 4. 해당 루트의 대댓글들을 한 번에 추가
            List<CommentDto> rootReplies = repliesByRoot.get(root.getId());
            if (rootReplies != null) {
                result.addAll(rootReplies);
            }
        }

        return result;
    }

    // 개선된 findRootId
    private Long findRootIdOptimized(CommentDto reply, Map<Long, CommentDto> commentMap) {
        CommentDto current = reply;
        Set<Long> visited = new HashSet<>();

        while (current.getParentId() != null && !visited.contains(current.getId())) {
            visited.add(current.getId());
            current = commentMap.get(current.getParentId()); // O(1) 접근
            if (current == null) break;
        }

        return current != null ? current.getId() : null;
    }

    // new! O(n)복잡도
//    private Long findRootId(CommentDto reply, List<CommentDto> rootDtos, List<CommentDto> replyDtos) {
//        CommentDto current = reply;
//        Set<Long> visited = new HashSet<>();
//
//        while (current.getParentId() != null && !visited.contains(current.getId())) {
//            visited.add(current.getId());
//
//            // 부모 찾기
//            final Long parentId = current.getParentId();
//            current = rootDtos.stream()
//                    .filter(r -> r.getId().equals(parentId))
//                    .findFirst()
//                    .orElse(replyDtos.stream()
//                            .filter(r -> r.getId().equals(parentId))
//                            .findFirst()
//                            .orElse(null));
//
//            if (current == null) break;
//        }
//
//        return current != null ? current.getId() : null;
//    }
    // new!
    private Long findRootCommentId(Comment comment) {
        Comment current = comment;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current.getId();
    }

    /**
     * 게시글 조회용 페이지 인덱스 계산 (0-based, ArticleController용)
     */
    public int calculatePageIndex(Long articleId, Integer page) {
        long totalRootComments = countRootComments(articleId);
        int pageSize = DEFAULT_COMMENT_PAGE_SIZE;

        // 총 페이지 수 계산
        int totalPages = totalRootComments == 0 ? 1 :
                (int) Math.ceil((double) totalRootComments / pageSize);

        int targetPageIndex;
        if (page == null) {
            // 페이지 파라미터가 없으면 마지막 페이지
            targetPageIndex = Math.max(totalPages - 1, 0);
        } else {
            // 페이지 파라미터가 있으면 1-based -> 0-based 변환 및 범위 체크
            targetPageIndex = Math.max(page - 1, 0);
            targetPageIndex = Math.min(targetPageIndex, totalPages - 1);
        }

        return targetPageIndex;
    }

    // rootComment 개수 구하기
    public long countRootComments(Long articleId) {
        return commentRepository.countByArticleIdAndParentIsNull(articleId);
    }

    /**
     * 댓글 작성 후 리다이렉트용 페이지 번호 계산 (1-based, CommentController용)
     */
    public int calculateCommentRedirectPage(Comment comment, Long articleId) {
        try {
            // 성능 최적화된 정확한 페이지 계산
            return calculateExactCommentPage(comment, articleId);
        } catch (Exception e) {
            log.error("댓글 페이지 계산 중 오류 발생", e);
            // 모든 실패 시 마지막 페이지 반환
            return getLastPageNumber(articleId);
        }
    }

    /**
     * 마지막 페이지 번호 (1-based)
     */
    public int getLastPageNumber(Long articleId) {
        long totalRootComments = countRootComments(articleId);
        int pageSize = DEFAULT_COMMENT_PAGE_SIZE;

        return totalRootComments == 0 ? 1 :
                (int) Math.ceil((double) totalRootComments / pageSize);
    }

    /**
     * 댓글의 정확한 위치 페이지 계산 (1-based)
     */
    private int calculateExactCommentPage(Comment comment, Long articleId) {
        int pageSize = DEFAULT_COMMENT_PAGE_SIZE;

        // 대댓글인 경우 루트 댓글 찾기
        Comment rootComment = (comment.getParent() != null) ?
                findRootComment(comment) : comment;

        // 해당 댓글보다 이전에 작성된 루트 댓글 수만 COUNT로 조회
        long earlierCommentsCount = commentRepository
                .countByArticleIdAndParentIsNullAndIdLessThan(
                        articleId, rootComment.getId());

        // 페이지 번호 계산 (1-based)
        int pageNumber = (int) (earlierCommentsCount / pageSize) + 1;

        return pageNumber;
    }

    /**
     * 루트 댓글까지 거슬러 올라가기
     */
    private Comment findRootComment(Comment comment) {
        Comment current = comment;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    /**
     * 댓글 좋아요 여부 확인
     */
    public boolean findLike(Long commentId, Long userId) {
        return userLikeCommentRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    /**
     * 댓글 좋아요
     */
    public boolean saveLike(Long commentId, Long userId) {
        Comment comment = findById(commentId);
        if (comment.getUser().getId().equals(userId)) {
            throw new SecurityException("자신의 댓글에는 운하하 할 수 없습니다");
        }

        /** 로그인한 유저가 해당 게시물을 좋아요 했는지 안 했는지 확인 **/
        if (!findLike(commentId, userId)) {
            /* 좋아요 하지 않은 댓글이면 좋아요 추가, true 반환 */
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

            /* UserLikeComment 엔티티 생성 */
            UserLikeComment userLikeComment = new UserLikeComment(user, comment);
            userLikeCommentRepository.save(userLikeComment);
            comment.increaseLikeCount();
            return true;
        } else {
            /* 좋아요 한 댓글이면 좋아요 삭제 */
            userLikeCommentRepository.deleteByCommentIdAndUserId(commentId, userId);
            comment.decreaseLikeCount();
            return false;
        }
    }

    /**
     * 사용자가 좋아요한 댓글 ID 목록 조회
     */
    public List<Long> findLikedCommentsByUser(Long userId) {
        return userLikeCommentRepository.findByUserId(userId)
                .stream()
                .map(like -> like.getComment().getId())
                .collect(Collectors.toList());
    }

    /**
     * 사용자 이메일로 작성한 댓글 목록 조회
     */
    public List<CommentDto> findCommentsByUserEmail(String email) {
        List<Comment> comments = commentRepository.findByUserEmailOrderByCreatedDateDesc(email);
        return comments.stream()
                .map(CommentDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 좋아요한 댓글 목록 조회
     */
    public List<CommentDto> findLikedCommentsByUserEmail(String email) {
        List<Comment> likedComments = commentRepository.findLikedCommentsByUserEmail(email);
        return likedComments.stream()
                .map(CommentDto::new)
                .collect(Collectors.toList());
    }
}
