# Execução em contêiner

> Estado em 11/08/2026: **imagem construída e aplicação no ar dentro do
> contêiner** — `kronos-core:local`, 1,32 GB, boot em 1,4 s, `healthy`, tela
> conferida no navegador. As provas estão em "O que já foi verificado". Segue
> faltando uma mudança de código, em "O que ainda falta".

## Por que existe

O KRONOS invoca três binários que não são Java — `ffmpeg`, `ffprobe` e
`mkvextract`. Sem contêiner, quem for usar o projeto precisa instalar ffmpeg e
mkvtoolnix à mão e acertar o PATH. **Esse é o ganho principal da imagem**, acima
de qualquer questão de portabilidade de JVM.

O KRONOS **não vai para VPS**. Roda na máquina de quem usa, ao lado de um
LM Studio local. A porta é publicada apenas em `127.0.0.1` porque a aplicação lê
e escreve no acervo de legendas e não tem autenticação nenhuma na frente dela.

## Como rodar

```bash
cp .env.example .env      # confira KRONOS_ACERVO
docker compose build
docker compose up -d
docker compose logs -f kronos
```

A interface fica em <http://127.0.0.1:8099>.

## Decisões de desenho

**Base Ubuntu, não UBI.** O projeto irmão `framework-net-java-quarkus` roda em
`ubi9/openjdk-25-runtime`. Aqui isso seria um erro: `ffmpeg` não existe nos
repositórios do RHEL e chegar nele pela UBI exige RPM Fusion de terceiro. No
Ubuntu é um `apt-get`. A divergência do padrão é deliberada, e o motivo é o
ffmpeg.

**Bind mount, não volume nomeado.** `cache/`, `logs/`, `relatorios/` e
`backups/` são conferidos, editados e copiados à mão com frequência — foi
editando o JSON do cache que se corrigiu 53 cartões de data em 06/08/2026.
Volume nomeado esconderia esses arquivos dentro do Docker e tornaria a inspeção
um exercício de arqueologia.

**`LANG=C.UTF-8` no runtime.** O acervo tem obra com acento e colchete no nome
(`[Sokudo]`, `[Coalgirls]`, `[Joseki]`) e legenda em PT-BR. Sob locale POSIX
esses nomes viram mojibake já na leitura de diretório — é o mesmo defeito que o
`build.gradle` combate no console do Windows, entrando por outra porta.

**Teto de memória de 4 GB.** O pipeline é pesado em CPU e o LM Studio disputa a
mesma máquina. Sem teto, o contêiner competiria justamente com aquilo de que ele
depende.

**`.dockerignore` reescrito.** O arquivo anterior era o scaffold do Quarkus para
copiar um runner já construído no host: `*` seguido de exceções em `build/`. Com
build multi-stage isso excluiria `src/` e o `COPY src src` falharia.

## Falha fechada nos caminhos do host

As duas variáveis que apontam para pasta do HOST são declaradas com `:?`, não
com `:-`:

```yaml
- "${KRONOS_ACERVO:?defina KRONOS_ACERVO no .env com a raiz do acervo no host}:/acervo"
- "${KRONOS_DATASET_REPO_HOST:?...}:/dataset"
```

**O `:?` não é estilo.** Até 11/08/2026 as duas linhas traziam `:-` com um
default. **Medido:** `docker compose --env-file /dev/null config` saía **0** e
imprimia `source: C:/animes` — quem esquecesse o `.env` montava em silêncio um
caminho que ninguém configurou. Um `C:/animes` que exista e não seja o acervo
entra sem um aviso; no dataset, uma pasta que não é o repositório real recebe
`git init/add/commit` do botão "Publicar Dataset" — publicação que reporta
sucesso sem publicar.

**E caminho errado nunca vira erro — medido em 11/08/2026, com o Docker no ar:**

```bash
$ docker run --rm -v "D:/caminho/que/nao/existe:/x" alpine ls -la /x
total 4
drwxrwxrwx 1 root root 4096 ...  .
drwxr-xr-x 1 root root 4096 ...  ..
# a pasta passou a EXISTIR no host, vazia, e o que se escreve nela cai lá
```

O bind mount para caminho ausente **não é recusado**: o Docker cria a pasta e o
contêiner a enxerga vazia. Com o default silencioso, portanto, apontar para o
lugar errado não produzia erro — produzia **acervo vazio**, e o KRONOS reporta
"nada a traduzir", que é o mesmo sinal de "acervo não montado".

É a mesma regra de falha fechada que a tradução de caminho na borda tem de
seguir (abaixo), aplicada uma camada antes.

