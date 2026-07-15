# 🚀 Instalação & Configuração

[← Arquitetura](01-arquitetura.md) | [Análise de Mídia →](03-modulo-analise-midia.md)

---

## Pré-requisitos

| Ferramenta | Versão mínima | Obrigatório? | Para quê |
|------------|----------------|--------------|----------|
| **Java (JDK)** | 25 | ✅ Sim | Runtime da aplicação |
| **Gradle** | Incluído via Wrapper | ✅ Sim | Build (`./gradlew`) |
| **FFmpeg / FFprobe** | Qualquer build recente | ✅ Sim | Análise de mídia e extração (containers não-MKV) |
| **MKVToolNix** (`mkvmerge` / `mkvextract`) | Qualquer build recente | ✅ Sim | Extração de MKV e remuxagem final |
| **LM Studio** | Qualquer versão com API OpenAI-compatible + `/api/v0/models` | ✅ Sim (para tradução) | Servir o LLM local |
| **TMDB API Key** | — | ⚪ Opcional | Metadados de anime com pôster/sinopse em PT-BR (sem chave, cai para Jikan/MAL) |

> O app é **Windows-first**: o seletor nativo de pasta/arquivo (`DialogoArquivoController`) usa PowerShell + `System.Windows.Forms`, e o `BrowserLauncher` abre o navegador padrão automaticamente no boot. Em outros SOs, os endpoints de diálogo nativo não funcionam — informe os caminhos manualmente nos formulários.

---

## Passo a passo

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd traducao_animes_llm_local_quarkus
```

### 2. Instalar as ferramentas externas

- **FFmpeg/FFprobe**: baixe em [ffmpeg.org](https://ffmpeg.org/download.html) e garanta que `ffmpeg`/`ffprobe` estão no `PATH`, ou configure o caminho completo em `application.yml` (`extrator.ffmpeg-path` / `extrator.ffprobe-path`).
- **MKVToolNix**: baixe em [mkvtoolnix.download](https://mkvtoolnix.download/) — no Windows, o app já procura automaticamente em `C:\Program Files\MKVToolNix\`.
- **LM Studio**: baixe em [lmstudio.ai](https://lmstudio.ai/), carregue um modelo de sua preferência (recomendado: modelos instruct de 7B+ com boa capacidade multilíngue — ex. Mistral Nemo, Qwen2.5-Instruct, Gemma) e **inicie o servidor local** (padrão: `http://127.0.0.1:1234`).

### 3. Configurar (opcional)

Copie/edite `src/main/resources/application-local.yml` para chaves privadas (git-ignored):

```yaml
tmdb:
  api-key: "sua-chave-tmdb-aqui"
```

Ajuste `src/main/resources/application.yml` se as ferramentas externas não estiverem no `PATH`:

```yaml
remuxer:
  mkvmerge-path: "C:/Program Files/MKVToolNix/mkvmerge.exe"

extrator:
  mkvmerge-path: "C:/Program Files/MKVToolNix/mkvmerge.exe"
  mkvextract-path: "C:/Program Files/MKVToolNix/mkvextract.exe"
  ffmpeg-path: "C:/ffmpeg/bin/ffmpeg.exe"
  ffprobe-path: "C:/ffmpeg/bin/ffprobe.exe"
```

### 4. Executar em modo desenvolvimento

```shell
./gradlew quarkusDev
```

> O servidor sobe em **`http://127.0.0.1:8080`** e o navegador abre automaticamente. O modo dev tem **live reload**: qualquer alteração em `.java` ou nos arquivos estáticos (`src/main/resources/static/`) é recarregada na próxima requisição, sem precisar reiniciar o processo manualmente.

### 5. Empacotar para produção

```shell
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

Über-jar (single-file):

```shell
./gradlew build -Dquarkus.package.jar.type=uber-jar
java -jar build/*-runner.jar
```

---

## Referência completa de configuração

Ver [Configuração — Referência Completa](14-configuracao.md) para todas as chaves de `application.yml`/`application.properties` com valores padrão e explicação.

---

## Verificando a instalação

1. Abra `http://127.0.0.1:8080` — o painel **Início** deve mostrar os widgets de status:
   - **Orquestrador**: `Java Quarkus` — badge verde "Online"
   - **LLM de Tradução**: badge indicando se o LM Studio respondeu
   - **Cache de Legendas**: contagem de arquivos `.cache.json` já existentes

   ![Painel Inicial com os três widgets de status (Orquestrador, LLM, Cache)](../src/main/resources/static/img/screenshots/painel-inicial.webp)
2. Vá ao painel **Análise de Mídia**, aponte para uma pasta com um `.mkv` de teste e clique em auditar — se o relatório aparecer no console com as faixas de vídeo/áudio/legenda, `ffprobe` está configurado corretamente.
3. Vá ao painel **Tradução Local**, escolha um contexto (ex.: "DanMachi") e uma pasta com uma legenda `.ass` de teste — se a tradução iniciar sem erro de "Servidor LLM indisponível", o LM Studio está acessível.

---

## Problemas comuns na instalação

| Sintoma | Causa provável | Solução |
|---------|------------------|---------|
| `mkvmerge.exe detectado` não aparece no log | MKVToolNix não está no `PATH` nem no caminho padrão do Windows | Configure `remuxer.mkvmerge-path` / `extrator.mkvmerge-path` explicitamente |
| "Servidor LLM indisponível" | LM Studio não está com o servidor local ligado, ou porta diferente de `1234` | Verifique em LM Studio → Developer → Start Server; ajuste `tradutor.llm.base-url` se necessário |
| Diálogo de seleção de pasta não abre | SO não é Windows, ou PowerShell bloqueado por política de execução | Digite o caminho manualmente no campo de texto |
| Porta 8080 já em uso | Outra instância do app já está rodando | Feche a instância anterior, ou mude `quarkus.http.port` em `application.properties` |

Mais detalhes em [Solução de Problemas](15-solucao-problemas.md).

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Arquitetura](01-arquitetura.md) | [Análise de Mídia →](03-modulo-analise-midia.md) |
