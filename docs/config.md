# config.yml

변경 후 `/ldl reload` (리소스팩 관련 `Pack` 항목은 `/ldl pack`).

## Settings

| 키 | 기본값 | 설명 |
|---|---|---|
| `default-style` | `default` | `conversationIO: linkdialogues` 가 쓸 스타일 (`styles/<이름>.yml`) |
| `interaction` | `[SwapHand]` | 진행/결정 입력. `SwapHand`(F) 는 항상 포함. `LeftClick`, `RightClick` 추가 가능 |
| `exit` | `Sneak` | 대화 종료 입력. `Sneak` / `None` |
| `select-keys` | `true` | 선택지에서 W/S 로 커서 이동, Space 로 결정 |
| `hotbar` | `Number` | 핫바 입력: `Number`(숫자키 선택) / `Scroll`(휠) / `None` |
| `scroll-cooldown` | `2` | 휠 입력 최소 간격 (tick) |
| `click-cooldown` | `4` | F/클릭 최소 간격 (tick) |
| `refresh-interval` | `10` | 변화가 없을 때 액션바를 다시 보내는 간격 (tick, 1~40) |
| `wait-final-click` | `true` | 선택지 없는 마지막 대사를 F 를 누를 때까지 유지 |
| `final-auto-close` | `0` | 마지막 대사를 N tick 후 자동으로 닫음 (0 = 끄기) |

## Conversation-Styles

대화별 스타일. [BetonQuest 연동](betonquest.md#대화별-스타일-지정-conversationio-를-건드리지-않고) 참고.

```yaml
Conversation-Styles:
  "myPackage>innkeeper": dark
```

## Pack

| 키 | 기본값 | 설명 |
|---|---|---|
| `namespace` | `linkdialogues` | 리소스팩 네임스페이스 (`assets/<namespace>/`) |
| `file-name` | `LinkDialogues` | `output/` 안의 폴더/zip 이름 |
| `description` | `LinkDialogues UI` | `pack.mcmeta` 설명 |
| `min-format` / `max-format` | `84` / `999` | `pack.mcmeta` 지원 포맷 범위 (26.1 = 84) |
| `build-on-enable` | `true` | 서버 시작 시 리소스팩 생성 |
| `copy-to` | `''` | 생성할 때마다 복사할 경로 (서버 루트 기준). 비우면 사용 안 함 |
| `craftengine.enabled` | `true` | CraftEngine 이 있으면 `resources/<folder>/` 에 자동 병합용 팩 생성 |
| `craftengine.folder` | `linkdialogues` | CraftEngine `resources/` 아래 폴더 이름 |
| `craftengine.reload-command` | `ce reload pack` | `/ldl pack` 후 내용이 바뀌었을 때 콘솔에서 실행 (비우면 안 함) |
| `font.file` | `Galmuri7.ttf` | `pack/fonts/` 의 TTF |
| `font.size` | `8` | 글자 크기 (px). Galmuri7 = 8, Galmuri9 = 10 권장 |
| `font.oversample` | `4` | 렌더링 선명도 (클수록 선명, 메모리 증가) |
| `font.cap-height` | `-1` | 글자 윗부분 높이(px). `-1` 이면 폰트에서 자동 측정. 줄 높이가 어긋날 때 수동 지정 |

폰트 교체는 [리소스팩 커스터마이즈](resource-pack.md#폰트) 참고.

## Messages

명령어 메시지 (MiniMessage 형식).

| 키 | 치환자 |
|---|---|
| `prefix` | |
| `reload` | |
| `pack-built` | `{path}` |
| `pack-failed` | `{error}` |
| `unknown-style` | `{style}` |
| `usage` | |

## config-version

설정 자동 변환용입니다. **직접 수정하지 마세요.**
