---
paths:
  - "**/domain/user/**"
  - "**/MajorNormalizer.java"
---

# User 도메인을 건드릴 때

## 전화번호는 하이픈 없이 저장된다

정규식은 `^01[0-9]\d{7,8}$` 다. **하이픈을 허용하도록 바꾸면 안 된다** — 저장된 값이
전부 숫자만이라 기존 사용자 전원이 수정에 실패한다.

회원가입이 `toDigits()` 를 거친 값을 보내고(Web `signup/page.tsx:256`), 테스트 픽스처도
`"01012345678"` 형태다. `010` 이 아니라 `01[0-9]` 로 여는 이유는 011·016 등 구형 번호가
남아 있어서다.

## `major` 는 프론트가 드롭다운이어도 서버에서 검증한다

API 는 직접 호출될 수 있다. 그리고 `MajorNormalizer.normalize()` 는 **알 수 없는 값을
그대로 되돌려주므로 정규화만으로는 검증이 되지 않는다.** 정규화 후 알려진 코드인지
확인하고, 아니면 `INVALID_MAJOR` 를 낸다.

`MajorNormalizer` 에는 정방향(label→code)만 있고 **역변환이 없다.** 표시용 라벨 변환은
프론트가 한다(`src/constant/majorOptions.ts`). 서버는 코드만 주고받는다.

## 프로필 수정은 부분 수정이 아니라 전체 치환이다

`name`·`major`·`phoneNumber` 가 모두 `nullable = false` 라 빈 값을 허용하지 않는다.

| 필드 | 규칙 |
|---|---|
| `name` | `@NotBlank`, 1~30자 |
| `major` | 정규화 후 알려진 코드. 아니면 `INVALID_MAJOR` |
| `phoneNumber` | `^01[0-9]\d{7,8}$` — 하이픈 없는 숫자만 |
| 이미지 | MIME `image/png`·`image/jpeg`·`image/webp` 화이트리스트, 최대 5MB |
