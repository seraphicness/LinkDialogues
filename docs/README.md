# LinkDialogues 문서

1. [설치](installation.md)
2. [BetonQuest 연동](betonquest.md)
3. [조작법](controls.md)
4. [config.yml](config.md)
5. [스타일](styles.md)
6. [초상화](portraits.md)
7. [리소스팩 커스터마이즈](resource-pack.md)
8. [명령어 · 권한](commands.md)
9. [문제 해결](troubleshooting.md)

## 폴더 구조

서버를 한 번 켜면 아래 파일들이 만들어집니다. **이미 있는 파일은 업데이트 시에도 덮어쓰지 않습니다.**

```
plugins/LinkDialogues/
├── config.yml              # 전체 설정 (입력, 리소스팩, 메시지)
├── styles/
│   ├── default.yml         # 기본 스타일
│   └── dark.yml            # 어두운 테마 예시
├── pack/                   # 리소스팩 재료
│   ├── layout.yml          # UI 줄 높이, 이미지 등록, 커스텀 사운드
│   ├── images/             # UI 이미지 (png)
│   ├── fonts/              # 텍스트 폰트 (ttf) + 라이선스
│   └── sounds/             # 커스텀 사운드 (ogg)
└── output/                 # 생성된 리소스팩
    ├── LinkDialogues/      # 폴더 형태
    └── LinkDialogues.zip   # zip 형태
```

## 동작 원리 (간단히)

- 대화 UI 전체가 **액션바 메시지 하나**입니다.
- 리소스팩에 "줄마다 높이가 다른 폰트"와 "UI 이미지를 글자로 등록한 폰트"를 만들어 두고,
  액션바 텍스트를 그 폰트들로 조합해 대화창 모양을 그립니다. 좌우 위치는 ±1px 공백 글자로 맞춥니다.
- 액션바는 시간이 지나면 사라지므로 `refresh-interval` 마다 다시 보냅니다.
- 그래서 **대화 중에는 다른 플러그인의 액션바 메시지와 겹칠 수 있습니다** ([문제 해결](troubleshooting.md) 참고).
