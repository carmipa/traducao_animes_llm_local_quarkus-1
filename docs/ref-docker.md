# Execução em contêiner

> Estado em 06/08/2026: **artefatos escritos, imagem nunca construída.** As duas
> mudanças de código que faltam estão listadas em "O que ainda falta". Enquanto
> elas não entrarem, `docker compose up` sobe a aplicação mas a tradução falha no
> primeiro lote.

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

## O que ainda falta

Duas mudanças de código, adiadas porque exigem reinício da aplicação e havia uma
tradução em curso em 06/08/2026:

1. **`application.yml` ler a variável do LM Studio.** As chaves
   `tradutor.llm.base-url` e `revisao-lore.llm.base-url` apontam fixo para
   `http://127.0.0.1:1234/v1`. Dentro do contêiner, `127.0.0.1` é o próprio
   contêiner: a tradução morre no primeiro lote com recusa de conexão. Precisam
   virar `${KRONOS_LLM_BASE_URL:http://127.0.0.1:1234/v1}` — o default preserva
   a execução fora do contêiner.

2. **Tradução de caminho na borda.** A interface pede caminho absoluto do
   Windows (`C:\animes\[Sokudo] DanMachi\Season 04`), e dentro do contêiner esse
   caminho não existe. O acervo é montado em `/acervo`, então a borda precisa
   converter. **Regra obrigatória: falhar fechado.** Caminho que não puder ser
   mapeado tem de produzir erro explícito, nunca "0 arquivos encontrados" —
   varredura cega e pasta vazia não podem emitir o mesmo sinal.

## O que já foi verificado

| Verificação | Resultado |
|---|---|
| `docker compose config` | válido, variáveis resolvem nos defaults |
| Instrumento calibrado | compose doente é **reprovado** ("volumes must be a array") |
| Porta presa no loopback | `host_ip: 127.0.0.1` no config resolvido |

**Não verificado:** a imagem nunca foi construída — o Docker Desktop estava
parado e o build competiria por CPU com a tradução em curso. Portanto o
`apt-get` dos binários, a existência da tag `eclipse-temurin:25-jre-noble` e o
boot da aplicação seguem sem prova.
