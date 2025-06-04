document.addEventListener('DOMContentLoaded', function () {
    // 모든 댓글 답글 버튼에 이벤트 리스너 추가
    initReplyButtons();
    // 댓글 수정 버튼 초기화
    document.querySelectorAll('#commentEdit').forEach(function (button) {
        button.addEventListener('click', function () {
            handleEditButtonClick(this);
        });
    });
});

// 답글 버튼 초기화
function initReplyButtons() {
    document.querySelectorAll('#commentReply').forEach(function (button) {
        button.addEventListener('click', function () {
            handleReplyButtonClick(this);
        });
    });
}

// 현재 열려있는 폼을 추적하는 전역 변수
let currentOpenForm = null;

// 모든 폼 닫기 함수
function closeAllForms() {
    if (currentOpenForm) {
        const {commentEtc, formType, commentId, button} = currentOpenForm;

        // 폼 숨기기
        commentEtc.innerHTML = '';

        // 버튼 상태 원복
        if (formType === 'reply') {
            const buttonText = button.childNodes[button.childNodes.length - 1];
            const buttonSvg = button.querySelector('svg');
            buttonText.nodeValue = "댓글쓰기";
            buttonSvg.style.display = "inline-block";
        } else if (formType === 'edit') {
            const buttonText = button.childNodes[0];
            buttonText.nodeValue = "수정";
            const popUp = button.closest('.comment').querySelector('#popUp');
            if (popUp) popUp.classList.remove('active');
        }

        currentOpenForm = null;
    }
}

// 답글 버튼 클릭 처리
function handleReplyButtonClick(button) {
    // 현재 댓글의 ID 가져오기
    const commentId = button.closest('.comment').getAttribute('id').replace('comment', '');

    const commentEtc = button.closest('.comment').querySelector('#commentEtc');

    // SVG 요소와 버튼 텍스트를 위한 변수
    const buttonSvg = button.querySelector('svg');

    // 텍스트 노드 찾기
    const buttonText = button.childNodes[button.childNodes.length - 1];

    if (currentOpenForm && currentOpenForm.formType === 'reply' &&
        currentOpenForm.commentId === commentId) {
        closeAllForms();
        return;
    }

    // 다른 열린 폼이 있다면 닫기
    closeAllForms();

    // 폼 열기
    const replyFormHTML = createReplyFormHTML(commentId);

    commentEtc.innerHTML = replyFormHTML;

    // 버튼 텍스트를 "닫기"로 변경
    buttonText.nodeValue = "닫기";

    // SVG 아이콘 숨김 처리
    buttonSvg.style.display = "none";

    // 현재 열린 폼 정보 업데이트
    currentOpenForm = {
        commentEtc,
        formType: 'reply',
        commentId,
        button
    };

    // 이미지 업로드 기능 추가
    initImageUpload(
        `commentImageButton-${commentId}`,
        `#commentImageFile-${commentId}`,
        `attached-${commentId}`
    );

    // 폼 유효성 검사
    initFormValidation(
        `replyForm-${commentId}`,
        `replyContent-${commentId}`
    );
}

function createReplyFormHTML(commentId) {
    return `
        <div class="contentContainer">
            <form id="replyForm-${commentId}" action="/articles/${articleId}/comments" method="post" enctype="multipart/form-data">
                <input type="hidden" name="parentId" value="${commentId}">
                <div class="commentInput">
                    <div class="commentContent">
                        <textarea id="replyContent-${commentId}" name="content" placeholder="댓글을 작성해주세요" maxlength="400"></textarea>
                        <div class="attached" id="attached-${commentId}"></div>
                    </div>
                </div>
                <div class="commentAttaches">
                    <div class="btn-area">
                        <div class="attaches">
                            <button type="button" id="commentImageButton-${commentId}" class="commentImageButton" style="
                            padding: 12px 16px;
                            background-color: var(--default-background-color);
                            border: 0;
                            border-radius: 50vh;">
                                <svg width="12" height="12" viewBox="0 0 12 12" fill="none"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path fill-rule="evenodd" clip-rule="evenodd"
                                          d="M10.8476 1.61328H1.15236C0.801242 1.61328 0.516602 1.90425 0.516602 2.26317V9.73683C0.516602 10.0958 0.801242 10.3867 1.15236 10.3867H10.8476C11.1988 10.3867 11.4834 10.0958 11.4834 9.73683V2.26317C11.4834 1.90425 11.1988 1.61328 10.8476 1.61328ZM4.69141 4.13301C4.69141 4.75433 4.18773 5.25801 3.56641 5.25801C2.94509 5.25801 2.44141 4.75433 2.44141 4.13301C2.44141 3.51169 2.94509 3.00801 3.56641 3.00801C4.18773 3.00801 4.69141 3.51169 4.69141 4.13301ZM3.91287 9.72654L3.91288 9.72652H1.00293L3.06215 6.80075C3.12264 6.71432 3.20201 6.64284 3.29424 6.59163C3.38641 6.54037 3.48907 6.51077 3.59442 6.50506C3.69972 6.49936 3.80498 6.51763 3.90217 6.5586C3.9994 6.59951 4.08606 6.66204 4.15555 6.74137L5.11109 8.02409L6.63979 5.85209C6.71989 5.73765 6.825 5.64298 6.94713 5.57516C7.06919 5.50728 7.20514 5.46809 7.34465 5.46053C7.48409 5.45297 7.62348 5.47718 7.75218 5.53142C7.88094 5.5856 7.99569 5.66841 8.08771 5.77346L11.1212 9.72654H3.91287Z"></path>
                                </svg>
                            </button>
                            <input class="commentImageFile" id="commentImageFile-${commentId}" type="file" accept="image/*">
                        </div>
                        <button type="submit" class="large primary" id="addReplyClick-${commentId}"
                                style="width: 74px; background-color: #1CA8AF;">
                            <i class="fa-solid fa-comment"></i>
                            등록
                        </button>
                    </div>
                </div>
            </form>
        </div>
    `;
}

