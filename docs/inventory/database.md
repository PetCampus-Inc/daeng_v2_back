> 생성: 2026-08-02 13:45 · 최종 수정: 2026-09-04 15:30

# 데이터 인벤토리

이 문서는 레거시/초안 저장소 객체(MySQL 테이블, Redis 키, 파일/스토리지 데이터 등)의 사용처와 신규 서버 이관 판정을 관리한다. 스키마 변경 원칙은 [`docs/rules/database-change.md`](../rules/database-change.md)를 따른다.

## 1. 판정 기준

| 판정 | 의미 |
|---|---|
| `KEEP` | 신규 서버에서도 동일한 데이터 개념이 필요하고 계약상 보존해야 하는 데이터 |
| `REDESIGN` | 데이터 개념은 필요하지만 테이블/키/제약/인덱스 구조를 재설계할 데이터 |
| `DROP` | 미사용, 중복, 위험, 보존 가치가 낮아 신규 저장소로 이관하지 않을 데이터 |
| `DEFER` | 사용 여부, 보존 기준, 신규 모델이 아직 불확실한 데이터 |

## 2. 이관 진척

`판정`이 "무엇을 할 것인가"라면 `이관 진척`은 "어디까지 했는가"다. 둘을 한 열에 섞지 않는다.

| 값 | 의미 |
|---|---|
| `완료` | 신규 서버에 반영되고 검증까지 끝난 항목 |
| `진행중` | 일부만 반영됐거나 검증이 남은 항목 |
| `미착수` | 아직 손대지 않은 항목. `DEFER`도 판정이 확정되면 대상이 될 수 있으므로 여기 둔다 |
| `해당없음` | `DROP` 판정이라 신규 서버로 가져가지 않는 항목 |

`완료`는 실제 코드와 검증 결과가 있을 때만 적는다. 어느 작업에서 처리했는지는 그 행에 `work/` 링크로 남긴다.

## 3. 작성 규칙

- 레거시 테이블 구조나 ERDCloud 초안을 신규 스키마로 그대로 옮긴다고 가정하지 않는다.
- 데이터 보존/삭제 기준이 불확실하면 `DEFER`로 둔다.
- 신규 스키마 확정은 구현 슬라이스별 Flyway migration과 함께 고정한다.
- row count, checksum, 샘플 비교 등 검증 기준이 필요한 데이터는 후속 확인에 명시한다.
- `DROP`은 프론트 호출, 운영 필요성, 보존 의무를 확인한 뒤 확정한다.
- 인벤토리에서 제외한 객체는 제외 근거를 6절에 남긴다. 목록에 없는 것과 판정이 없는 것을 구분할 수 없으면 다음 사람이 다시 조사하게 된다.
- 레거시는 계속 변경되므로, 재추출할 때마다 4절에 기준 커밋 SHA와 날짜를 갱신한다.

## 4. 추출 범위와 교차 확인

| 구분 | 원본 | 기준 시점 | 결과 |
|---|---|---|---|
| ERDCloud 초안 | `../똑독 V3-snapshot.json` | 2026-08-04 00:43 KST | 테이블 29개. 데이터 객체 판정 초안이며 최종 스키마/DDL 아님 |
| 레거시 JPA 엔티티 | `PetCampus-Inc/daeng_v1_back` `dev@2479b02c` (2026-08-30)의 `@Entity` 클래스 | 2026-08-31 | 엔티티 59개. ERDCloud 초안 29개 전부 실재하며, V3 계열 13개가 초안 이후 추가됨 |
| 레거시 스키마 변경 이력 | 같은 커밋의 `scripts/migrations/*.sql` (22개) | 2026-08-31 | 컬럼/제약/인덱스와 백필 정책의 근거. Flyway가 아니라 배포 스크립트가 매 배포마다 재실행하는 수동 SQL |

레거시 엔티티 59개 = ERDCloud 초안 29개 + V3 계열 신규 13개 + 레거시 v1/v2 잔존 17개(6절 참고).

## 5. 인벤토리

