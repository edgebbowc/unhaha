document.addEventListener('DOMContentLoaded', function() {
    // 메인 댓글 폼 이미지 업로드 초기화
    initImageUpload(
        imageUploadConfig.mainImageButtonId,
        imageUploadConfig.mainFileInputClass,
        imageUploadConfig.mainAttachedDivId
    );

    // 메인 댓글 폼 유효성 검사
    initFormValidation(
        imageUploadConfig.mainFormId,
        imageUploadConfig.mainTextareaId
    );
});

// 이미지 업로드 기능 초기화 함수 (재사용 가능)
function initImageUpload(buttonId, fileInputSelector, attachedDivId) {
    const imageButton = document.getElementById(buttonId);
    const fileInput = document.querySelector(fileInputSelector);
    const attachedDiv = document.getElementById(attachedDivId);

    if (!imageButton || !fileInput || !attachedDiv) return;

    // 이미지 버튼 클릭 시 파일 선택창 열기
    imageButton.addEventListener('click', function(e) {
        e.preventDefault();
        fileInput.click();
    });

    // 파일 선택 시 업로드 처리
    fileInput.addEventListener('change', function(e) {
        if (!this.files || !this.files[0]) return;

        const file = this.files[0];
        uploadImage(file, attachedDiv);

        // 파일 입력 필드 초기화
        this.value = '';
    });
}

// 이미지 업로드 처리 함수
function uploadImage(file, attachedDiv) {
    // FormData 생성
    const formData = new FormData();
    formData.append('file', file);

    // 로딩 표시
    const loadingElement = document.createElement('div');
    loadingElement.className = 'loading';
    loadingElement.textContent = '업로드 중...';
    attachedDiv.appendChild(loadingElement);

    // AJAX로 서버에 업로드
    fetch(imageUploadConfig.uploadUrl, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('이미지 업로드에 실패했습니다.');
            }
            return response.json();
        })
        .then(data => {
            // 로딩 요소 제거
            attachedDiv.removeChild(loadingElement);

            // 업로드 성공 시 이미지 표시
            if (data && data.imageUrl) {
                addImageToForm(data.imageUrl, attachedDiv);
            } else {
                console.error('업로드된 이미지 URL이 없습니다:', data);
                alert('이미지 URL을 받지 못했습니다.');
            }
        })
        .catch(error => {
            handleUploadError(error, loadingElement, attachedDiv);
        });
}

// 이미지를 폼에 추가하는 함수
function addImageToForm(imageUrl, attachedDiv) {
    // hidden input 추가
    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'imageUrl';
    hiddenInput.value = imageUrl;
    hiddenInput.id = 'hidden-' + Date.now();
    attachedDiv.appendChild(hiddenInput);

    // 이미지 링크 추가
    const imageLink = document.createElement('a');
    imageLink.href = '#';
    imageLink.dataset.inputId = hiddenInput.id;
    imageLink.onclick = function(e) {
        e.preventDefault();
        removeImage(this);
    };

    const img = document.createElement('img');
    img.src = imageUrl;
    img.alt = imageUrl;

    const closeIcon = document.createElement('i');
    closeIcon.className = 'close fa fa-close';

    imageLink.appendChild(img);
    imageLink.appendChild(closeIcon);
    attachedDiv.appendChild(imageLink);
}

// 이미지 제거 함수
function removeImage(imageLink) {
    const inputId = imageLink.dataset.inputId;
    const inputToRemove = document.getElementById(inputId);
    if (inputToRemove) {
        inputToRemove.remove();
    }
    imageLink.remove();
}

// 업로드 오류 처리 함수
function handleUploadError(error, loadingElement, attachedDiv) {
    console.error('이미지 업로드 오류:', error);
    if (attachedDiv.contains(loadingElement)) {
        attachedDiv.removeChild(loadingElement);
    }

    const errorElement = document.createElement('div');
    errorElement.className = 'error';
    errorElement.textContent = '이미지 업로드에 실패했습니다.';
    attachedDiv.appendChild(errorElement);

    setTimeout(() => {
        if (attachedDiv.contains(errorElement)) {
            attachedDiv.removeChild(errorElement);
        }
    }, 3000);
}

// 폼 유효성 검사 초기화 함수
function initFormValidation(formId, textareaId) {
    const form = document.getElementById(formId);
    const textarea = document.getElementById(textareaId);

    if (!form || !textarea) return;

    form.addEventListener('submit', function(event) {
        // 텍스트 영역이 비어있는지 확인
        const textIsEmpty = !textarea.value.trim();

        // 이미지가 업로드되었는지 확인
        const attachedDiv = form.querySelector('.attached');
        const hasUploadedImages = attachedDiv && (
            attachedDiv.querySelector('input[name="imageUrl"]') ||
            attachedDiv.querySelector('img')
        );

        // 텍스트도 없고 이미지도 없는 경우에만 제출 방지
        if (textIsEmpty && !hasUploadedImages) {
            event.preventDefault();
            alert('내용을 입력해주세요');
            textarea.focus();
        }
    });
}