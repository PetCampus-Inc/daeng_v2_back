> 생성: 2026-08-02 13:45 · 최종 수정: 2026-08-30 23:04

# 데이터 인벤토리

이 문서는 레거시/초안 저장소 객체(MySQL 테이블, Redis 키, 파일/스토리지 데이터 등)의 사용처와 신규 서버 이관 판정을 관리한다. 스키마 변경 원칙은 [`docs/rules/database-change.md`](../rules/database-change.md)를 따른다.

## 1. 판정 기준

| 판정 | 의미 |
|---|---|
| `KEEP` | 신규 서버에서도 동일한 데이터 개념이 필요하고 계약상 보존해야 하는 데이터 |
| `REDESIGN` | 데이터 개념은 필요하지만 테이블/키/제약/인덱스 구조를 재설계할 데이터 |
| `DROP` | 미사용, 중복, 위험, 보존 가치가 낮아 신규 저장소로 이관하지 않을 데이터 |
| `DEFER` | 사용 여부, 보존 기준, 신규 모델이 아직 불확실한 데이터 |

## 2. 작성 규칙

- 레거시 테이블 구조나 ERDCloud 초안을 신규 스키마로 그대로 옮긴다고 가정하지 않는다.
- 데이터 보존/삭제 기준이 불확실하면 `DEFER`로 둔다.
- 신규 스키마 확정은 구현 슬라이스별 Flyway migration과 함께 고정한다.
- row count, checksum, 샘플 비교 등 검증 기준이 필요한 데이터는 후속 확인에 명시한다.
- `DROP`은 프론트 호출, 운영 필요성, 보존 의무를 확인한 뒤 확정한다.

## 3. ERDCloud 추출 범위

| 항목 | 내용 |
|---|---|
| 원본 | `../똑독 V3-snapshot.json` |
| 추출 시점 | 2026-08-04 00:43 KST |
| 테이블 수 | 29개 |
| 용도 | 데이터 객체 판정 초안. 최종 스키마/DDL 아님 |

## 4. 인벤토리