| 저장소 | 객체 | ERDCloud 표시명 | 사용 도메인 | 소유 도메인 후보 | 판정 | 이관 진척 | 신규 방향 | 상세 확정 시점 | 후속 확인 |
|---|---|---|---|---|---|---|---|---|---|
| MySQL | `user` | 사용자 | auth/user | auth/user | `REDESIGN` | `완료` | 회원 기본 정보 후보. 신규 식별자, 상태, 권한 모델 재확정 | auth/user 슬라이스 | 소셜 계정, 탈퇴, 주소, 알림 설정과의 소유권 분리 |
| MySQL | `social_user` | 소셜 | auth | auth/user | `REDESIGN` | `완료` | 소셜 계정 연결 데이터 후보 | auth 슬라이스 | provider 식별자 unique, 재연결 정책, 회원 삭제 시 처리 |
| MySQL | `withdraw_reason` | 탈퇴 사유 | auth/user | auth/user | `REDESIGN` | `미착수` | 탈퇴 이력/사유 데이터 후보 | auth/user 슬라이스 | 보존 기간, 개인정보 삭제 정책 |
| MySQL | `user_address` | 사용자 주소 | user/mypage | user/mypage | `REDESIGN` | `완료` | 사용자 저장 주소 후보. address 검색/좌표 변환 API와 별도 | mypage/address 슬라이스 (`POST /api/v0/mypage/address`) | HOME 주소 필수 여부, 좌표 저장 여부, 주소 타입 |
| MySQL | `user_notification_setting` | user_notification_setting | user/notification | notification | `REDESIGN` | `미착수` | 사용자 알림 설정 후보. 레거시는 KD3-287에서 이 테이블을 건드리지 않고 `notification_preference`를 새로 만들어 두 개가 공존한다 | notification 슬라이스 | 두 테이블 중 어느 쪽이 진실인지 확정하고 신규 서버에서는 하나로 합친다 |
| MySQL | `pet` | 반려견 | pet | pet | `REDESIGN` | `진행중` | 반려견 핵심 데이터 후보 | pet 슬라이스 ([`KD3-430`](../work/KD3-430-pet-domain-foundation-schema.md)) | HTTP API·유스케이스 미구현(KD3-420~423), 기존 데이터 backfill 미착수 |
| MySQL | `pets` | (초안 없음) | pet | pet | `REDESIGN` | `진행중` | 신규 스키마. 소유자·breed_id는 다른 도메인 애그리게잇에 대한 느슨한 참조(FK 제약 없음). 대표견 단일성은 `representative_user_id`(nullable, UNIQUE) 컬럼으로 DB가 보장 | pet 슬라이스 ([`KD3-430`](../work/KD3-430-pet-domain-foundation-schema.md)) | 활성 pet 0건 상태의 동시 등록(첫 pet 경쟁) 시 최대 5마리 보장은 MySQL에서 미검증 |
| MySQL | `tb_breed` | 견종 | pet/reference | pet | `DROP` | `해당없음` | 레거시가 KD3-370에서 `breed_catalog`를 신설해 대체했고 프론트도 `GET /api/v0/breed-catalog`로 이전 | - | 잔존 참조가 없는지 확인 후 삭제. 기준 데이터는 `breed_catalog`로 단일화 |
| MySQL | `bookmark` | 북마크 | bookmark | bookmark/comparison | `REDESIGN` | `미착수` | 사용자-유치원 북마크 후보 | bookmark 슬라이스 | target이 school 고정인지, 중복 unique |
| MySQL | `comparison_history` | 비교 내역 | comparison | bookmark/comparison | `DEFER` | `미착수` | 유치원 비교 이력 후보 | comparison 슬라이스 | 기능 유지 여부, 보존 기간 |
| MySQL | `tb_school` | 유치원 | school | school/owner | `REDESIGN` | `미착수` | 유치원 핵심 데이터 후보 | school/owner 슬라이스 | Redis 유치원 데이터와의 역할 분리, placeId 매핑 |
| MySQL | `tb_school_profile` | 유치원 프로필 | school/owner | school/owner | `REDESIGN` | `미착수` | 유치원 상세 프로필 후보 | owner-school-profile 슬라이스 | 영업시간, 주소, 좌표, 공개 상태 |
| MySQL | `tb_school_profile_image` | 유치원 프로필 이미지 | school/owner/media | school/owner/media | `REDESIGN` | `미착수` | 유치원 프로필 이미지 후보 | owner-school-profile 슬라이스 | S3 key 소유권, 정렬 순서 |
| MySQL | `tb_school_price_image` | 유치원 가격표 이미지 | school/owner/media | school/owner/media | `REDESIGN` | `미착수` | 가격표 이미지 후보 | owner-school-price 슬라이스 | S3 key 소유권, 단일/다중 이미지 정책 |
| MySQL | `tb_school_profile_option` | 유치원 프로필 옵션 | school/owner | school/owner | `DEFER` | `미착수` | 유치원 프로필 옵션 후보 | owner-school-profile 슬라이스 | 옵션 목록이 enum인지 별도 기준 데이터인지 확인 |
| MySQL | `tb_school_business_registration` | 유치원 사업자등록 내역 | business-registration/school | owner-verification/business-registration | `REDESIGN` | `미착수` | 사업자등록 검증 이력 후보 | business-registration 슬라이스 | 검증 결과 보존, created_by 관계 |
| MySQL | `tb_owner_verification` | 원장 권한 신청 내역 | owner-verification | owner-verification/business-registration | `REDESIGN` | `미착수` | 원장 권한 신청/검증 후보 | owner-verification 슬라이스 | 수동 유치원 선택, 신청 상태, 중복 신청 정책 |
| MySQL | `tb_user_school_role` | 사용자 유치원 권한 | school/owner/authz | owner/authz | `REDESIGN` | `미착수` | 사용자-유치원 권한 후보 | owner/school-role 슬라이스 | owner/teacher/member 권한 범위, role enum |
| MySQL | `school_invite` | 유치원 초대 | owner | owner-member | `REDESIGN` | `미착수` | 유치원 초대 코드/토큰 후보 | owner-member 슬라이스 | 만료, 1회성 여부, 초대 대상 |
| MySQL | `school_pet_membership` | 유치원 반려견 연결 내역 | owner/pet/school | owner-member | `REDESIGN` | `미착수` | 반려견-유치원 연결 후보 | owner-member 또는 pet-school 슬라이스 | 승인 상태, 보호자 관계, 초대 이력 |
| MySQL | `attendance_record` | 알림장 | attendance | attendance | `REDESIGN` | `미착수` | 알림장/출석 기록 후보 | attendance 슬라이스 | 작성자 권한, 반려견/유치원 관계, 발송 상태 |
| MySQL | `attendance_record_note_template` | 알림장 템플릿 | attendance | attendance | `REDESIGN` | `미착수` | 알림장 템플릿 후보 | attendance-template 슬라이스 | 유치원별 템플릿 소유권, 삭제 정책 |
| MySQL | `free_memo` | 자유메모 | memo | memo | `REDESIGN` | `미착수` | 유치원/사용자 메모 후보 | memo 슬라이스 | user_id 의미, 대상 school/pet 연결 여부 |
| MySQL | `free_memo_photo` | 자유메모 사진 | memo/media | memo | `REDESIGN` | `미착수` | 자유메모 첨부 이미지 후보 | memo 슬라이스 | S3 key 소유권, 정렬, 삭제 정책 |
| MySQL | `checklist_template` | 체크리스트 템플릿 | checklist | checklist | `REDESIGN` | `미착수` | 체크리스트 템플릿 후보 | checklist 슬라이스 | 템플릿 버전, 유치원별 소유 여부 |
| MySQL | `checklist_section` | 체크리스트 섹션 | checklist | checklist | `REDESIGN` | `미착수` | 체크리스트 섹션 후보 | checklist 슬라이스 | 정렬 순서, 템플릿 삭제 시 처리 |
| MySQL | `checklist_question` | 체크리스트 질문 | checklist | checklist | `REDESIGN` | `미착수` | 체크리스트 질문 후보 | checklist 슬라이스 | 질문 타입, 필수 여부, 정렬 |
| MySQL | `question_option` | 체크리스트 질문 옵션 | checklist | checklist | `REDESIGN` | `미착수` | 체크리스트 선택지 후보 | checklist 슬라이스 | 단일/다중 선택, 정렬 |
| MySQL | `checklist_submission` | 체크리스트 제출 내역 | checklist | checklist | `REDESIGN` | `미착수` | 체크리스트 제출 이력 후보 | checklist 슬라이스 | 제출자, 템플릿 버전 스냅샷 필요 여부 |
| MySQL | `checklist_answer` | 체크리스트 답변 | checklist | checklist | `REDESIGN` | `미착수` | 체크리스트 답변 후보 | checklist 슬라이스 | 답변 타입별 저장 방식 |
| MySQL | `user_agreement` | (초안 없음) | auth/user | auth/user | `REDESIGN` | `완료` | **신규 서버에서 `user_agreements`로 확정**([`KD3-258`](../work/KD3-258-user-social-auth.md) V2). `(user_id, term_type)` unique, append-only라 `BaseEntity` 공통 컬럼 없이 `agreed_at`만 둔다 — 재제출해도 최초 동의 시각이 보존된다 | 확정됨 | 약관 버전 관리 필요 여부(현재 버전 개념 없음). 탈퇴 시 동의 이력 보존/삭제 정책 |
| MySQL | `notification_preference` | (초안 없음) | notification | notification | `REDESIGN` | `미착수` | 사용자 알림 수신 설정 후보. `user_id` PK, `push_enabled` | notification 슬라이스 | `user_notification_setting`과 중복 개념인지 확정하고 하나로 통합 |
| MySQL | `push_device` | (초안 없음) | notification | notification | `REDESIGN` | `미착수` | 푸시 기기 등록 후보. provider/platform/token, 동일 유저·플랫폼 재등록 시 기존 활성 기기 비활성화(QA3-205) | notification 슬라이스 | 토큰 회전, 만료 기기 정리 주기, 로그아웃 시 처리 |
| MySQL | `notification` | (초안 없음) | notification | notification | `REDESIGN` | `미착수` | 알림함 항목 후보. school/pet FK, 읽음 상태 | notification 슬라이스 | 보존 기간, 읽음 처리 단위(단건/전체), 유치원 해제 시 처리 |
| MySQL | `notification_outbox` | (초안 없음) | notification | notification | `REDESIGN` | `미착수` | 푸시 발송 명령 outbox 후보. `notification_id` NULL이면 알림함에 남기지 않는 PUSH_ONLY 채널 | notification 슬라이스 | outbox 패턴을 신규 서버에서도 유지할지, 재시도/lease 정책 |
| MySQL | `album_photo` | (초안 없음) | album | album/media | `REDESIGN` | `미착수` | 유치원 앨범 사진 후보. school/author FK, S3 key와 크기·contentType 보관 | album 슬라이스 | S3 수명주기 연동, 재원 기간(사이클) 스코핑 규칙(QA3-184) |
| MySQL | `album_photo_favorite` | (초안 없음) | album | album/media | `REDESIGN` | `미착수` | 보호자 사진 즐겨찾기 후보. 사진·사용자 unique | album 슬라이스 | 연결 해제된 보호자의 즐겨찾기 보존 여부 |
| MySQL | `attendance_checkinout` | (초안 없음) | attendance | attendance | `REDESIGN` | `미착수` | 등·하원 체크 상태 후보. pet/school/`school_pet_membership` FK, `attendance_date` 기준 unique | attendance 슬라이스 | `attendance_record`(알림장)와의 책임 분리, 취소 허용 범위 |
| MySQL | `attendance_checkinout_event` | (초안 없음) | attendance | attendance | `REDESIGN` | `미착수` | 등·하원 이벤트 이력 후보. event_type과 occurred_at | attendance 슬라이스 | 이벤트 소싱 수준으로 유지할지, 보존 기간 |
| MySQL | `breeds` | (초안 없음) | pet/reference | pet | `REDESIGN` | `진행중` | 신규 기준 테이블. v1 UTF-8 시드 385행을 Flyway로 적용하며, `display_order`는 제품이 정한 표시 순서 | pet 슬라이스 ([`KD3-418`](../work/KD3-418-breed-catalog-v1-api.md)) | 시드 갱신 주체와 주기. 레거시 `tb_breed`와 `breed_catalog`를 대체하며, 빈 MySQL DB 적용 검증 필요 |
| MySQL | `idempotency_key` | (초안 없음) | global | global/infra | `DEFER` | `미착수` | 멱등 요청 응답 저장 후보. `(user_id, operation, idempotency_key)` unique, `expires_at` 만료 | 알림장 발송 등 재시도 위험 슬라이스 | 신규 서버에서 DB 기반으로 갈지 Redis로 갈지, 만료 청소 주체 |
| MySQL | `kg_change_report` | (초안 없음) | kindergarten | kindergarten | `DEFER` | `미착수` | 유치원 정보 변경 제보 후보 | kindergarten 슬라이스 | 기능 유지 여부, 승인 운영 주체 |
| MySQL | `kg_change_evidence` | (초안 없음) | kindergarten/media | kindergarten | `DEFER` | `미착수` | 변경 제보 증빙 이미지 후보. S3 key와 `sha256` unique로 중복 차단 | kindergarten 슬라이스 | 기능 유지 여부, S3 보존 정책 |