/**
 * 댓글 수정 버튼 클릭시 처리 함수
 * @param button
 */
function handleEditButtonClick(button) {
    // 현재 댓글의 ID 가져오기
    const commentElement = button.closest('.comment');
    const commentId = commentElement.getAttribute('id').replace('comment', '');

    const commentContentElement = commentElement.querySelector('.commentContent');
    const commentContent = commentContentElement.innerText || '';

    // 댓글 내용 영역 찾기
    const commentEtc = commentElement.querySelector('#commentEtc');

    const originalContent = commentEtc.innerHTML;

    // 텍스트 노드 찾기
    const buttonText = button.childNodes[button.childNodes.length - 1];

    const popUp = commentElement.querySelector('#popUp');

    // 같은 댓글의 폼이 이미 열려있으면 닫기
    if (currentOpenForm && currentOpenForm.formType === 'edit' &&
        currentOpenForm.commentId === commentId) {
        closeAllForms();
        if (popUp) popUp.classList.remove('active');
        return;
    }

    // 다른 열린 폼이 있다면 닫기
    closeAllForms();

    // 버튼 텍스트를 "취소"로 변경
    buttonText.nodeValue = "취소";

    // 현재 열린 폼 정보 업데이트
    currentOpenForm = {
        commentEtc,
        formType: 'edit',
        commentId,
        button
    };

    const existingImages = [];
    const imageElements = commentElement.querySelectorAll('.comment-image');
    imageElements.forEach(img => {
        existingImages.push(img.src);
    });

    commentEtc.innerHTML = createEditFormHTML(commentId, commentContent.trim());

    // 수정 폼 요소 찾기
    const form = commentEtc.querySelector('form');
    const attachedDiv = form.querySelector(`#attached-edit-${commentId}`);

    // 이미지 추가
    existingImages.forEach(imageUrl => {
        addImageToForm(imageUrl, attachedDiv);
    });


    // 이미지 업로드 기능 추가
    initImageUpload(
        `editImageButton-${commentId}`,
        `#editImageFile-${commentId}`,
        `attached-edit-${commentId}`
    );

    // 폼 제출시 유효성 검사
    initFormValidation(
        `editForm-${commentId}`,
        `editContent-${commentId}`
    );
}

function createEditFormHTML(commentId, commentContent) {
    return `
        <div class="contentContainer">
            <form id="editForm-${commentId}" action="/articles/${articleId}/comments/${commentId}" method="post" enctype="multipart/form-data">
            <input type="hidden" name="currentPage" value="${currentPage}">
                <div class="commentInput">
                    <div class="commentContent">
                        <textarea id="editContent-${commentId}" name="content" maxlength="400">${commentContent}</textarea>
                        <div class="attached" id="attached-edit-${commentId}"></div>
                    </div>
                </div>
                <div class="commentAttaches">
                    <div class="btn-area">
                        <div class="attaches">
                            <button type="button" id="editImageButton-${commentId}" class="commentImageButton" style="
                            padding: 12px 16px;
                            background-color: var(--default-background-color);
                            border: 0;
                            border-radius: 50vh;">
                                <svg width="12" height="12" viewBox="0 0 12 12" fill="none"
                                     xmlns="http://www.w3.org/2000/svg">
                                    <path fill-rule="evenodd" clip-rule="evenodd"
                                          d="M10.8476 1.61328H1.15236C0.801242 1.61328 0.516602 1.90425 0.516602 2.26317V9.73683C0.516602 10.0958 0.801242 10.3867 1.15236 10.3867H10.8476C11.1988 10.3867 11.4834 10.0958 11.4834 9.73683V2.26317C11.4834 1.90425 11.1988 1.61328 10.8476 1.61328ZM4.69141 4.13301C4.69141 4.75433 4.18773 5.25801 3.56641 5.25801C2.94509 5.25801 2.44141 4.75433 2.44141 4.13301C2.44141 3.51169 2.94509 3.00801 3.56641 3.00801C4.18773 3.00801 4.69141 3.51169 4.69141 4.13301ZM3.91287 9.72654L3.91288 9.72652H1.00293L3.06215 6.80075C3.12264 6.71432 3.20201 6.64284 3.29424 6.59163C3.38641 6.54037 3.48907 6.51077 3.59442 6.50506C3.69972 6.49936 3.80498 6.51763 3.90217 6.5586C3.9994 6.59951 4.08606 6.66204 4.15555 6.74137L5.11109 8.02409L6.63979 5.85209C6.71989 5.73765 6.825 5.64298 6.94713 5.57516C7.06919 5.50728 7.20514 5.46809 7.34465 5.46053C7.48409 5.45297 7.62348 5.47718 7.75218 5.53142C7.88094 5.5856 7.99569 5.66841 8.08771 5.77346L11.1212 9.72654H3.91287Z"></path>
                                </svg>
                            </button>
                            <input class="commentImageFile" id="editImageFile-${commentId}" type="file" accept="image/*">
                        </div>
                        <div class="edit-buttons">
                            <button type="submit" class="large primary" id="editCommentSubmit-${commentId}"
                                    style="width: 74px; background-color: #1CA8AF;" >
                                    <i class="fa-solid fa-comment"></i>
                                등록
                            </button>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    `;
}