#!/usr/bin/env node
// docs/ 구조를 기계로 검증한다. 판단이 필요한 것(내용의 타당성, 어느 폴더가 맞는지)은 검사하지 않는다.
// 검사 대상은 docs/rules/documentation.md §1·§2의 규칙 중 대조만으로 판정 가능한 것뿐이다.
import { readFileSync } from 'node:fs';
import { execSync } from 'node:child_process';
import path from 'node:path';

const ALLOWED_DIRS = ['rules', 'conventions', 'workflows', 'work', 'domains', 'inventory', 'architecture', 'adr'];
const ALLOWED_ROOT_FILES = ['README.md', 'service.md'];
const WORK_NAME = /^[A-Z]+[A-Z0-9]*-\d+-[a-z0-9]+(-[a-z0-9]+)*\.md$/;

// -z: 경로를 NUL로 구분해 받는다. 기본 출력은 한글 경로를 escape 처리해 비교가 깨진다.
const tracked = execSync('git ls-files -z', { encoding: 'utf8' }).split('\0').filter(Boolean);
const trackedSet = new Set(tracked);
const failures = [];

// 1. 폴더 allowlist — 정의되지 않은 위치에 문서를 만들지 않는다
for (const file of tracked.filter((f) => f.startsWith('docs/'))) {
  const rest = file.slice('docs/'.length);
  const segments = rest.split('/');
  if (segments.length === 1) {
    if (!ALLOWED_ROOT_FILES.includes(segments[0])) {
      failures.push(`docs/ 최상위에는 ${ALLOWED_ROOT_FILES.join(', ')}만 둔다: ${file}`);
    }
  } else if (!ALLOWED_DIRS.includes(segments[0])) {
    failures.push(`정의되지 않은 폴더다. documentation.md §1에 위치를 먼저 정의한다: ${file}`);
  }
}

// 2. work/ 파일명 — <JIRA-KEY>-<kebab-설명>.md
for (const file of tracked.filter((f) => f.startsWith('docs/work/') && f.endsWith('.md'))) {
  const name = path.basename(file);
  if (!WORK_NAME.test(name)) {
    failures.push(`작업 문서 파일명은 <JIRA-KEY>-<kebab-설명>.md 여야 한다: ${file}`);
  }
}

// 3. 링크 무결성 — 문서 이동으로 죽은 상대 경로 참조를 잡는다
const LINK = /\[[^\]]*\]\(([^)\s]+)\)/g;
const dirs = new Set(tracked.map((f) => path.dirname(f)));
for (const file of tracked.filter((f) => f.endsWith('.md'))) {
  const body = readFileSync(file, 'utf8');
  for (const [, rawTarget] of body.matchAll(LINK)) {
    if (/^(https?:|#|mailto:)/.test(rawTarget)) continue;
    const target = rawTarget.split('#')[0];
    if (!target) continue;
    const resolved = path.posix.normalize(path.posix.join(path.dirname(file), target));
    const asDir = resolved.replace(/\/$/, '');
    if (!trackedSet.has(resolved) && !dirs.has(asDir)) {
      failures.push(`깨진 링크: ${file} → ${rawTarget}`);
    }
  }
}

if (failures.length) {
  console.error(failures.map((f) => `- ${f}`).join('\n'));
  process.exit(1);
}
console.log(`docs 검사 통과 (문서 ${tracked.filter((f) => f.startsWith('docs/')).length}개)`);
