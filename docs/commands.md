# 명령어 · 권한

기본 명령어 `/linkdialogues`, 별칭 `/ldl`. 권한 `linkdialogues.admin` (기본: OP).

| 명령어 | 설명 |
|---|---|
| `/ldl reload` | `config.yml`, 스타일, `pack/layout.yml` 다시 읽기. 새 스타일의 conversationIO 등록 |
| `/ldl pack` | 설정을 다시 읽고 리소스팩 재생성. CraftEngine 폴더 갱신 (내용이 바뀌었으면 `ce reload pack` 실행) |
| `/ldl preview [스타일]` | BetonQuest 없이 예제 대화 표시 (플레이어 전용). 위치/색 조정할 때 사용 |

## 언제 무엇을?

| 수정한 것 | 실행 |
|---|---|
| `config.yml` 의 `Settings`, `Messages`, `Conversation-Styles` | `/ldl reload` |
| 스타일 파일 (`styles/*.yml`) | `/ldl reload` |
| 스타일 파일 추가 | `/ldl reload` |
| `config.yml` 의 `Pack` | `/ldl pack` |
| `pack/layout.yml`, 이미지, 폰트, 사운드 | `/ldl pack` (+ 플레이어 리소스팩 재적용) |

> 진행 중인 대화에는 리로드 내용이 바로 반영되지 않을 수 있습니다. 새로 시작한 대화부터 적용됩니다.
