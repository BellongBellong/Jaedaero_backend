-- 기존 DB를 사용 중인 경우 한 번만 실행하세요.
-- 저장 값은 애플리케이션의 AES/GCM 암호화 값이며 평문이 아닙니다.
ALTER TABLE codef_institution_connection
    ADD COLUMN login_id_encrypted VARCHAR(1024) NULL COMMENT 'AES 암호화된 기관 로그인 ID' AFTER login_type,
    ADD COLUMN login_password_encrypted VARCHAR(1024) NULL COMMENT 'AES 암호화된 기관 로그인 비밀번호' AFTER login_id_encrypted,
    ADD COLUMN birth_date_encrypted VARCHAR(1024) NULL COMMENT 'AES 암호화된 생년월일' AFTER login_password_encrypted;
