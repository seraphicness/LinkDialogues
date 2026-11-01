# BetonQuest 연동

LinkDialogues 는 BetonQuest 3.x 에 **conversationIO** 로 등록됩니다.
대화 내용(분기, 조건, 이벤트)은 전부 BetonQuest 가 처리하고, LinkDialogues 는 화면 표시와 입력만 담당합니다.

## conversationIO 이름

| 이름 | 사용하는 스타일 |
|---|---|
| `linkdialogues` | `config.yml` 의 `default-style` (또는 `Conversation-Styles` 지정) |
| `linkdialogues_<스타일명>` | `styles/<스타일명>.yml` 고정. 예) `linkdialogues_dark` |

스타일 파일을 새로 추가하면 `/ldl reload` 후 `linkdialogues_<새 스타일>` 이 등록됩니다.

## 대화 하나에만 적용

```yaml
conversations:
  innkeeper:
    quester: '여관 주인'
    conversationIO: linkdialogues        # ← 추가
    first: greet
    NPC_options:
      greet:
        text: '어서 오세요!'
        pointers: bye
    player_options:
      bye:
        text: '안녕히 계세요'
```

어두운 테마로 보여주려면 `conversationIO: linkdialogues_dark`.

## 서버 전체 기본값으로

BetonQuest 의 `config.yml`:

```yaml
conversation:
  default_io: linkdialogues
```

## 대화별 스타일 지정 (conversationIO 를 건드리지 않고)

`conversationIO: linkdialogues` 를 쓰는 대화는 LinkDialogues `config.yml` 의 `Conversation-Styles` 로 스타일을 바꿀 수 있습니다.

```yaml
Conversation-Styles:
  "myPackage>innkeeper": dark   # 패키지>대화 (정확한 ID)
  "blacksmith": dark            # 대화 이름만 (모든 패키지)
```

우선순위: `linkdialogues_<스타일>` > `Conversation-Styles` (전체 ID) > `Conversation-Styles` (이름) > `default-style`

## 표시되는 것

| BetonQuest | 화면 |
|---|---|
| `quester` | 이름표 (색 코드 유지). 스타일의 `Portraits` 와 연결되는 키 |
| NPC `text` | 대화창. 자동 줄바꿈, 5줄이 넘으면 페이지로 나뉨 |
| `player_options` | 선택지 창. 3개까지 한 번에 보이고, 더 많으면 선택에 따라 스크롤됨 |

- 텍스트의 색/굵게 등 서식은 그대로 표시됩니다. 폰트에 없는 글자는 `?` 로 바뀝니다.
- 선택지가 없는 NPC 대사(대화의 마지막)는 **플레이어가 F키를 누를 때까지 화면에 남습니다.**
  (`Settings.wait-final-click`, `final-auto-close` 로 변경)

## 대화 종료 / 이동

- 기본적으로 **웅크리기(Shift)** 로 대화를 끝낼 수 있습니다 (`Settings.exit`).
- BetonQuest 대화에 `stop: 'true'` 가 있으면 종료할 수 없습니다. 스타일의 `prevent-exit: true` 도 같은 효과입니다.
- 대화 중 이동은 스타일의 `effect` 로 제한합니다 (`Slowness` / `Freeze` / `None`).
  선택지가 떠 있는 동안에는 W/S 입력이 걸음으로 이어지지 않도록 항상 완전히 고정됩니다.
- 대화 시작 시 NPC 쪽을 바라보게 합니다 (`npc-focus`).