**Custo assumido:** `down`, `logs` e `ps` também interpolam o arquivo, então
também passam a exigir `.env`. É o preço da recusa explícita, e a mensagem diz
qual variável falta. Nada no código do KRONOS invoca `docker compose` —
conferido em 11/08/2026, nenhum `.java`, `.js` ou `.html` o chama.

Congelado por `CatracaContainerPreparadoTest#volumesNaoTemDefaultSilencioso`,
com caso-controle próprio que cobre as três grafias de default do compose
(`${VAR}`, `${VAR:-x}`, `${VAR-x}`).

## O que ainda falta

**Tradução de caminho na borda.** A interface pede caminho absoluto do Windows
(`C:\animes\[Sokudo] DanMachi\Season 04`), e dentro do contêiner esse caminho não
existe. O acervo é montado em `/acervo`, então a borda precisa converter.
**Regra obrigatória: falhar fechado.** Caminho que não puder ser mapeado tem de
produzir erro explícito, nunca "0 arquivos encontrados" — varredura cega e pasta
vazia não podem emitir o mesmo sinal.

*(A outra pendência desta lista — `application.yml` ler o endereço do LM Studio —
foi FEITA: `application.yml:179` e `:198` trazem
`${KRONOS_LLM_BASE_URL:http://127.0.0.1:1234/v1}`, e
`CatracaContainerPreparadoTest#enderecoDoLlmSegueConfiguravel` impede a volta do
valor fixo.)*

## A suíte NÃO roda no `docker build`

`Dockerfile:33` compila com `./gradlew build -x test`. **Uma imagem verde não
prova nada sobre os testes** — a suíte pode estar vermelha há semanas e o
`docker compose build` continuaria terminando com sucesso.

Quem roda os testes é o host, antes:

```powershell
.\checar-portao.ps1        # guardas com --rerun-tasks, três estados
```

Declarado aqui, e não consertado dentro do Dockerfile, porque rodar a suíte no
build multiplicaria o tempo de imagem e ainda assim rodaria num ambiente que não
é onde se trabalha.

## O que já foi verificado

Tudo abaixo em 11/08/2026, Docker 29.3.1, notebook (32 CPU, 39,7 GB).

| Verificação | Resultado |
|---|---|
| `docker compose config` com `.env` | válido, resolve `source: C:/animes` e o repo do dataset |
| `docker compose --env-file /dev/null config` | **recusa**, saída 1: `required variable KRONOS_ACERVO is missing a value` |
| Bind mount para caminho ausente | **cria a pasta no host e o contêiner a vê vazia** — não recusa |
| Instrumento calibrado (compose) | compose doente é **reprovado** ("volumes must be a array") |
| Instrumento calibrado (catraca) | `:-` reinjetado no arquivo REAL ⇒ catraca vermelha em `CatracaContainerPreparadoTest.java:262`; restaurado e conferido por SHA-256 |
| Porta presa no loopback | `host_ip: 127.0.0.1` no config resolvido |
| `docker compose build` | ✅ `kronos-core:local`, **1,32 GB** |
| Binários externos na imagem | `/usr/bin/ffmpeg` · `/usr/bin/ffprobe` · `/usr/bin/mkvextract` · `git` · `curl` |
| Base e JVM | `eclipse-temurin:25-jre-noble` existe; `openjdk 25.0.3 LTS` dentro do contêiner |
| Usuário não-root | `uid=999(kronos) gid=999(kronos)` |
| UTF-8 no runtime | `LANG=C.UTF-8`; `[Sokudo] Pós-operatório ぼくらの` sai íntegro do `echo` no contêiner |
| Boot | `started in 1.408s. Listening on: http://0.0.0.0:8099`, `HEALTHCHECK` = **healthy** |
| Acervo montado | 25 entrada(s) em `/acervo`; painel lê **455 arquivos** do cache pelo bind mount |
| HTTP | `GET http://127.0.0.1:8099/` ⇒ **200** |
| **Tela** (bytes não provam tela) | conferida no navegador: barra lateral, menu numerado, cartões de estado, hero e CSS aplicados; **0 erro e 0 aviso** no console |

**Não verificado:** nenhum episódio foi traduzido de dentro do contêiner. O
painel exibe o LLM como "conectado" via `host.docker.internal`, mas *exibir
conectado* não é *ter traduzido* — a tradução de caminho na borda (abaixo)
continua sendo o que falta para isso valer.

**O build NÃO tem teto próprio.** Aqui ele é limitado pela VM do WSL2
(≈19,4 GB dos 39,7 GB da máquina), então o host sempre sobra. Num Docker Engine
sobre Linux, onde o daemon divide a RAM com todo o resto, esse teto não existe e
o `docker build` precisaria de `--memory`. Fica declarado, sem mecanismo: o
KRONOS não sai desta máquina.
