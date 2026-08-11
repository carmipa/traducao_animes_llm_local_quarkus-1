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
| `docs/catracas-e-fronteiras.md` | 229 | `ce94bc2124f2f27077b18b83f937bf58fbfa4db306871a883aa969f9a74f43fb` | 2026-08-11 |
| `docs/ref-docker.md` | 164 | `07d8bd6feffe12da181b28929e18137fcd86e09f4a0ceed1c9a5aef0f4a606cb` | 2026-08-11 |
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

O que ficou de fora, e por quê:

- Guardas de tenant, RLS e `casa_id`: o KRONOS não tem banco nem inquilino. Guarda que não
  protege nada ensina a ignorar as que protegem.
- `build.sh` / `deploy.sh` do modelo de lá: aqui não há publicação — o KRONOS roda na máquina
  de quem usa. "Construir não é publicar" não tem análogo neste projeto.
- Conferência visual obrigatória por captura de tela: o KRONOS tem interface web, mas o dano
  documentado de lá (botão flutuante engolindo elemento) não tem incidente correspondente
  aqui. Sem cicatriz, é cerimônia.
