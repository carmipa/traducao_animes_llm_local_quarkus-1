# 🧹 Módulo: Renomear Arquivos

[← Remuxer](08-modulo-remuxer.md) | [Contextos & Lore →](09-contextos-lore.md)

---

## Para que serve

Painel **"13. Renomear Arquivos"** da SPA (grupo **Finalização**). Renomeia em lote arquivos de vídeo com nomes confusos de release groups de tracker (ex.: `[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv`) para um padrão limpo `Nome do Anime - S01E01.mkv`, extraindo o número do episódio por **regex** — com simulação prévia (dry-run) e **reversão completa (undo)**.

![Painel de Renomear Arquivos](../src/main/resources/static/img/screenshots/renomear-arquivos.png)

---

## Pacote e classes principais

| Classe | Papel |
|--------|-------|
| `RenomeadorUseCase` (`application`) | Simula, aplica e reverte a renomeação; extrai o episódio por regex; salva o manifesto de reversão dentro do projeto |
| `OperacaoRenomeacao` (`domain`) | Record da operação (id, data, pasta, lista de `ItemRenomeado` original → novo) |
| `RenomearArquivosController` (`presentation/web`) | Endpoints REST (JAX-RS) — simular, aplicar e reverter em background |
| `RenomearArquivosRequest` (`presentation/web`) | Record do payload `{caminhoOrigem, nomePadrao}` |

---

## Como o episódio é extraído

1. Tags entre colchetes (`[SubsPlease]`, `[ABCD1234]`) são removidas antes da análise.
2. **Padrão principal**: separadores comuns seguidos de número — `- 01`, `Ep 03`, `Episódio 03`, `E04`, `Episode 12`.
3. **Fallback**: primeiro número isolado de 2-4 dígitos no nome.
4. Sem número identificável → o arquivo é **mantido intacto** (nunca renomeia no chute).

O nome final é `"<Nome Novo Padrão> - S01E<NN><extensão original>"`.

---

## Fluxo com segurança de reversão

```mermaid
sequenceDiagram
    actor Op as Operador
    participant UI as Painel Renomear Arquivos
    participant API as RenomearArquivosController
    participant UC as RenomeadorUseCase

    Op->>UI: Pasta + nome padrão
    UI->>API: POST /api/renomear-arquivos/simular
    API->>UC: simularRenomeacao (assíncrono)
    UC-->>UI: [DRY-RUN] antes → depois via SSE (canal renomear-arquivos)

    Op->>UI: Confere e clica "Aplicar Renomeação"
    UI->>API: POST /api/renomear-arquivos/aplicar
    API->>UC: aplicarRenomeacao (assíncrono)
    UC->>UC: Files.move ATOMIC_MOVE por arquivo
    UC->>UC: grava manifesto em logs/renomear-arquivos/undo/
    UC-->>UI: [OK]/[ERRO] por arquivo via SSE

    Op->>UI: (se necessário) "Reverter (Undo)"
    UI->>API: POST /api/renomear-arquivos/reverter
    API->>UC: reverterRenomeacao — lê o .json e desfaz
```

- **Dry-run primeiro, sempre**: a simulação lista cada `antes → depois` sem tocar em nada.
- **Undo garantido sem sujar a mídia**: ao aplicar, um manifesto `kronos_undo_renomeacao_<hash>.json` é salvo em `logs/renomear-arquivos/undo/` dentro do projeto; "Reverter" o lê e desfaz os `move` (o manifesto só é apagado se a reversão terminar sem erros).
- Conflitos (destino já existe) são **pulados com erro logado** — nunca sobrescreve.
- Cada arquivo renomeado incrementa a métrica `arquivosSanitizados` na [Telemetria](10-modulo-telemetria.md).

---

## Endpoints REST

| Endpoint | Payload | Canal SSE |
|----------|---------|-----------|
| `POST /api/renomear-arquivos/simular` | `{caminhoOrigem, nomePadrao}` | `renomear-arquivos` |
| `POST /api/renomear-arquivos/aplicar` | `{caminhoOrigem, nomePadrao}` | `renomear-arquivos` |
| `POST /api/renomear-arquivos/reverter` | `{caminhoOrigem}` | `renomear-arquivos` |

```json
{ "caminhoOrigem": "C:/animes/[SubsPlease] Nome Anime", "nomePadrao": "Nome Anime" }
```

`caminhoOrigem` é **obrigatório** (`400` se ausente). Os três endpoints respondem `200` imediatamente e executam em background — acompanhe pelo console do painel.

---

## Pontos de atenção

- O padrão gerado assume **S01** fixo — para temporadas ≠ 1, inclua a temporada no nome padrão manualmente ou renomeie por temporada em pastas separadas.
- Renomear vídeos **depois** de traduzir/remuxar quebra o pareamento vídeo ↔ legenda do [Remuxer](08-modulo-remuxer.md) — o lugar natural do Renomear Arquivos é **antes** da Análise de Mídia ou **após o remux final**, na coleção pronta.
- A pasta alvo de mídia não recebe manifesto, backup ou arquivo operacional do KRONOS. O undo fica em `logs/renomear-arquivos/undo/` dentro do projeto.

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Remuxer](08-modulo-remuxer.md) | [Contextos & Lore →](09-contextos-lore.md) |