| 저장소 | 객체 | ERDCloud 표시명 | 사용 도메인 | 소유 도메인 후보 | 판정 | 신규 방향 | 상세 확정 시점 | 후속 확인 |
|---|---|---|---|---|---|---|---|---|
| MySQL | `user` | 사용자 | auth/user | auth/user | `REDESIGN` | 회원 기본 정보 후보. 신규 식별자, 상태, 권한 모델 재확정 | auth/user 슬라이스 | 소셜 계정, 탈퇴, 주소, 알림 설정과의 소유권 분리 |
| MySQL | `social_user` | 소셜 | auth | auth/user | `REDESIGN` | 소셜 계정 연결 데이터 후보 | auth 슬라이스 | provider 식별자 unique, 재연결 정책, 회원 삭제 시 처리 |
| MySQL | `withdraw_reason` | 탈퇴 사유 | auth/user | auth/user | `REDESIGN` | 탈퇴 이력/사유 데이터 후보 | auth/user 슬라이스 | 보존 기간, 개인정보 삭제 정책 |
| MySQL | `user_address` | 사용자 주소 | user/mypage | user/mypage | `REDESIGN` | 사용자 저장 주소 후보. address 검색/좌표 변환 API와 별도 | mypage/address 슬라이스 (`POST /api/v0/mypage/address`) | HOME 주소 필수 여부, 좌표 저장 여부, 주소 타입 |
| MySQL | `user_notification_setting` | user_notification_setting | user/notification | auth/user | `REDESIGN` | 사용자 알림 설정 후보 | notification 또는 mypage 슬라이스 | push/email 설정 범위, 기본값 |
| MySQL | `pet` | 반려견 | pet | pet | `REDESIGN` | 반려견 핵심 데이터 후보 | pet 슬라이스 | 보호자 관계, 대표 반려견, 삭제 정책 |
| MySQL | `tb_breed` | 견종 | pet/reference | pet | `DEFER` | 견종 기준 데이터 후보 | pet 슬라이스 | seed source, 운영 수정 여부, 레거시 코드 매핑 |
| MySQL | `bookmark` | 북마크 | bookmark | bookmark/comparison | `REDESIGN` | 사용자-유치원 북마크 후보 | bookmark 슬라이스 | target이 school 고정인지, 중복 unique |
| MySQL | `comparison_history` | 비교 내역 | comparison | bookmark/comparison | `DEFER` | 유치원 비교 이력 후보 | comparison 슬라이스 | 기능 유지 여부, 보존 기간 |
| MySQL | `tb_school` | 유치원 | school | school/owner | `REDESIGN` | 유치원 핵심 데이터 후보 | school/owner 슬라이스 | Redis 유치원 데이터와의 역할 분리, placeId 매핑 |
| MySQL | `tb_school_profile` | 유치원 프로필 | school/owner | school/owner | `REDESIGN` | 유치원 상세 프로필 후보 | owner-school-profile 슬라이스 | 영업시간, 주소, 좌표, 공개 상태 |
| MySQL | `tb_school_profile_image` | 유치원 프로필 이미지 | school/owner/media | school/owner/media | `REDESIGN` | 유치원 프로필 이미지 후보 | owner-school-profile 슬라이스 | S3 key 소유권, 정렬 순서 |
| MySQL | `tb_school_price_image` | 유치원 가격표 이미지 | school/owner/media | school/owner/media | `REDESIGN` | 가격표 이미지 후보 | owner-school-price 슬라이스 | S3 key 소유권, 단일/다중 이미지 정책 |
| MySQL | `tb_school_profile_option` | 유치원 프로필 옵션 | school/owner | school/owner | `DEFER` | 유치원 프로필 옵션 후보 | owner-school-profile 슬라이스 | 옵션 목록이 enum인지 별도 기준 데이터인지 확인 |
| MySQL | `tb_school_business_registration` | 유치원 사업자등록 내역 | business-registration/school | owner-verification/business-registration | `REDESIGN` | 사업자등록 검증 이력 후보 | business-registration 슬라이스 | 검증 결과 보존, created_by 관계 |
| MySQL | `tb_owner_verification` | 원장 권한 신청 내역 | owner-verification | owner-verification/business-registration | `REDESIGN` | 원장 권한 신청/검증 후보 | owner-verification 슬라이스 | 수동 유치원 선택, 신청 상태, 중복 신청 정책 |
| MySQL | `tb_user_school_role` | 사용자 유치원 권한 | school/owner/authz | owner/authz | `REDESIGN` | 사용자-유치원 권한 후보 | owner/school-role 슬라이스 | owner/teacher/member 권한 범위, role enum |
| MySQL | `school_invite` | 유치원 초대 | owner | owner-member | `REDESIGN` | 유치원 초대 코드/토큰 후보 | owner-member 슬라이스 | 만료, 1회성 여부, 초대 대상 |
| MySQL | `school_pet_membership` | 유치원 반려견 연결 내역 | owner/pet/school | owner-member | `REDESIGN` | 반려견-유치원 연결 후보 | owner-member 또는 pet-school 슬라이스 | 승인 상태, 보호자 관계, 초대 이력 |
| MySQL | `attendance_record` | 알림장 | attendance | attendance | `REDESIGN` | 알림장/출석 기록 후보 | attendance 슬라이스 | 작성자 권한, 반려견/유치원 관계, 발송 상태 |
| MySQL | `attendance_record_note_template` | 알림장 템플릿 | attendance | attendance | `REDESIGN` | 알림장 템플릿 후보 | attendance-template 슬라이스 | 유치원별 템플릿 소유권, 삭제 정책 |
| MySQL | `free_memo` | 자유메모 | memo | memo | `REDESIGN` | 유치원/사용자 메모 후보 | memo 슬라이스 | user_id 의미, 대상 school/pet 연결 여부 |
| MySQL | `free_memo_photo` | 자유메모 사진 | memo/media | memo | `REDESIGN` | 자유메모 첨부 이미지 후보 | memo 슬라이스 | S3 key 소유권, 정렬, 삭제 정책 |
| MySQL | `checklist_template` | 체크리스트 템플릿 | checklist | checklist | `REDESIGN` | 체크리스트 템플릿 후보 | checklist 슬라이스 | 템플릿 버전, 유치원별 소유 여부 |
| MySQL | `checklist_section` | 체크리스트 섹션 | checklist | checklist | `REDESIGN` | 체크리스트 섹션 후보 | checklist 슬라이스 | 정렬 순서, 템플릿 삭제 시 처리 |
| MySQL | `checklist_question` | 체크리스트 질문 | checklist | checklist | `REDESIGN` | 체크리스트 질문 후보 | checklist 슬라이스 | 질문 타입, 필수 여부, 정렬 |
| MySQL | `question_option` | 체크리스트 질문 옵션 | checklist | checklist | `REDESIGN` | 체크리스트 선택지 후보 | checklist 슬라이스 | 단일/다중 선택, 정렬 |
| MySQL | `checklist_submission` | 체크리스트 제출 내역 | checklist | checklist | `REDESIGN` | 체크리스트 제출 이력 후보 | checklist 슬라이스 | 제출자, 템플릿 버전 스냅샷 필요 여부 |
| MySQL | `checklist_answer` | 체크리스트 답변 | checklist | checklist | `REDESIGN` | 체크리스트 답변 후보 | checklist 슬라이스 | 답변 타입별 저장 방식 |

## 5. Cross-domain 위험 후보

| 위험 | 관련 객체 | 확인 방향 |
|---|---|---|
| 탈퇴/삭제가 여러 도메인 데이터에 전파됨 | `user`, `social_user`, `pet`, `user_address`, `school_pet_membership`, `attendance_record`, `free_memo`, `checklist_submission` | auth/user 슬라이스에서 전체 삭제를 직접 구현하지 않고, 도메인별 보존/익명화 정책을 먼저 정한다 |
| 원장 권한 판단이 여러 기능의 선행 조건이 됨 | `tb_user_school_role`, `tb_school`, `user` | owner/authz 소유 포트를 만들고 다른 도메인은 직접 테이블을 수정하지 않는다 |
| 반려견-유치원 연결이 보호자, 반려견, 유치원을 동시에 묶음 | `school_pet_membership`, `school_invite`, `pet`, `user`, `tb_school` | owner-member 슬라이스에서 상태값과 unique 기준을 먼저 확정한다 |
| 유치원 프로필과 Redis/외부 검색 데이터 역할이 겹침 | `tb_school`, `tb_school_profile`, `tb_school_profile_option` | 신규 DB 저장 데이터와 Redis 캐시/검색 데이터의 출처를 분리한다 |
| 이미지 데이터가 DB와 S3 수명주기를 함께 가짐 | `tb_school_profile_image`, `tb_school_price_image`, `free_memo_photo` | DB 삭제와 S3 object 삭제/보존/정렬 정책을 함께 정한다 |
| 체크리스트 템플릿 변경이 과거 제출 답변에 영향 | `checklist_template`, `checklist_submission`, `checklist_answer` | 제출 시 템플릿 버전 스냅샷 필요 여부를 확정한다 |
| 사업자/대표자/주소 정보에 개인정보와 검증 이력이 포함됨 | `tb_owner_verification`, `tb_school_business_registration`, `tb_school` | 보존 기간, 마스킹, 철회 후 접근 정책을 정한다 |
