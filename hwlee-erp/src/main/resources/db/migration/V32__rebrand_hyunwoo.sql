-- V32: 리브랜딩 — 회사명 "현우" → "hyunwoo", 이메일 도메인 @hwlee-erp.example → @hyunwoo.com
--
-- 이미 적용된 시드(V8/V28/V31)는 Flyway 체크섬 보존을 위해 직접 수정하지 않고,
-- 이 전진(forward) 마이그레이션으로 기존 DB와 신규 환경 모두를 일괄 갱신한다.
-- (created_by/updated_by 감사 컬럼의 과거 username 은 '이력'이므로 의도적으로 건드리지 않는다.)

-- 1) 직원 이메일 도메인
UPDATE employee
   SET email = REPLACE(email, '@hwlee-erp.example', '@hyunwoo.com')
 WHERE email LIKE '%@hwlee-erp.example';

-- 2) 로그인 계정 (username = 이메일)
UPDATE app_user
   SET username = REPLACE(username, '@hwlee-erp.example', '@hyunwoo.com')
 WHERE username LIKE '%@hwlee-erp.example';

-- 3) 회사(본사) 명 — 현우전자 → hyunwoo전자
UPDATE department
   SET name = REPLACE(name, '현우', 'hyunwoo')
 WHERE name LIKE '%현우%';

-- 4) 품목명 — 현우 노트북/모니터 → hyunwoo 노트북/모니터
UPDATE item
   SET name = REPLACE(name, '현우', 'hyunwoo')
 WHERE name LIKE '%현우%';