## 6. 인벤토리 제외 객체

레거시 엔티티 59개 중 아래 17개는 똑독 v1/v2 시절 테이블이고, [`docs/adr/0001`](../adr/0001-legacy-v1-v2-폐기.md)에 따라 신규 서버로 이관하지 않는다. 대응하는 API도 전부 `api.md`에서 `DROP`(도메인 `legacy-admin`/`legacy-member`/`legacy-school`)으로 판정돼 있다. 5절에 행을 만들지 않되, "빠뜨린 것"과 구분하기 위해 여기 남긴다.

| 제외 객체 | 근거 |
|---|---|
| `tb_member`, `tb_admin`, `tb_link_firebase_auth_member`, `tb_link_school_admin`, `tb_refresh_token` | v1/v2 회원·인증 모델. 신규는 `user`/`social_user` 기반 |
| `tb_dog`, `tb_attendance`, `tb_ticket`, `tb_enrollmentform`, `tb_school_form`, `tb_agenda` | v1/v2 원장 업무 모델. 신규는 `pet`/`attendance_record`/`attendance_checkinout` 기반 |
| `tb_alarm`, `tb_alarm_event`, `tb_push`, `tb_fcm` | v1/v2 알림 모델. 신규는 `notification`/`notification_outbox`/`push_device` 기반 |
| `tb_image`, `tb_video` | v1/v2 미디어 모델. 신규는 도메인별 S3 key 컬럼 보유 |

