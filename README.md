# LinkDialogues

**BetonQuest 대화를 리소스팩 UI로 표시하는 Paper 플러그인**

BetonQuest의 기본 채팅/메뉴 대화 대신, 화면 하단에 대화창 · NPC 이름표 · 초상화 · 선택지를 그려 줍니다.
모든 UI는 **액션바 한 줄 + 자동 생성 리소스팩(폰트)** 으로 표현되므로 클라이언트 모드가 필요 없습니다.

```
            ┌────────┐
            │초상화  │ ╱ 여관 주인 ╲
            └────────┘ ┌──────────────────────────────┐   ┌─────────────────────┐
                       │ 어서 오세요, 여행자님!        │   │ ▶ 1. 방을 빌리고 싶어요│
                       │ 오늘 밤 묵어 가실 건가요?     │   │   2. 소문을 들려줘     │
                       │                              │   │   3. 그냥 둘러볼게요   │
                       └──────────────────────────────┘   └─────────────────────┘
                                     숫자키로 선택, F키로 결정
```

## 주요 기능

- BetonQuest 3.x `conversationIO` 로 등록 → 대화 파일에 한 줄만 추가하면 적용
- 타이핑 효과(글자별 사운드), 긴 대사 자동 줄바꿈 · 페이지 넘김
- 선택지: **숫자키로 선택 → F키로 결정** (W/S + Space, 마우스 휠도 지원)
- NPC 이름표, NPC별 초상화, 배경 안개, 대화 중 이동 제한, NPC 바라보기
- 스타일 파일로 테마를 여러 개 만들고 대화별로 다르게 지정
- MiniMessage / `&` 색 코드 그대로 표시, 한글 픽셀 폰트(갈무리) 기본 내장
- 리소스팩 자동 생성 + **CraftEngine 자동 병합**, 그 외 플러그인(Nexo 등)용 복사 경로 지원
- `/ldl preview` 로 BetonQuest 없이 UI 미리보기

## 요구 사항

| 항목 | 버전 |
|---|---|
| 서버 | Paper 26.x (Java 25) |
| BetonQuest | 3.x (3.2.0 기준 개발) — 없어도 플러그인은 켜지지만 대화 연결 불가 |
| CraftEngine | 선택 사항 (있으면 리소스팩 자동 병합) |

## 빠른 시작

1. [Releases](../../releases) 에서 `LinkDialogues-x.y.z.jar` 를 받아 `plugins/` 에 넣고 서버를 켭니다.
2. 리소스팩이 `plugins/LinkDialogues/output/LinkDialogues.zip` 에 생성됩니다.
   CraftEngine 을 쓰면 자동으로 병합되므로 따로 할 일이 없습니다.
3. BetonQuest 대화 파일에 `conversationIO: linkdialogues` 를 추가합니다.

```yaml
conversations:
  innkeeper:
    quester: '여관 주인'
    conversationIO: linkdialogues
    first: greet
    NPC_options:
      greet:
        text: '어서 오세요, 여행자님! 오늘 밤 묵어 가실 건가요?'
        pointers: room, rumor, bye
    player_options:
      room:
        text: '방을 빌리고 싶어요'
      rumor:
        text: '소문을 들려줘'
      bye:
        text: '그냥 둘러볼게요'
```

4. 게임에서 `/ldl preview` 로 UI가 잘 나오는지 확인합니다.

## 문서

자세한 사용법은 [`docs/`](docs/README.md) 에 있습니다.

| 문서 | 내용 |
|---|---|
| [설치](docs/installation.md) | 설치, 리소스팩 적용, CraftEngine 연동 |
| [BetonQuest 연동](docs/betonquest.md) | conversationIO 지정, 스타일 지정, 종료/이동 제한 |
| [조작법](docs/controls.md) | 플레이어 입력, 입력 방식 변경 |
| [config.yml](docs/config.md) | 전체 설정 항목 |
| [스타일](docs/styles.md) | 색상, 위치, 문구, 사운드, 테마 만들기 |
| [초상화](docs/portraits.md) | NPC별 초상화 지정 |
| [리소스팩 커스터마이즈](docs/resource-pack.md) | layout.yml, 이미지 교체, 폰트, 커스텀 사운드 |
| [명령어 · 권한](docs/commands.md) | `/ldl` 명령어 |
| [문제 해결](docs/troubleshooting.md) | 자주 묻는 문제 |

## 직접 빌드

JDK 25 가 필요합니다 (Gradle 툴체인이 자동으로 내려받습니다).

```bash
./gradlew build
# 결과물: build/libs/LinkDialogues-<버전>.jar
```

## 라이선스

[MIT](LICENSE). 단, 포함된 갈무리 폰트는 SIL OFL 1.1 을 따릅니다.

## 서드파티

- **갈무리(Galmuri)** 폰트 — © Lee Minseo, [SIL Open Font License 1.1](src/main/resources/pack/fonts/Galmuri-LICENSE.txt).
  생성되는 리소스팩에 포함되며, 라이선스 파일은 `plugins/LinkDialogues/pack/fonts/` 에 함께 저장됩니다.
- 기본 포함된 UI 이미지(`pack/images/*.png`)는 이 프로젝트용으로 새로 만든 예제 이미지입니다.
