-- db/init.sql
-- 이 파일은 애플리케이션이 자동 실행하지 않는다. psql로 수동 적용한다.
-- 대상: 로컬 PostgreSQL(postgres 데이터베이스 내 todolist_db 스키마)

-- 1. 스키마 생성 (Phase 1)
CREATE SCHEMA IF NOT EXISTS todolist_db;

-- 2. 최초 관리자 지정 (FR-M03)
-- 회원가입(Phase 2)으로 먼저 계정을 만든 뒤, 그 계정의 이메일로 아래 UPDATE를 실행해
-- role을 ADMIN으로 승격한다. 일반 API 경로로는 ADMIN이 될 수 없다.
-- <ADMIN_EMAIL>을 실제 가입 이메일로 치환한 뒤 주석을 해제해 실행한다.
--
-- UPDATE todolist_db.users
-- SET role = 'ADMIN'
-- WHERE email = '<ADMIN_EMAIL>'
--   AND deleted_at IS NULL;
