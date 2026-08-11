# 🚦 Catracas e Fronteiras — como as regras sobrevivem

[← Arquitetura](arquitetura.md) | [Voltar ao README](../README.md)

---

## O problema que este mecanismo resolve

Um repositório trabalhado por várias IAs — Claude, Gemini, GPT, Codex, Cursor — e por um humano
com pressa tem um modo de falha próprio: **a regra combinada some**. Não porque alguém discorde
dela, mas porque ninguém a lê no momento em que ela importa.

Documentação não impõe nada. Um arquivo de instruções pode ser ignorado por qualquer agente que
abra o projeto amanhã. **Um teste vermelho, não.**

Daí a peça central da arquitetura do KRONOS: **13 guardas executáveis**, 62 testes, que não
verificam comportamento — verificam que **um padrão perigoso não voltou**. Elas leem o
código-fonte, a estrutura de pacotes ou o HTML, e **reprovam o build** ao encontrar a forma do
bug.

```mermaid
graph LR
    DEV["👤 Humano ou IA<br/>escreve código"] --> BUILD["🔨 gradlew test"]
    BUILD --> G1{"🚦 Guarda<br/>vê a forma do bug?"}
    G1 -->|não| OK["✅ merge"]
    G1 -->|sim| RED["🛑 BUILD VERMELHO<br/>com arquivo, linha e<br/>a forma esperada"]
    RED --> FIX["🔧 conserta<br/>ou homologa por escrito"]
    FIX --> BUILD

    DOC["📄 CLAUDE.md<br/>instruções, convenções"] -. "pode ser ignorado<br/>sem nenhum sinal" .-> DEV

    classDef ator fill:#1e293b,stroke:#3B82F6,color:#F9FAFB
    classDef guarda fill:#7c2d12,stroke:#FB923C,color:#F9FAFB,stroke-width:2px
    classDef ruim fill:#7f1d1d,stroke:#F87171,color:#F9FAFB
    classDef bom fill:#14532d,stroke:#4ADE80,color:#F9FAFB
    classDef fraco fill:#1e293b,stroke:#6B7280,color:#9CA3AF
    class DEV,BUILD,FIX ator
    class G1 guarda
    class RED ruim
    class OK bom
    class DOC fraco
```

---

## As duas famílias

```mermaid
graph TB
    subgraph A["🧱 Fronteira*ArchTest — 9 guardas, 51 testes"]
        direction TB
        A1["Allowlist por TIPO EXATO,<br/>nunca por prefixo de pacote"]
        A2["Uma dependência nova reprova<br/>listando a aresta que apareceu"]
        A3["Homologar é decisão consciente:<br/>entra na lista, com justificativa"]
    end

    subgraph B["🔒 Catraca*Test — 4 guardas, 11 testes"]
        direction TB
        B1["Varre o FONTE atrás da<br/>forma de um defeito conhecido"]
        B2["Sem número congelado<br/>quando o invariante é universal"]
        B3["Multiforma: cobre as grafias<br/>alternativas do mesmo padrão"]
    end

    A --> C["🎯 Efeito comum:<br/>a regra sobrevive à troca de modelo de IA"]
    B --> C

    classDef fa fill:#14532d,stroke:#4ADE80,color:#F9FAFB
    classDef fb fill:#4c1d95,stroke:#A78BFA,color:#F9FAFB
    classDef fc fill:#7c2d12,stroke:#FB923C,color:#F9FAFB
    class A1,A2,A3 fa
    class B1,B2,B3 fb
    class C fc
```

**Fronteira** congela *quem pode importar quem*. **Catraca** congela *que forma não pode voltar
a existir*. A diferença prática: a fronteira olha o grafo de dependências; a catraca lê o texto
do código.

---

## O inventário completo

