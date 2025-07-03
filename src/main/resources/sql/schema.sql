-- ====================================
-- 기존 테이블 삭제 (순서 중요 - 외래키 역순)
-- ====================================

DROP TABLE IF EXISTS user_like_comment;
DROP TABLE IF EXISTS user_like_article;
DROP TABLE IF EXISTS comment_image;
DROP TABLE IF EXISTS article_image;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS user;

-- ====================================
-- MySQL용 게시판 시스템 데이터베이스 스키마
-- ====================================

-- 1. 사용자 테이블
CREATE TABLE user
(
    user_id            BIGINT       NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    email              VARCHAR(255) NOT NULL UNIQUE,
    nickname           VARCHAR(255) NOT NULL UNIQUE,
    store_file_url     VARCHAR(255),
    upload_file_name   VARCHAR(255),
    role               VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id)
);

-- 2. 게시글 테이블
CREATE TABLE article
(
    article_id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    board              VARCHAR(255),
    content            TEXT,
    like_achieved_at   TIMESTAMP,
    like_count         INT DEFAULT 0,
    title              VARCHAR(255) NOT NULL,
    view_count         INT DEFAULT 0,
    user_id            BIGINT,
    PRIMARY KEY (article_id),
    CONSTRAINT fk_article_user FOREIGN KEY (user_id) REFERENCES user (user_id)
);

-- 3. 댓글 테이블
CREATE TABLE comment
(
    comment_id         BIGINT NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    content            TEXT   NOT NULL,
    like_count         INT DEFAULT 0,
    article_id         BIGINT,
    parent_id          BIGINT,
    user_id            BIGINT,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (comment_id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES user (user_id)
);

-- 4. 게시글 이미지 테이블
CREATE TABLE article_image
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    status             VARCHAR(255) NOT NULL,
    url                VARCHAR(255) NOT NULL,
    article_id         BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_article_image_article FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    CONSTRAINT chk_article_image_status CHECK (status IN ('TEMP', 'ACTIVE'))
);

-- 5. 댓글 이미지 테이블
CREATE TABLE comment_image
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    status             VARCHAR(255) NOT NULL,
    url                VARCHAR(255) NOT NULL,
    comment_id         BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_image_comment FOREIGN KEY (comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    CONSTRAINT chk_comment_image_status CHECK (status IN ('TEMP', 'ACTIVE'))
);

-- 6. 사용자-게시글 좋아요 관계 테이블
CREATE TABLE user_like_article
(
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    article_id         BIGINT,
    user_id            BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_like_article_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_like_article_article FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    CONSTRAINT uk_user_like_article UNIQUE (user_id, article_id)
);

-- 7. 사용자-댓글 좋아요 관계 테이블
CREATE TABLE user_like_comment
(
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    comment_id         BIGINT,
    user_id            BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_like_comment_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_like_comment_comment FOREIGN KEY (comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    CONSTRAINT uk_user_like_comment UNIQUE (user_id, comment_id)
);

-- ====================================
-- 인덱스 생성 (성능 최적화)
-- ====================================

CREATE INDEX idx_user_email ON user (email);
CREATE INDEX idx_user_nickname ON user (nickname);
CREATE INDEX idx_article_board ON article (board);
CREATE INDEX idx_article_user_id ON article (user_id);
CREATE INDEX idx_article_created_date ON article (created_date);
CREATE INDEX idx_comment_article_id ON comment (article_id);
CREATE INDEX idx_comment_user_id ON comment (user_id);
CREATE INDEX idx_comment_parent_id ON comment (parent_id);


