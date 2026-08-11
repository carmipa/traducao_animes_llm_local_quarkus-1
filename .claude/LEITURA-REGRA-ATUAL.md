# Portão de leitura — comprovante da última leitura das regras do KRONOS

Sem este arquivo, "eu li a regra" é **lembrança** — e lembrança não distingue a versão atual
da de duas semanas atrás. O comprovante registra o SHA-256 do que foi lido; se o documento
mudou depois, a divergência aparece.

**Quem confere é `checar-portao.ps1`, não a boa vontade de quem lê.** Comprovante que ninguém
verifica é documentação com sorte.

## Como atualizar

Leia o documento **inteiro**, depois:

```powershell
Get-FileHash docs/catracas-e-fronteiras.md -Algorithm SHA256
```

e substitua o hash e a data abaixo. Trocar o hash sem ler é o único jeito de burlar isto —
e é exatamente o tipo de coisa que não se automatiza.

---

## Documentos-regra deste repositório

| Documento | Linhas | SHA-256 | Lido em |
|---|---|---|---|
| `docs/catracas-e-fronteiras.md` | 293 | `897e1af58972efcabea5abba1f22ffe1f99e6a174170602b723b810c05e9c8a7` | 2026-08-11 |
| `docs/ref-docker.md` | 213 | `708287a07545735a2e10fc0ea2123b8fe178c0250b81538d65f3ec57a5cc4910` | 2026-08-11 |
| `docs/ref-memoria-decisoes-ia.md` | 54 | `a1c890d7b0487227f069ae511a96db4f08eb7cd01ab437b7cf2c860472aedd20` | 2026-08-11 |

## O que NÃO entra nesta tabela

- **`~/.claude/CLAUDE.md`** (contrato global do Paulo) e o vault `C:\cerebro_de_ia` — moram
  fora do repositório e valem em todos os projetos. Um hash aqui envelheceria a cada sessão
  em qualquer outro projeto.
- **`instrucoes/regra-java-quarkus-qute.md` do vault.** Apesar da precedência por stack, ela
  nasceu de OUTRO projeto (Cheffzy, multi-tenant, VPS) e **não prende o KRONOS**, que é local
  e não vai para VPS. Decisão registrada; não reabrir por conta própria.

---

## Leitura de 2026-08-11 — o que a tarefa afetou

Origem: os três documentos do site do Christiano Carminati (`REGRA-DESTE-DOCKER.md`,
`LEITURA-REGRA-ATUAL.md`, `DEPLOY-NESTA-VPS.md`), trazidos por Paulo para avaliação do que
se aplica aqui. **Método trazido, mecanismo não** — o site é institucional, sem banco e numa
VPS; o KRONOS é local e nunca vai para VPS.

O que entrou:

- **`:?` obrigatório** nos dois caminhos de HOST do `docker-compose.yml`. Estado anterior
  provado fail-open: `docker compose --env-file /dev/null config` saía **0** resolvendo
  `source: C:/animes`. Agora sai **1** dizendo qual variável falta.
- **Catraca** `CatracaContainerPreparadoTest#volumesNaoTemDefaultSilencioso`, com caso-controle
  para as três grafias de default (`${VAR}`, `${VAR:-x}`, `${VAR-x}`) e calibrada contra o
  arquivo real doente.
- **`-x test` do `Dockerfile:33` declarado por escrito** em `docs/ref-docker.md` — imagem verde
  não prova suíte verde.
- **`checar-portao.ps1`**: endereço único das guardas, **três estados** (0/1/2) e
  `--rerun-tasks` obrigatório.

Na segunda passada, com o Docker no ar, entrou também:

- **Falha fechada na borda de caminho.** Sondadas 9 rotas com pasta inexistente: **7
  respondiam HTTP 200/202 "iniciada"** para trabalho impossível, incluindo as duas que gravam
  no acervo. `core.io.GuardaCaminhoEntrada` recusa antes de enfileirar, congelada por
  `CatracaBordaAssincronaConfereCaminhoTest`. Não era problema de contêiner: caminho digitado
  errado no Windows dava o mesmo silêncio.
- **Definition of Done** e **"Construir não é publicar"**, em
  [[docs/catracas-e-fronteiras.md]] e [[docs/ref-docker.md]].

A varredura item a item da REGRA de lá — o que entrou, o que ficou e por quê — está na tabela
"O que veio do site do Christiano e o que ficou de fora" em `docs/catracas-e-fronteiras.md`,
que é o lugar onde ela é lida por quem for mexer no projeto. Resumo dos "não":
tenant/RLS/`casa_id` (não há banco), `build.sh`/`deploy.sh` (não há publicação), CSP (não há, e
os 40 `style=` funcionam numa aplicação só de loopback), canto inferior direito (medido: um só
ocupante, e sem cicatriz local guarda é cerimônia).
