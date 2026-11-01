# 스타일

`plugins/LinkDialogues/styles/*.yml` 하나가 테마 하나입니다. 파일 이름이 스타일 이름이 됩니다.

- 새 테마: `default.yml` 을 복사해 `kingdom.yml` 로 저장 → 수정 → `/ldl reload`
- 사용: BetonQuest 에서 `conversationIO: linkdialogues_kingdom`, 또는 `config.yml` 의 `default-style` / `Conversation-Styles`
- 확인: `/ldl preview kingdom`

스타일 파일 수정은 `/ldl reload` 만으로 적용됩니다 (리소스팩 재생성 불필요).
단, **새 이미지를 쓰는 경우**는 `pack/layout.yml` 에 등록 후 `/ldl pack` 이 필요합니다.

## Settings

| 키 | 기본값 | 설명 |
|---|---|---|
| `typing-speed` | `1` | 글자 하나당 tick. `0` 이면 즉시 전체 표시 |
| `typing-sound-every` | `1` | 몇 글자마다 타이핑 사운드 |
| `effect` | `Slowness` | 대화 중 이동 제한: `Slowness`(크게 느려짐) / `Freeze`(이동·점프 불가) / `None` |
| `answer-numbers` | `true` | 선택지 앞에 `1. ` 번호 표시 (숫자키 모드에서는 켜 두는 것을 권장) |
| `prevent-exit` | `false` | 웅크리기로 종료 금지 |
| `prevent-skip` | `false` | F 로 타이핑 건너뛰기 금지 |
| `character-name` | `true` | NPC 이름표 표시 |
| `character-image` | `true` | 초상화 칸 표시 (`false` 면 초상화도 안 보임) |
| `background-fog` | `true` | 화면 아래쪽 어둡게 |
| `npc-focus` | `true` | 대화 시작 시 NPC 쪽을 바라봄 |
| `max-line-width` | `0` | 대사 한 줄 최대 폭(px). `0` = 대화창 폭에서 자동 계산 |

## Texts

대화창 아래 안내 문구. MiniMessage 사용 가능. 비워 두면 표시하지 않습니다.

| 키 | 언제 |
|---|---|
| `typing-info-line` | 글자가 써지는 중 |
| `steady-info-line` | 선택지가 나와 있을 때 |
| `next-info-line` | 다음 페이지가 있을 때 |
| `final-info-line` | 마지막 대사 |

```yaml
Texts:
  typing-info-line: 'F키로 넘기기'
  steady-info-line: '숫자키로 선택, F키로 결정'
  next-info-line: 'F키로 계속'
  final-info-line: 'F키로 닫기'
```

## Sounds

바닐라 사운드 ID 또는 [커스텀 사운드](resource-pack.md#커스텀-사운드) (`linkdialogues:<이름>`).
항목을 지우면 소리가 나지 않습니다.

| 키 | 언제 |
|---|---|
| `typing` | 글자가 써질 때 |
| `selection` | 선택지 결정 / 대화 닫기 |
| `scroll` | 선택지 커서 이동 |

```yaml
Sounds:
  typing:
    id: minecraft:block.note_block.hat
    source: MASTER      # MASTER, MUSIC, RECORD, WEATHER, BLOCK, HOSTILE, NEUTRAL, PLAYER, AMBIENT, VOICE, UI
    volume: 0.35
    pitch: 1.6
```

## Offsets

좌우 위치 (px). 대화창 왼쪽 끝이 기준(0)이고, `+` 는 오른쪽입니다.
**위아래 위치는 스타일이 아니라 `pack/layout.yml` 의 `ascent` 로 정합니다.**

| 키 | 기본값 | 대상 |
|---|---|---|
| `name` | `0` | 이름 글자 (이름표 기준 추가 이동) |
| `name-background` | `20` | 이름표 |
| `dialogue-background` | `0` | 대화창 |
| `dialogue-line` | `10` | 대사 글자 (대화창 왼쪽 여백) |
| `answer-background` | `140` | 선택지 창 |
| `answer-line` | `13` | 선택지 글자 (선택지 창 기준) |
| `arrow` | `-7` | 선택 화살표 (선택지 글자 기준) |
| `character` | `-16` | 초상화 칸 |
| `information` | `27` | 안내 문구 |

## Images

이 스타일이 쓸 이미지 ID (`pack/layout.yml` 의 `Images` 에 등록된 이름).
테마마다 다른 대화창 그림을 쓰고 싶으면, layout.yml 에 이미지를 추가 등록하고 여기서 바꿔 끼웁니다.

```yaml
Images:
  character-background: character-background
  arrow: hand
  dialogue-background: dialogue-background
  answer-background: answer-background
  name-start: name-start
  name-mid: name-mid
  name-end: name-end
  fog: fog
```

## Portraits

NPC 이름 → 초상화 이미지. [초상화](portraits.md) 참고.

## Colors

`#RRGGBB` 또는 색 이름(`white`, `gold` …).

이미지는 **이 색으로 곱해져서(틴트)** 표시됩니다. 흰색 이미지 + 색 지정 = 원하는 색의 창이 되므로,
같은 이미지로 여러 색 테마를 만들 수 있습니다. 원본 색 그대로 쓰려면 `#ffffff`.

| 키 | 대상 |
|---|---|
| `name` | 이름 글자 |
| `name-background` | 이름표 |
| `dialogue` | 대사 기본 글자색 (대사에 색이 있으면 그 색 우선) |
| `dialogue-background` | 대화창 |
| `answer` | 선택지 글자 |
| `answer-background` | 선택지 창 |
| `character-background` | 초상화 칸 / 초상화 |
| `arrow` | 선택 화살표 |
| `selected` | 선택된 선택지 글자 |
| `fog` | 배경 안개 |
| `information` | 안내 문구 |
