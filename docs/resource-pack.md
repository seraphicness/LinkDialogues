# 리소스팩 커스터마이즈

리소스팩 재료는 `plugins/LinkDialogues/pack/` 에 있습니다. 수정 후에는 **`/ldl pack`** 으로 다시 만들어야 적용됩니다.
(플레이어는 바뀐 리소스팩을 다시 받아야 합니다. CraftEngine 은 자동 리로드)

## layout.yml

### 줄 높이 (ascent)

`ascent` 는 "액션바 기준선에서 위로 몇 px 에 글자/이미지 윗부분이 오는가" 입니다. 클수록 화면 위쪽.

```yaml
Character-Name:      # 이름표 글자
  ascent: 40

Dialogue-Lines:      # 대사
  count: 5           # 한 페이지 줄 수
  ascent: 25         # 첫 줄 높이
  space: 9           # 줄 간격 (px)

Information-Line:    # 안내 문구
  ascent: -25

Answer-Lines:        # 선택지
  count: 3           # 한 번에 보이는 선택지 수
  ascent: 75
  space: 9
```

> 줄 수(`count`)를 바꾸면 그만큼 폰트가 추가로 만들어집니다. 대화창 이미지 높이도 맞춰 바꿔 주세요.

### 이미지 등록

```yaml
Images:
  <이미지 ID>:
    file: <images 폴더의 png>
    reduction-ratio: 1     # 축소 배율 (2 = 절반)
    ascent: 37             # 이미지 윗부분 높이
```

선택 화살표처럼 선택지 줄을 따라다녀야 하는 이미지는 `is-arrow: true` 를 쓰면
선택지 줄마다 위치가 자동으로 맞춰집니다 (`ascent` 대신 `arrow-shift` 로 미세 조정).

```yaml
  hand:
    file: hand.png
    is-arrow: true
    arrow-shift: 0
    reduction-ratio: 1
```

기본 이미지 ID: `dialogue-background`, `answer-background`, `character-background`, `hand`, `fog`,
`name-start`, `name-mid`, `name-end`. 스타일의 `Images` 에서 이 ID 들을 참조합니다.

## 이미지 교체

`pack/images/` 의 png 를 같은 이름으로 덮어쓰고 `/ldl pack`.

| 파일 | 기본 크기 | 용도 |
|---|---|---|
| `dialogue.png` | 209×57 | 대화창 |
| `answer.png` | 134×37 | 선택지 창 |
| `character.png` | 32×32 | 초상화 칸 |
| `hand.png` | 14×8 | 선택 화살표 |
| `fog.png` | 256×256 | 배경 안개 (가로로 반복) |
| `name_start.png` | 3×14 | 이름표 왼쪽 끝 |
| `name_mid.png` | 2×14 | 이름표 가운데 (이름 길이만큼 반복) |
| `name_end.png` | 3×14 | 이름표 오른쪽 끝 |

팁:

- 이미지는 스타일의 `Colors` 로 **곱해져 틴트**됩니다. 흰색/밝은 회색으로 그리고 색은 스타일에서 주는 것을 권장합니다.
- 크기를 바꾸면 스타일의 `Offsets` 와 layout.yml 의 `ascent` 를 함께 조정해야 할 수 있습니다.
  대사 줄 폭은 대화창 이미지 폭에 맞춰 자동 계산됩니다.
- 오른쪽 끝의 투명한 부분은 폭 계산에서 제외됩니다. 너무 큰 이미지는 클라이언트 메모리를 많이 쓰므로 필요한 만큼만 크게 만드세요.

## 폰트

텍스트는 `pack/fonts/` 의 TTF 하나로 그립니다. 기본은 한글 픽셀 폰트 **갈무리7** (Galmuri7, SIL OFL 1.1).

다른 폰트로 바꾸기:

1. `pack/fonts/` 에 TTF 를 넣습니다.
2. `config.yml`:

```yaml
Pack:
  font:
    file: MyFont.ttf
    size: 10          # 글자 크기 (px)
    oversample: 4
    cap-height: -1    # 자동 측정. 줄 높이가 어긋나면 글자 윗부분 높이를 직접 입력
```

3. `/ldl pack`

- 갈무리9 (`Galmuri9.ttf`, `size: 10`) 도 함께 들어 있습니다.
- 폰트에 없는 글자는 `?` 로 표시됩니다.
- 크기를 키우면 줄 간격(`space`)과 이미지도 함께 키워야 겹치지 않습니다.
- 픽셀 폰트는 설계 크기의 정수배(예: 갈무리7 → 8, 16) 에서 가장 선명합니다.

## 커스텀 사운드

1. `pack/sounds/typing.ogg` 를 넣습니다 (ogg Vorbis, 모노 권장).
2. `layout.yml`:

```yaml
Sounds:
  typing:
    file: typing.ogg
```

3. `/ldl pack` → 사운드 ID 는 `linkdialogues:typing`
4. 스타일에서 사용:

```yaml
Sounds:
  typing:
    id: linkdialogues:typing
    volume: 0.5
    pitch: 1.0
```

## 생성되는 파일

```
assets/linkdialogues/
├── font/
│   ├── default.json        # UI 이미지 + 공백 글자
│   ├── text.ttf            # 텍스트 폰트
│   ├── name.json, information.json
│   ├── line_1.json ~ line_N.json
│   └── answer_1.json ~ answer_N.json
├── textures/font/images/*.png
├── sounds/*.ogg            # 커스텀 사운드가 있을 때
└── sounds.json
```

UI 이미지는 개인 사용 영역 문자 `U+E100` 부터, 공백 글자는 `U+F800`~`U+F83F` 를 사용합니다.
모두 `linkdialogues` 폰트 안에서만 쓰이므로 다른 리소스팩의 글자와 충돌하지 않습니다.