| Guarda | Testes | O que impede |
|--------|-------:|--------------|
| `FronteiraTraducaoArchTest` | 15 | A fatia `traducao` (tier gold) ganhar qualquer aresta de saída para outra fatia funcional. Congela os tipos de `core` consumidos, um a um |
| `FronteiraContextoArchTest` | 8 | O peer `contexto` (72 lores) passar a depender de fatia funcional |
| `FronteiraTrocaTipoLegendaArchTest` | 6 | A regra de negócio do achatamento voltar para dentro da fatia, desfazendo o desacoplamento de 29/07/2026 |
| `FronteiraCorretorCacheArchTest` | 5 | As quatro fatias da área de correção de cache se enroscarem de novo |
| `FronteiraQualidadeTraducaoArchTest` | 5 | O peer de máscara de tags e validação perder independência |
| `FronteiraLlmArchTest` | 4 | O contrato `LlmPort` vazar detalhe de provedor — é o que permite trocar o LLM sem tocar no pipeline |
| `FronteiraLegendaArchTest` | 3 | O peer de modelo e I/O de `.ass`/`.srt` depender de quem o consome |
| `FronteiraCacheTraducaoArchTest` | 3 | Outra fatia escrever cache paralelo. `cachetraducao` é **dono único** |
| `FronteiraInboundArchTest` | 2 | Entrada de fora do sistema cruzar direto para dentro sem passar pela porta |
| `CatracaAgregadorasForaDoCdiTest` | 4 | Alguém "consertar" a ausência deliberada de `@Component` nas lores agregadas. A ausência é decisão de qualidade de tradução, não esquecimento |
| `CatracaSlotsReservadosLoreTest` | 3 | A dívida de obras sem lore ficar invisível — o id resolveria para nada e o operador não veria |
| `CatracaFronteiraQuebraAssTest` | 2 | Uma fronteira de termo esquecer que `\N` do ASS são **dois caracteres** e o `N` é letra |
| `CatracaRegraDuplicadaEntreFatiasTest` | 2 | Duplicação silenciosa. Duplicar é permitido — esconder que duplicou, não |

---

## As três regras de operação

### 1. Guarda nasce de prejuízo real, nunca de bug hipotético

Guarda preventiva é cerimônia, e cerimônia é a primeira coisa abandonada na pressa. Cada uma das
13 carrega, no próprio javadoc, o incidente que a originou. Exemplos verdadeiros:

- `CatracaRegraDuplicadaEntreFatiasTest` — *"cópia não declarada foi a causa de três defeitos em
  03/08/2026, um deles vivo por nove dias."*
- `CatracaFronteiraQuebraAssTest` — a metade que faltava da mecânica corrompeu tradução correta:
  reverteu `Nahel Argama` para `Argama` num caso e produziu `Nahel\NNahel Argama` na tela no
  outro.
- `FronteiraContextoArchTest` — 15 caches de Gundam 0083 gravados com `contextoId = guilty_crown`.

### 2. Guarda tem de ser vista REPROVANDO um caso-controle

Uma guarda exercitada apenas no código são pode estar passando **por não enxergar nada**. Antes
de confiar no verde, injeta-se o defeito à mão e confirma-se que ela morre.

Caso real, 05/08/2026: a `CatracaFronteiraQuebraAssTest` procurava **uma** grafia de fronteira e
por isso afirmava cobertura que não tinha. Passou a cobrir nove grafias, e cada uma foi vista
reprovando um arquivo de caso-controle — **9 de 9**, com a grafia certa nomeada na mensagem. Na
primeira execução real ela encontrou um ponto cego que estava verde havia meses.

### 3. Número congelado só quando não há invariante universal

Baseline numérica ("no máximo N ocorrências") é aceitável quando congelar por tipo é inviável —
mas **número se ajusta de boa-fé**, e já foi ajustado neste repositório: uma IA com permissão de
commit baixou o total de 14 para 11 no mesmo commit em que "consertava" algo, porque a própria
refatoração cegou o scanner.

Quando o invariante é universal, a guarda nasce **sem número**. É o caso da
`CatracaFronteiraQuebraAssTest`: não existe "quantidade aceitável" de fronteira sem tratar a
quebra, então não há o que ajustar para baixo.

---

## ⚠️ A armadilha do verde falso

O cache do Gradle **já produziu falso-verde** em teste de arquitetura neste projeto. A guarda lê
arquivos-fonte, e o Gradle considera a tarefa atualizada quando as entradas declaradas não
mudaram — só que o arquivo violado pode não estar entre elas.

```bash
# ERRADO — pode passar verde sem ter rodado
./gradlew test --tests "*Catraca*"

# CERTO
./gradlew test --tests "*Catraca*" --rerun-tasks
```

