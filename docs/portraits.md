# 초상화

대화창 왼쪽 위 캐릭터 칸에 NPC별 그림을 넣을 수 있습니다.
초상화는 **BetonQuest 대화의 `quester` 이름**으로 연결됩니다.

## 예시: '여관 주인' 에게 초상화 넣기

BetonQuest 대화:

```yaml
conversations:
  innkeeper:
    quester: '여관 주인'          # ← 이 이름이 연결 키
    conversationIO: linkdialogues
```

### ① 그림 파일 넣기

`plugins/LinkDialogues/pack/images/innkeeper.png`

- 권장 크기 **32×32** (기본 캐릭터 칸과 같은 크기)
- 더 큰 그림은 `reduction-ratio` 로 줄일 수 있습니다 (64×64 + `reduction-ratio: 2` → 32×32)

### ② `pack/layout.yml` 의 `Images:` 에 등록

```yaml
Images:
  # ... 기존 항목 ...
  innkeeper:
    file: innkeeper.png
    reduction-ratio: 1
    ascent: 60          # 기본 캐릭터 칸(character-background)과 같은 높이
```

### ③ 스타일 파일의 `Portraits:` 에 연결

`plugins/LinkDialogues/styles/default.yml`

```yaml
Portraits:
  '여관 주인': innkeeper
```

- 왼쪽: `quester` 이름 (색 코드 제외, 앞뒤 공백 무시)
- 오른쪽: ②에서 정한 이미지 ID
- 스타일마다 따로 설정합니다. `dark` 스타일에서도 보이게 하려면 `dark.yml` 에도 추가하세요.

### ④ 적용

```
/ldl pack
/ldl reload
```

NPC 를 더 추가하려면 ①~③ 반복 후 ④.

## 참고

- 초상화는 기본 캐릭터 칸 이미지를 **대신해서** 그려집니다. 테두리가 필요하면 그림에 포함하세요.
- 초상화도 `Colors.character-background` 색으로 틴트됩니다. 원래 색 그대로 보이게 하려면 `'#ffffff'` 로 두세요.
- 좌우 위치는 스타일의 `Offsets.character`, 위아래 위치는 layout.yml 의 `ascent` 로 조정합니다.
- 스타일의 `Settings.character-image` 가 `false` 면 초상화도 표시되지 않습니다 (기본 `dark` 스타일이 `false`).
- 같은 이름의 NPC 가 여러 대화에 나오면 모두 같은 초상화가 나옵니다.
