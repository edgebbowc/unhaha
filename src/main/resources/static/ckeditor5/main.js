/**
 * This configuration was generated using the CKEditor 5 Builder. You can modify it anytime using this link:
 * https://ckeditor.com/ckeditor-5/builder/#installation/NoNgNARATAdArDADBSJGICwE4DMGNQ5T5wCMUpA7ImhiLgBxYNRy5xxMgoQDWA9ikRhgpMMOFjJAXUgNKGBgwBGUCNKA
 */

const {
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
} = window.CKEDITOR;

const LICENSE_KEY =
	'eyJhbGciOiJFUzI1NiJ9.eyJleHAiOjE3NzY0NzAzOTksImp0aSI6IjkyOTA1OTRhLTY3ZmYtNDY5Zi04YWQxLTY5MThiZWM2NjY5MyIsImxpY2Vuc2VkSG9zdHMiOlsiMTI3LjAuMC4xIiwibG9jYWxob3N0IiwiMTkyLjE2OC4qLioiLCIxMC4qLiouKiIsIjE3Mi4qLiouKiIsIioudGVzdCIsIioubG9jYWxob3N0IiwiKi5sb2NhbCJdLCJ1c2FnZUVuZHBvaW50IjoiaHR0cHM6Ly9wcm94eS1ldmVudC5ja2VkaXRvci5jb20iLCJkaXN0cmlidXRpb25DaGFubmVsIjpbImNsb3VkIiwiZHJ1cGFsIl0sImxpY2Vuc2VUeXBlIjoiZGV2ZWxvcG1lbnQiLCJmZWF0dXJlcyI6WyJEUlVQIl0sInZjIjoiYmVhODk4YjEifQ._RwTGYck3hLvdRTzURPiobt1JtEkzD4w96LeGDob5rcF00nRj8W0t23_6RR5ikNOa_O5iuI8Gm2wLwhU7T1Zuw';

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
	licenseKey: LICENSE_KEY,
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

// ClassicEditor.create(document.querySelector('#editor'), editorConfig).then(editor => {
// 	const wordCount = editor.plugins.get('WordCount');
// 	document.querySelector('#editor-word-count').appendChild(wordCount.wordCountContainer);
//
// 	return editor;
// });
ClassicEditor.create(document.querySelector('#editor'), editorConfig).then(editor => {
	// WordCount 플러그인 가져오기
	const wordCount = editor.plugins.get('WordCount');

	if (!wordCount) {
		console.error('WordCount plugin not found!');
		return;
	}
	// 초기 wordCount 계산 함수
	function updateInitialWordCount() {
		setTimeout(() => {
			const content = editor.getData();
			if (content) {
				// HTML 태그 제거 후 순수 텍스트 추출
				const tempDiv = document.createElement('div');
				tempDiv.innerHTML = content;
				const textContent = tempDiv.textContent || tempDiv.innerText || '';
				const characterCount = textContent.length;

				const countElement = document.getElementById('count');
				if (countElement) {
					countElement.textContent = characterCount;
				}
			}
		}, 100); // 에디터 데이터 로딩 후 실행
	}

	// 에디터 준비 완료 후 초기 카운트 설정
	editor.model.document.on('ready', () => {
		updateInitialWordCount();
	});

	// 데이터 변경 시에도 초기 카운트 설정 (안전장치)
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

				// 현재 에디터 내용 가져오기
				let content = editor.getData();

				// HTML 태그 제거 후 순수 텍스트만 추출
				const tempDiv = document.createElement('div');
				tempDiv.innerHTML = content;
				let textContent = tempDiv.textContent || tempDiv.innerText || '';

				// 초과된 글자 수 - 3 계산
				const targetLength = maxCount - 3; // 9997자로 설정

				// 텍스트를 목표 길이로 자르기
				const truncatedText = textContent.substring(0, targetLength);

				// 에디터에 잘린 텍스트 다시 설정
				editor.setData(`<p>${truncatedText}</p>`);

				// 글자 수 업데이트
				countElement.textContent = targetLength;
			} else {
				countElement.textContent = currentCount;
			}
		} else {
			console.error('Count element or stats not found');
		}
	});

	return editor;
}).catch(error => {
	console.error('CKEditor initialization failed:', error);
});