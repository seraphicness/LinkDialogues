# 설치

## 1. 플러그인 넣기

1. `LinkDialogues-x.y.z.jar` 를 `plugins/` 에 넣습니다.
2. (권장) BetonQuest 3.x 를 함께 설치합니다.
3. 서버를 켭니다. 콘솔에 아래와 같이 나오면 정상입니다.

```
[LinkDialogues] 리소스팩 생성: plugins/LinkDialogues/output/LinkDialogues.zip
[LinkDialogues] BetonQuest conversationIO 등록: linkdialogues, linkdialogues_default, linkdialogues_dark
```

> BetonQuest 가 없으면 `conversationIO 를 등록하지 않았습니다` 경고가 나오지만 플러그인은 켜지며,
> `/ldl preview`, `/ldl pack` 은 사용할 수 있습니다.

## 2. 리소스팩 적용

UI는 리소스팩이 있어야 보입니다. 아래 중 하나를 고르세요.

### A. CraftEngine 사용 (자동)

CraftEngine 이 설치되어 있으면 **아무것도 할 필요 없습니다.**

- LinkDialogues 는 CraftEngine 보다 먼저 로드되어
  `plugins/CraftEngine/resources/linkdialogues/` 에 `pack.yml` + `resourcepack/` 을 만듭니다.
- CraftEngine 이 리소스팩을 빌드할 때 이 폴더가 자동으로 병합됩니다.
- `/ldl pack` 으로 다시 만들었을 때 내용이 바뀌었으면 `ce reload pack` 을 자동 실행합니다.
  (내용이 같으면 실행하지 않습니다.)

관련 설정 (`config.yml`):

```yaml
Pack:
  craftengine:
    enabled: true            # false 면 CraftEngine 폴더를 만들지 않음
    folder: linkdialogues    # resources/ 아래 폴더 이름
    reload-command: 'ce reload pack'   # 비우면 자동 리로드 안 함
```

### B. 다른 리소스팩 플러그인 (Nexo, ItemsAdder, Oraxen 등)

`copy-to` 에 그 플러그인이 외부 팩을 읽는 경로를 적으면, 팩을 만들 때마다 그쪽으로 복사합니다.
경로는 **서버 루트 기준**입니다.

```yaml
Pack:
  copy-to: 'plugins/Nexo/pack/external_packs/LinkDialogues'
```

### C. 직접 배포

`plugins/LinkDialogues/output/LinkDialogues.zip` 을 서버 리소스팩(`server.properties` 의 `resource-pack`)이나
기존 팩에 합쳐서 배포합니다. 기존 팩과 합칠 때는 `assets/linkdialogues/` 폴더만 복사하면 됩니다.

> 다른 팩과 이름이 겹치지 않도록 모든 파일은 `linkdialogues` 네임스페이스에 들어갑니다.
> (`config.yml` 의 `Pack.namespace` 로 변경 가능)

## 3. 확인

게임에서 `/ldl preview` 를 입력해 대화창이 나오는지 확인합니다.
글자가 네모(□)로 나오거나 아무것도 안 보이면 리소스팩이 적용되지 않은 것입니다 → [문제 해결](troubleshooting.md)

## 업데이트

- jar 만 교체하고 서버를 재시작하면 됩니다.
- 설정 파일 구조가 바뀐 경우 `config-version` 을 보고 **자동 변환**합니다 (콘솔에 `설정을 vX → vY 으로 변환했습니다`).
- 기본 이미지/스타일 파일은 이미 있으면 덮어쓰지 않습니다. 새 기본 파일을 받고 싶으면 해당 파일을 지우고 재시작하세요.