제외를 확정하려면 운영 DB에서 각 테이블의 최신 `created_at`과 row count를 확인해 실사용이 끊겼는지 봐야 한다. 아직 확인하지 않았다.

## 7. Cross-domain 위험 후보

| 위험 | 관련 객체 | 확인 방향 |
|---|---|---|
| 탈퇴/삭제가 여러 도메인 데이터에 전파됨 | `user`, `social_user`, `pet`, `user_address`, `school_pet_membership`, `attendance_record`, `free_memo`, `checklist_submission` | auth/user 슬라이스에서 전체 삭제를 직접 구현하지 않고, 도메인별 보존/익명화 정책을 먼저 정한다 |
| 원장 권한 판단이 여러 기능의 선행 조건이 됨 | `tb_user_school_role`, `tb_school`, `user` | owner/authz 소유 포트를 만들고 다른 도메인은 직접 테이블을 수정하지 않는다 |
| 반려견-유치원 연결이 보호자, 반려견, 유치원을 동시에 묶음 | `school_pet_membership`, `school_invite`, `pet`, `user`, `tb_school` | owner-member 슬라이스에서 상태값과 unique 기준을 먼저 확정한다 |
| 유치원 프로필과 Redis/외부 검색 데이터 역할이 겹침 | `tb_school`, `tb_school_profile`, `tb_school_profile_option` | 신규 DB 저장 데이터와 Redis 캐시/검색 데이터의 출처를 분리한다 |
| 이미지 데이터가 DB와 S3 수명주기를 함께 가짐 | `tb_school_profile_image`, `tb_school_price_image`, `free_memo_photo` | DB 삭제와 S3 object 삭제/보존/정렬 정책을 함께 정한다 |
| 체크리스트 템플릿 변경이 과거 제출 답변에 영향 | `checklist_template`, `checklist_submission`, `checklist_answer` | 제출 시 템플릿 버전 스냅샷 필요 여부를 확정한다 |
| 사업자/대표자/주소 정보에 개인정보와 검증 이력이 포함됨 | `tb_owner_verification`, `tb_school_business_registration`, `tb_school` | 보존 기간, 마스킹, 철회 후 접근 정책을 정한다 |
| 알림 설정이 두 테이블로 갈라져 있음 | `user_notification_setting`, `notification_preference` | 어느 쪽이 진실인지 먼저 정하고 신규 서버에서는 하나만 만든다 |
| 푸시 발송이 알림함과 다른 수명주기를 가짐 | `notification`, `notification_outbox`, `push_device` | outbox 유지 여부와 재시도/만료 정책을 알림함 보존 정책과 함께 정한다 |
| 등·하원 상태와 알림장이 같은 "출석" 개념을 나눠 가짐 | `attendance_checkinout`, `attendance_checkinout_event`, `attendance_record`, `school_pet_membership` | 두 개념의 경계와 재원 기간(사이클) 스코핑 기준을 먼저 확정한다 |
| 앨범 사진이 재원 기간과 S3 수명주기에 동시에 묶임 | `album_photo`, `album_photo_favorite`, `school_pet_membership` | 연결 해제/재연결 시 열람 범위와 S3 보존 정책을 함께 정한다 |
