# 🏋️ UNHAHA - 피트니스 커뮤니티 플랫폼

> **백엔드 개발자 포트폴리오 프로젝트**  
> Spring Boot 기반의 피트니스 커뮤니티 웹 애플리케이션

## 📋 목차

1. [🎯 프로젝트 소개](#-프로젝트-소개)
2. [⚡ 주요 기능](#-주요-기능)
3. [🛠️ 기술 스택](#️-기술-스택)
4. [📁 아키텍처](#-아키텍처)
5. [📖 API 문서](#-api-문서)
6. [🗄️ 데이터베이스 설계](#️-데이터베이스-설계)

## 🎯 프로젝트 소개

**UNHAHA**는 피트니스 애호가들을 위한 커뮤니티 플랫폼입니다. 사용자들이 운동 경험을 공유하고, 서로 동기부여하며, 피트니스 정보를 교환할 수 있는 공간을 제공합니다.

### 💡 개발 목적
**백엔드 개발자로서의 기술 역량과 실무 적용 능력을 증명**하기 위한 포트폴리오 프로젝트입니다. 피트니스 커뮤니티라는 도메인을 통해 실제 서비스 수준의 백엔드 시스템 개발 경험을 쌓고, Spring Boot 생태계 전반에 대한 깊이 있는 이해를 보여주고자 했습니다.

### 🎯 기술적 특징
- **확장 가능한 아키텍처**: 대용량 트래픽 처리 가능한 백엔드 시스템 설계
- **코드 품질**: 클린 코드, 테스트 코드, 문서화 등 실무 표준 적용
- **성능 최적화**: Redis를 활용한 조회수 관리, 쿼리 최적화 등


### 🎨 디자인 및 프론트엔드
- **UI/UX**: [chimhaha.net](http://chimhaha.net)의 디자인 시스템을 기반으로 피트니스 커뮤니티에 특화
- **프론트엔드**: 기존 CSS, HTML, JavaScript를 활용하여 백엔드 시스템과 통합

## ⚡ 주요 기능

### 🔐 사용자 관리
- **회원가입/로그인**: Spring Security + Session 기반 인증
- **OAuth 연동**: 네이버 소셜 로그인
- **권한 관리**: Role 기반 접근 제어 (RBAC)
- **프로필 관리**: 사용자 정보 및 프로필 이미지 관리
  - **비동기 프로필 이미지 업로드**: Google Cloud Storage 연동
  - **이전 이미지 자동 삭제**: 프로필 변경 시 기존 이미지 GCS에서 삭제
  - **URL 기반 저장**: 데이터베이스에는 이미지 URL 경로만 저장하여 효율성 확보 

### 📝 게시판 시스템
- **다중 게시판**: 보디빌딩, 파워리프팅, 크로스핏 등 카테고리별 분류
- **CRUD 기능**: 게시글 작성, 조회, 수정, 삭제
- **CKEditor5 리치 텍스트 에디터 적용**
- **이미지 업로드**: Google Cloud Storage 연동 파일 관리
  - **temp/active 상태 관리**: 임시 업로드된 이미지는 temp 상태, 게시글 제출 시 active로 전환
  - **파일 확장자 검증**: jpeg, jpg, png만 허용하여 보안 강화
  - **스케줄러 기반 정리**: 1분마다 temp 상태 이미지 자동 삭제로 스토리지 최적화
  - **게시글 수정 시 이미지 처리**: 기존 이미지 temp 전환, 새 이미지 active 전환
  - **게시글 삭제 시 연관 이미지 자동 삭제**: 데이터 정합성 보장

 
- **좋아요 기능**: 게시글 추천 기능
- **조회수 관리**: Redis를 활용한 고성능 중복 방지 조회수 시스템
  - **(비회원)IP 기반 중복 방지**: Redis의 `setIfAbsent` 명령어로 동일 IP의 24시간 내 중복 조회 차단
  - **회원 기반 중복 방지**: Redis Set 자료구조를 활용하여 로그인 사용자의 게시글별 중복 조회 방지
  - **TTL(Time To Live) 적용**: 86400초(24시간) 후 재조회 가능하도록 자동 만료 설정
  - **메모리 효율성**: Redis의 인메모리 특성으로 빠른 조회수 검증 및 업데이트
  - **확장 가능한 설계**: IP 주소와 회원 이메일을 기반으로 한 유연한 중복 체크 로직

### 💬 댓글 시스템
- **계층형 댓글**: 대댓글 지원 (부모-자식 관계)
- **이미지 첨부**: 댓글에 이미지 업로드 지원
  - **GCS 연동 이미지 업로드**: 댓글에 이미지 첨부 지원
  - **썸네일 제공**: 이미지 썸네일로 표시, 클릭 시 삭제 가능
  - **temp/active 상태 관리**: 댓글 작성 중 temp, 제출 시 active로 전환
  - **수정 시 이미지 처리**: 기존 이미지 중 미사용 이미지 temp 전환, 새 이미지 active 전환
- **페이징 시스템**:
  - **루트 댓글 기준 페이징**: 30개 이상 시 페이징 버튼 표시
  - **마지막 페이지 우선 표시**: 최신 댓글을 먼저 보여주는 UX
  - **대댓글 제외 페이징**: 대댓글은 페이징 개수에서 제외하여 정확한 페이징 구현
- **좋아요 기능**:
  - **CSS 효과 적용**: 좋아요 클릭 시 시각적 피드백


### 🔍 검색 & 필터링
- **전체 검색**: 제목, 내용, 작성자 통합 검색
- **카테고리 필터**: 게시판별 필터링
- **Querydsl 활용**: 복잡한 동적 쿼리 처리

### ⚙️ 시스템 최적화
- **스케줄러 시스템**:
  - **통합 이미지 정리**: 게시글 및 댓글의 temp 이미지 1분마다 자동 삭제
  - **스토리지 비용 최적화**: 불필요한 이미지 자동 제거로 GCS 비용 절감
- **비동기 처리**: 이미지 업로드 등 무거운 작업의 비동기 처리로 사용자 경험 향상
- **쿼리 최적화**: N+1 문제 해결 및 효율적인 데이터 조회

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 2.7.9
- **Java Version**: 11
- **Security**: Spring Security + Session
- **ORM**: Spring Data JPA + Hibernate
- **Database**: MySQL 8.0
- **Redis (조회수 중복 방지 전용)**
  - 조회수 중복 방지 (setIfAbsent, Set 자료구조)
  - TTL 기반 시간 제한 관리
- **Query DSL**: 복잡한 동적 쿼리 처리
- **Testing**: JUnit 5, Mockito, Testcontainers

### Frontend & UI
- **UI/UX Design**: [chimhaha.net](http://chimhaha.net) 기반
- **CSS**: chimhaha.net의 스타일시트 활용
- **HTML**: chimhaha.net의 마크업 구조 참조
- **JavaScript**: chimhaha.net의 클라이언트 스크립트 활용
- **Template Engine**: Thymeleaf

### Infrastructure
- **Build Tool**: Gradle
- **Cloud Storage**: Google Cloud Storage (이미지 업로드)
- **Database**: MySQL (운영), H2 (테스트)
- **Documentation**: Swagger/OpenAPI 3.0

### Development Tools
- **IDE**: IntelliJ IDEA
- **Version Control**: Git, GitHub
- **API Testing**: Postman
- **Database Tool**: MySQL Workbench

## 📁 아키텍처
### 🏗️ 전체 시스템 아키텍처
![Editor _ Mermaid Chart-2025-06-30-134127](https://github.com/user-attachments/assets/0e39ecac-6148-4b19-96e5-83798d8c1f4a)
### 🏗️ 3-Layer Architecture
![Editor _ Mermaid Chart-2025-06-30-110132](https://github.com/user-attachments/assets/806590c0-d402-4122-b1ea-3ff66d818d85)
### 🔄 주요 데이터 플로우
![Editor _ Mermaid Chart-2025-06-30-132816](https://github.com/user-attachments/assets/d275eb62-dab3-48a3-90b8-6f8164e3a519)
#### 📝 게시글 작성 플로우
![Editor _ Mermaid Chart-2025-06-30-114838](https://github.com/user-attachments/assets/0ac939c2-4236-4cab-b1f9-5418bcf96032)
#### 🔍 조회수 처리 플로우  
![Editor _ Mermaid Chart-2025-06-30-114639](https://github.com/user-attachments/assets/de7b0568-ab99-4ac5-b6b2-c61dd48c9e14)


### 주요 설계 원칙
- **Layered Architecture**: 계층별 책임 분리
- **Dependency Injection**: Spring IoC 컨테이너 활용
- **Domain-Driven Design**: 도메인 중심 설계
- **RESTful API**: REST 원칙 준수


# 📖 API 문서

### 주요 엔드포인트
#### 📝 게시글 API
- GET    /api/home                                     # 인기글 목록 조회
- GET    /api/articles/{boardType}                     # 게시글 목록 조회
- GET    /api/articles/{boardType}/{articleId}         # 게시글 상세 조회
- POST   /api/articles/{boardType}                     # 게시글 작성
- PUT    /api/articles/{boardType}/{articleId}         # 게시글 수정
- DELETE /api/articles/{boardType}/{articleId}         # 게시글 삭제
- POST   /api/articles/{boardType}/{articleId}/like    # 게시글 좋아요

#### 💬 댓글 API
- POST   /api/{boardType}/{articleId}/comments                     # 댓글 작성
- PUT    /api/{boardType}/{articleId}/comments/{commentId}         # 댓글 수정
- DELETE /api/{boardType}/{articleId}/comments/{commentId}         # 댓글 삭제
- POST   /api/{boardType}/{articleId}/comments/{commentId}/like    # 댓글 좋아요

#### 📷 이미지 API
- POST   /articles/images                # 게시글 이미지 업로드 (GCS)
- POST   /comments/images                # 댓글 이미지 업로드 (GCS)
- POST   /mypage/upload-profile-image    # 프로필 이미지 업로드 (GCS)

## 🗄️ 데이터베이스 설계
### ERD 다이어그램
![Editor _ Mermaid Chart-2025-06-30-103938](https://github.com/user-attachments/assets/b012aa82-84aa-4519-a927-e8852a050083)

### 주요 관계 설명
- **User ↔ Article**: 1:N (한 사용자가 여러 게시글 작성)
- **Article ↔ Comment**: 1:N (한 게시글에 여러 댓글)
- **Comment ↔ Comment**: 1:N (계층형 댓글 - 부모/자식 관계)
- **Article ↔ ArticleImage**: 1:N (게시글당 여러 이미지)
- **Comment ↔ CommentImage**: 1:N (댓글당 여러 이미지)
- **User ↔ Like**: M:N (사용자-좋아요 관계, 중간 테이블 활용)

