ALTER TABLE video.file_info ADD COLUMN u_id BIGINT UNSIGNED DEFAULT 0 COMMENT '归属id';
CREATE INDEX INDEX_U_ID ON video.file_info (u_id);