Isso vale para toda verificação de arquitetura, e vale em dobro para IA com permissão de commit:
**conferir o número da catraca em todo commit dela e exigir a mutação.**

---

## Guardas que não se chamam "Catraca"

Duas moram fora do pacote `arquitetura` e fazem o mesmo trabalho:

- **`WebInterfaceTest#indexSidebarComEstruturaNavMenuValida`** — cobra a numeração do menu como
  invariante: no grupo de ordem G, o item de posição N começa com `G.N`. Antes de 05/08/2026 ela
  congelava o rótulo literal, o que quebrava a cada renomeação e — pior — **não via item novo
  entrando sem número**, que foi exatamente como nasceu o antigo `4b.`.
- **`SseConsoleDinamicoTest#batimentoCarregaIdentidadeDaExecucaoENaoORelogio`** — impede que o
  batimento SSE volte a carregar o relógio em vez da identidade da execução. Com o relógio, o
  navegador não distingue "servidor reiniciou" de "continuo conectado", e o console fica exibindo
  a execução morta.

---

## O endereço único: `checar-portao.ps1`

```powershell
.\checar-portao.ps1
```

Um comando roda **todas** as guardas. Guarda que depende de alguém lembrar de rodar é
documentação com sorte.

**Três estados, nunca dois:**

| Saída | Significado |
|---|---|
| `0` | pode trabalhar |
| `1` | defeito real — guarda vermelha, hash divergente, documento sumido, compilação quebrada |
| `2` | **não deu para conferir** — sem Java, sem `gradlew`, zero teste selecionado |

O `2` não é detalhe. Sem ele, um ambiente sem Java e um repositório impecável produzem
exatamente o mesmo silêncio, e "não verificou" passa por aprovação.

**`--rerun-tasks` é obrigatório e o script o impõe.** O prejuízo está registrado: uma IA
comitou alteração de lore com a suíte "verde" que era `UP-TO-DATE` do cache do Gradle —
nenhum teste havia rodado. Verde de cache é indistinguível de verde de execução, a não ser
que se force a execução.

**Cobertura declarada, não presumida.** O filtro pega a convenção de nome (`Catraca*`,
`Fronteira*`, `Guarda*`) **mais** as duas guardas da seção acima, nomeadas explicitamente no
script. Guarda nova com nome fora da convenção não entra sozinha — acrescente na lista do
`$FILTROS`. Medido em 11/08/2026: 154 testes em 31 classes, 30 s.

O passo 1 do portão confere `.claude/LEITURA-REGRA-ATUAL.md`: o SHA-256 e a contagem de linhas
de cada documento-regra têm de bater com o arquivo em disco. Sem isso, "eu li a regra" é
lembrança — e lembrança não distingue a versão atual da de duas semanas atrás.

> A própria conferência nasceu com o defeito que ela existe para pegar. Na primeira execução,
> um hash adulterado para `...FFFF` não casava com o padrão `[0-9a-f]{64}` embutido na regex:
> a linha era **descartada em silêncio**, o contador caía de 3 para 2 e o portão respondia
> `OK` e saía `0`. Guarda que descarta o que não entende aprova por cegueira. Hoje linha
> malformada é defeito, e os três casos doentes (hash errado, hash malformado, contagem
> errada) foram vistos reprovando.

---

## Como escrever uma guarda nova

1. **Tenha o prejuízo.** Sem incidente concreto, não entra.
2. **Escolha a família.** Aresta entre pacotes → `Fronteira*ArchTest`. Forma no texto do código →
   `Catraca*Test`.
3. **Cubra as grafias alternativas.** Buscar uma forma mede a forma, não o invariante.
4. **Monte o caso-controle e veja a guarda morrer.** Só então confie no verde.
5. **Escreva na mensagem de falha o que fazer** — arquivo, linha, forma esperada pronta para
   colar. Quem vai ler é alguém com pressa, possivelmente uma IA.
6. **Documente o incidente no javadoc.** A guarda sem cicatriz será apagada por alguém que a
   achou chata.

---

[← Arquitetura](arquitetura.md) | [Voltar ao README](../README.md)
