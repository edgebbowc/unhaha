// 팝업 버튼 클릭 시 실행되는 함수
function toggleCommentOptions(button) {
    // 현재 클릭된 댓글의 팝업 메뉴 찾기
    const commentElement = button.closest('.comment');
    const popUp = commentElement.querySelector('#popUp');

    // 팝업 메뉴 활성화/비활성화 토글
    popUp.classList.toggle('active');

}

// 다른 영역 클릭 시 팝업 메뉴 닫기
document.addEventListener('click', function(event) {
    if (!event.target.closest('.popUpBtn') && !event.target.closest('#popUp')) {
        // 모든 활성 팝업 메뉴 찾아서 닫기
        document.querySelectorAll('#popUp.active').forEach(popup => {
            popup.classList.remove('active');
        });
    }
});

/**
 * 댓글 좋아요 폼 제출 처리
 * 자신의 댓글인 경우 제출을 막고 alert 표시
 */
function handleCommentLike(event, commentId, commentUserId, currentUserId) {
    // 로그인하지 않은 경우
    if (!currentUserId || currentUserId === 0) {
        event.preventDefault(); // 폼 제출 방지
        alert('로그인이 필요합니다');
        return false;
    }

    // 자신의 댓글인 경우
    if (commentUserId === currentUserId) {
        event.preventDefault(); // 폼 제출 방지
        alert('자기 자신의 댓글은 좋아요할 수 없습니다');
        return false;
    }

    // 다른 사용자의 댓글인 경우 정상 제출
    return true;
}