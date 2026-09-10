-- 상담 체크리스트 답변 (KD3-465). 레거시 checklist_submission + checklist_answer(폴리모픽 4컬럼)를
-- (user_code, target_id) 1행 + answers JSON으로 재설계. 항상 유저·유치원 단위로 통째로 읽고 쓴다.
-- 템플릿(checklist_template/section/question/question_option)은 미이관 — resources/checklists/*.json 정적.
CREATE TABLE checklist_submissions (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code         VARCHAR(8)   NOT NULL,
  target_id         VARCHAR(100) NOT NULL,
  template_version  VARCHAR(50)  NOT NULL,
  answers           TEXT         NOT NULL,   -- {questionCode: value} JSON 문자열. 통째로만 읽고 써서 JSON 타입 불필요
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  deleted_at        DATETIME(6),
  UNIQUE (user_code, target_id)
);
