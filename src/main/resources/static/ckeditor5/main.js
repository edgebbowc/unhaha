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
	'eyJhbGciOiJFUzI1NiJ9.eyJleHAiOjE3NDYxNDM5OTksImp0aSI6IjdmZjUwYmViLTAzOWUtNGY3ZS1hODRjLTRlNWZlZmY5NTQwZSIsInVzYWdlRW5kcG9pbnQiOiJodHRwczovL3Byb3h5LWV2ZW50LmNrZWRpdG9yLmNvbSIsImRpc3RyaWJ1dGlvbkNoYW5uZWwiOlsiY2xvdWQiLCJkcnVwYWwiLCJzaCJdLCJ3aGl0ZUxhYmVsIjp0cnVlLCJsaWNlbnNlVHlwZSI6InRyaWFsIiwiZmVhdHVyZXMiOlsiKiJdLCJ2YyI6IjQ4MTlkNTlkIn0.b8tyOWdxf2hb5K1hNrIRvJEt6tfzw6dfl5UhzY5hihVqzk2IbPT76kxFuD0lyKTZHZVE9cj_h1fiLvgrd8bsGw';

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

			return fetch('/images/article', {
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

ClassicEditor.create(document.querySelector('#editor'), editorConfig).then(editor => {
	const wordCount = editor.plugins.get('WordCount');
	document.querySelector('#editor-word-count').appendChild(wordCount.wordCountContainer);

	return editor;
});
