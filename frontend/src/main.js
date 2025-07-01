import {
    ClassicEditor,
    Alignment,
    Autoformat,
    AutoImage,
    AutoLink,
    Autosave,
    BlockQuote,
    Bold,
    Essentials,
    FindAndReplace,
    FontBackgroundColor,
    FontColor,
    FontFamily,
    FontSize,
    GeneralHtmlSupport,
    Heading,
    Highlight,
    HorizontalLine,
    ImageBlock,
    ImageCaption,
    ImageInline,
    ImageInsert,
    ImageInsertViaUrl,
    ImageResize,
    ImageStyle,
    ImageTextAlternative,
    ImageToolbar,
    ImageUpload,
    Indent,
    Italic,
    Link,
    LinkImage,
    List,
    MediaEmbed,
    Mention,
    Paragraph,
    PasteFromOffice,
    RemoveFormat,
    SimpleUploadAdapter,
    SourceEditing,
    Strikethrough,
    Table,
    TableToolbar,
    TextTransformation,
    Underline,
    WordCount
} from 'ckeditor5';
import 'ckeditor5/ckeditor5.css';

const editorConfig = {
    toolbar: {
        items: [
            "fontFamily",
            "fontSize",
            "|",
            "imageUpload",
            "mediaEmbed",
            "link",
            "|",
            "bold",
            "underline",
            "italic",
            "strikethrough",
            "|",
            "fontColor",
            "fontBackgroundColor",
            "removeFormat",
            "|",
            "alignment",
            "numberedList",
            "bulletedList",
            "indent",
            "outdent",
            "|",
            "blockQuote",
            "horizontalLine",
            "undo",
            "redo",
            "sourceEditing",
        ],
    },
    plugins: [
        Alignment,
        Autoformat,
        AutoImage,
        AutoLink,
        Autosave,
        BlockQuote,
        Bold,
        Essentials,
        FindAndReplace,
        FontBackgroundColor,
        FontColor,
        FontFamily,
        FontSize,
        GeneralHtmlSupport,
        Heading,
        Highlight,
        HorizontalLine,
        ImageBlock,
        ImageCaption,
        ImageInline,
        ImageInsert,
        ImageInsertViaUrl,
        ImageResize,
        ImageStyle,
        ImageTextAlternative,
        ImageToolbar,
        ImageUpload,
        Indent,
        Italic,
        Link,
        LinkImage,
        List,
        MediaEmbed,
        Mention,
        Paragraph,
        PasteFromOffice,
        RemoveFormat,
        SimpleUploadAdapter,
        SourceEditing,
        Strikethrough,
        Table,
        TableToolbar,
        TextTransformation,
        Underline,
        WordCount
    ],
    extraPlugins: [CustomUploadPlugin],
    fontFamily: {
        supportAllValues: true
    },
    fontSize: {
        options: [10, 12, 14, 'default', 18, 20, 22],
        supportAllValues: true
    },
    htmlSupport: {
        allow: [
            {
                name: "div",
                attributes: {
                    id: true,
                },
                classes: true,
            },
            {
                name: "video",
                attributes: true,
                classes: true,
                styles: true,
            },
        ]
    },
    image: {
        upload: {
            types: ["jpeg", "png", "gif", "bmp", "webp", "tiff", "svg+xml"],
        },
        resizeUnit: "px",
        resizeOptions: [
            {
                name: "resizeImage:original",
                value: null,
                icon: "original",
            },
            {
                name: "resizeImage:400",
                value: "400",
                icon: "medium",
            },
            {
                name: "resizeImage:800",
                value: "800",
                icon: "large",
            },
        ],
        toolbar: [
            "resizeImage:400",
            "resizeImage:800",
            "resizeImage:original",
            "|",
            "imageStyle:inline",
            "imageStyle:block",
            "imageStyle:side",
        ],
    },

    language: 'ko',
    licenseKey: 'GPL',
    link: {
        addTargetToExternalLinks: true,
    },
    mention: {
        feeds: [
            {
                marker: '@',
                feed: [
                    /* See: https://ckeditor.com/docs/ckeditor5/latest/features/mentions.html */
                ]
            }
        ]
    },
};

const MAX_SINGLE_SIZE = 2 * 1024 * 1024; // 2MB

class CustomUploadAdapter  {
    constructor(loader) {
        this.loader = loader;
    }

    upload() {
        return this.loader.file.then(file => {
            // 단일 파일 제한
            if (file.size > MAX_SINGLE_SIZE) {
                return Promise.reject('첨부파일은 최대 2MB까지 등록 가능합니다');
            }

            const data = new FormData();
            data.append('upload', file);

            return fetch('/articles/images', {
                method: 'POST',
                body: data,
                credentials: 'include'
            })
                .then(response => response.json())
                .then(result => {
                    // 서버에서 온 결과 확인
                    console.log('서버 응답:', result);  // 디버깅용

                    // uploaded가 false인 경우 에러로 처리
                    if (!result.uploaded) {
                        return Promise.reject(result.error || '업로드 실패');
                    }

                    // 성공시 URL 반환
                    return {
                        default: result.url
                    };
                })
                .catch(error => {
                    console.error('에러 발생:', error);  // 디버깅용
                    const errorMessage = error instanceof Error ? error.message : error;
                    alert(errorMessage);
                    return Promise.reject(errorMessage);
                });
        });
    }

    abort() {
        // 업로드 중단 처리
    }
}

function CustomUploadPlugin(editor) {
    editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
        return new CustomUploadAdapter(loader);
    };
}

// 에디터 초기화 함수를 전역에 노출
window.createCKEditor = function(selector, customConfig = {}) {
    const finalConfig = { ...editorConfig, ...customConfig };

    return ClassicEditor.create(document.querySelector(selector), finalConfig)
        .then(editor => {
            // WordCount 플러그인 설정
            const wordCount = editor.plugins.get('WordCount');

            if (!wordCount) {
                console.error('WordCount plugin not found!');
                return editor;
            }

            // 초기 wordCount 계산 함수
            function updateInitialWordCount() {
                setTimeout(() => {
                    const content = editor.getData();
                    if (content) {
                        const tempDiv = document.createElement('div');
                        tempDiv.innerHTML = content;
                        const textContent = tempDiv.textContent || tempDiv.innerText || '';
                        const characterCount = textContent.length;

                        const countElement = document.getElementById('count');
                        if (countElement) {
                            countElement.textContent = characterCount;
                        }
                    }
                }, 100);
            }

            // 에디터 준비 완료 후 초기 카운트 설정
            editor.model.document.on('ready', () => {
                updateInitialWordCount();
            });

            setTimeout(() => {
                updateInitialWordCount();
            }, 200);

            // 이벤트 리스너 등록
            wordCount.on('update', (evt, stats) => {
                const countElement = document.getElementById('count');
                if (countElement && stats) {
                    const maxCount = 10000;
                    const currentCount = stats.characters || 0;

                    if (currentCount > maxCount) {
                        alert('본문은 10000자를 초과할 수 없습니다.');

                        let content = editor.getData();
                        const tempDiv = document.createElement('div');
                        tempDiv.innerHTML = content;
                        let textContent = tempDiv.textContent || tempDiv.innerText || '';
                        const targetLength = maxCount - 3;
                        const truncatedText = textContent.substring(0, targetLength);

                        editor.setData(`<p>${truncatedText}</p>`);
                        countElement.textContent = targetLength;
                    } else {
                        countElement.textContent = currentCount;
                    }
                } else {
                    console.error('Count element or stats not found');
                }
            });

            return editor;
        });
};