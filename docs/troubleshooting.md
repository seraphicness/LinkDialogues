# 문제 해결

## UI가 안 보이거나 글자가 □ 로 나옴

리소스팩이 적용되지 않은 상태입니다.

- CraftEngine: `plugins/CraftEngine/resources/linkdialogues/` 가 있는지 확인 → `/ce reload pack` → 플레이어 재접속
- 직접 배포: `output/LinkDialogues.zip` 을 서버 팩에 넣었는지, 플레이어가 받았는지 확인
- `/ldl pack` 후 콘솔의 경고(이미지 없음 등)를 확인하세요.

## `UnknownDependencyException: [BetonQuest]`

구버전 jar 입니다. 최신 jar 는 BetonQuest 가 없어도 켜집니다 (`softdepend`).

## BetonQuest 대화가 기본 채팅 대화로 나옴

- 대화 파일에 `conversationIO: linkdialogues` 가 있는지 확인
- 콘솔에 `BetonQuest conversationIO 등록: ...` 이 나왔는지 확인 (BetonQuest 3.x 필요)
- `linkdialogues_<스타일>` 을 썼다면 `styles/<스타일>.yml` 이 있는지 확인

## F키 / 숫자키가 안 먹힘

- 다른 플러그인이 `PlayerSwapHandItemsEvent` / `PlayerItemHeldEvent` 를 먼저 취소하고 있는지 확인
- `click-cooldown` 이 너무 크지 않은지 확인
- 숫자키는 **선택지가 나와 있을 때만** 동작합니다. 선택지 개수보다 큰 숫자키는 무시됩니다.
- 숫자키는 커서만 옮기고, 결정은 F키입니다.

## 대화 중 화면이 깜빡이거나 다른 메시지와 겹침

다른 플러그인(HUD, 마나/스태미나 표시 등)이 **액션바**를 쓰고 있습니다. LinkDialogues 의 UI 도 액션바라서 서로 덮어씁니다.
대화 중에는 해당 플러그인의 액션바 표시를 끄거나, 보스바/타이틀을 쓰는 HUD 를 사용하세요.

## 글자나 이미지 위아래 위치가 어긋남

- 텍스트만 어긋남: `config.yml` 의 `Pack.font.cap-height` 를 직접 지정 (갈무리7 기준 `7`) 후 `/ldl pack`
- 전체적으로 조정: `pack/layout.yml` 의 `ascent` 값 수정 후 `/ldl pack`
- 좌우: 스타일의 `Offsets` 수정 후 `/ldl reload`
- `/ldl preview` 로 확인하면서 조정하면 편합니다.

## 대화 후에도 느려짐 / 점프가 안 됨

대화 중 이동 제한은 `linkdialogues:dialogue_lock` 속성 modifier 로 걸립니다. 대화가 끝나면 제거되며,
서버가 대화 중에 꺼졌더라도 **재접속하면 자동으로 제거**됩니다.

## 대사 마지막이 안 닫힘

`wait-final-click: true` (기본값) 이면 선택지 없는 마지막 대사는 F키를 눌러야 닫힙니다.
자동으로 닫으려면 `final-auto-close` 에 tick 수를 지정하세요.

## 업데이트했는데 새 기본 이미지/문구가 안 나옴

기존 파일은 덮어쓰지 않습니다. `plugins/LinkDialogues/pack/images/` 또는 `styles/` 의 해당 파일을 지우고 재시작하세요.
