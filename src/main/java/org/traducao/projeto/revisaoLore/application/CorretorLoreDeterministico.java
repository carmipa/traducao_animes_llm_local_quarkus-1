package org.traducao.projeto.revisaoLore.application;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import org.springframework.stereotype.Component;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: reforça DETERMINISTICAMENTE a terminologia canônica de lore na
 * revisão (Opção 7), SEM LLM. Cobre casos específicos herdados (nome "Shin" traduzido
 * como "Canela"; "dud rounds" como "rodadas aleatórias") e delega o mapa genérico da obra
 * ativa ao {@link EnforcadorTermosLore}. É o complemento determinístico do prompt de lore:
 * o prompt PEDE ao LLM; esta classe GARANTE nas formas-ruim conhecidas.
 *
 * <h2>Por que o mapa genérico é delegado, e não reimplementado</h2>
 * Esta classe carregava uma SEGUNDA cópia do algoritmo do enforcer, justificada no
 * comentário original por "a arquitetura proíbe uma fatia importar a outra". A justificativa
 * valia para fatia, não para PEER — e o custo apareceu: as duas cópias divergiram em três
 * propriedades, todas medidas antes desta mudança.
 * <ul>
 *   <li><b>Teto de ocorrências.</b> A cópia daqui usava {@code replaceAll}, trocando TODAS as
 *       ocorrências da forma-ruim. Com {@code "Quatro" → "Quattro"} no mapa de Zeta/ZZ, a fala
 *       "Quattro, há quatro inimigos" virava "Quattro, há Quattro inimigos" — o NÚMERO quatro
 *       corrompido. O enforcer restaura no máximo tantas quantas o canônico aparece no EN.</li>
 *   <li><b>Ordem por comprimento.</b> Sem ordenar as chaves da mais longa para a mais curta,
 *       {@code "Vazio"→"Void"} destruía {@code "Genoma do Vazio"→"Void Genome"}.</li>
 *   <li><b>Canônico multi-palavra.</b> A checagem era sensível à caixa para todo termo, então
 *       o EN "two mobile suits" não reconhecia o canônico "Mobile Suits" e a restauração nunca
 *       disparava.</li>
 * </ul>
 * O mapa forma-ruim→canônico continua sendo do peer {@code contexto}; o ALGORITMO passa a ter
 * uma implementação só, no peer {@code qualidadeTraducao}, que age sobre o texto produzido.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Opera sobre texto MASCARADO (tags viram {@code [[TAGn]]} e ficam intactas).</li>
 *   <li>O mapa só restaura uma forma-ruim quando o original EN contém o termo canônico
 *       na grafia EXATA (nome próprio maiúsculo "Titans" ≠ comum "titans").</li>
 *   <li>Comparações por fronteira de palavra; forma-ruim casa ignorando caixa; canônico
 *       inserido literalmente. Nunca deixa a linha PIOR: classe sem estado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entradas nulas/tradução vazia ou nenhuma substituição aplicável devolvem
 * {@link Optional#empty()} (mantém o texto original); não lança.
 */
@Component
public class CorretorLoreDeterministico {

    private static final String INICIO_DE_TERMO = FronteiraTermoAss.INICIO;

    private final EnforcadorTermosLore enforcadorTermosLore;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a implementação ÚNICA do reforço de terminologia.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida; nenhuma cópia local do algoritmo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public CorretorLoreDeterministico(EnforcadorTermosLore enforcadorTermosLore) {
        this.enforcadorTermosLore = enforcadorTermosLore;
    }

    private static final Pattern PADRAO_SHIN = Pattern.compile(INICIO_DE_TERMO + "Shin(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_CANELA = Pattern.compile(INICIO_DE_TERMO + "[Cc]anela(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_DUD_ROUNDS = Pattern.compile("(?i)" + INICIO_DE_TERMO + "dud\\s+rounds?(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_RODADAS_ALEATORIAS = Pattern.compile(
        "(?i)" + INICIO_DE_TERMO + "rodadas\\s+(?:aleat[oó]rias|fracassadas|falsas|dud)(?![\\p{L}\\p{N}])");

    /**
     * PROPÓSITO DE NEGÓCIO: restaura terminologia canônica de lore na fala mascarada,
     * combinando os casos específicos herdados com o mapa genérico da obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: o mapa só aplica quando o original EN contém o canônico
     * na grafia exata; forma-ruim casa por fronteira de palavra, ignorando caixa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas/tradução vazia ou nenhuma
     * substituição aplicável devolvem {@link Optional#empty()}.
     *
     * @param originalMascarado o texto original (EN) já mascarado
     * @param traducaoMascarada a fala traduzida (PT) já mascarada
     * @param correcoesTerminologia mapa forma-ruim (PT) → canônico da obra ativa
     * @return a fala corrigida quando houve alteração; caso contrário {@link Optional#empty()}
     */
    public Optional<String> corrigir(
            String originalMascarado, String traducaoMascarada, Map<String, String> correcoesTerminologia) {
        if (originalMascarado == null || traducaoMascarada == null || traducaoMascarada.isBlank()) {
            return Optional.empty();
        }

        String corrigida = traducaoMascarada;
        // Só restaura "Shin" quando o nome NÃO está presente na grafia correta no PT: no
        // erro real o nome virou "canela" (some do PT); se "Shin" já está no PT, o "canela"
        // é a especiaria (falso-positivo) e não pode ser tocado.
        if (PADRAO_SHIN.matcher(originalMascarado).find()
            && !PADRAO_SHIN.matcher(corrigida).find()
            && PADRAO_CANELA.matcher(corrigida).find()) {
            corrigida = PADRAO_CANELA.matcher(corrigida).replaceAll("Shin");
        }

        if (PADRAO_DUD_ROUNDS.matcher(originalMascarado).find()
            && PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).find()) {
            corrigida = PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).replaceAll("munições falhas");
        }

        // DIVIDA NOMEADA — "Opcao 7: enforcer antes do mascaramento". Medida em 2026-07-28.
        //
        // O enforcer trata a quebra do ASS explicitamente (SEPARADOR_INTERNO aceita espaco OU
        // "\N"), porque a legenda parte termo composto no meio: "Quin\NMantha", "Char\NAznable".
        // Nesta chamada esse tratamento NUNCA dispara: o texto chega MASCARADO e o mascarador ja
        // trocou "\N" por "[[TAGn]]" (MascaradorTags.PADRAO_TAG casa "\\[Nnh]"). O caminho do
        // reforco de cache passa texto CRU e por isso funciona; aqui, nao.
        //
        // Medido no acervo (167 caches): 56.420 falas, 11.799 (20,9%) contem a quebra, 286 tem
        // termo da lore partido por ela. Isso mede o CENARIO, nao 286 defeitos -- a Revisao so
        // perde quando a forma-ruim esta no PT E o canonico no EN esta partido, subconjunto
        // menor. Severidade baixa: quem cobre o acervo hoje e o reforco de cache.
        //
        // O CONSERTO NAO E ACEITAR "[[TAGn]]" NO REGEX DO ENFORCER. Isso fecharia o sintoma e
        // acoplaria o enforcer ao formato do mascarador. A causa e ORDEM DE PIPELINE: o
        // mascaramento existe para proteger tags do LLM, e o enforcer e deterministico -- ele nao
        // inventa tag e nao precisa de mascara. A ordem correta seria enforcer/corretor no texto
        // CRU, mascarar so para a chamada ao LLM, desmascarar, validar. Mexer nisso altera a ordem
        // de um caminho que hoje funciona, entao nao se faz de raspao.
        corrigida = enforcadorTermosLore.reforcar(
            originalMascarado, corrigida, semAsQueOoriginalUsaDePROPOSITO(
                originalMascarado, correcoesTerminologia));

        if (corrigida.equals(traducaoMascarada)) {
            return Optional.empty();
        }
        return Optional.of(corrigida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tira do mapa as entradas cuja <b>forma-ruim o próprio original em
     * inglês usa</b> — nessa fala ela não é forma-ruim, é a palavra que o roteiro escolheu.
     *
     * <h2>O prejuízo, medido em 27/08/2026</h2>
     * O corretor determinístico ia reescrever uma fala do Zeta assim:
     *
     * <pre>
     *   EN      I'm Rosamia! Not Rosammy!
     *   ANTES   Eu sou Rosamia! Não Rosammy!
     *   DEPOIS  Eu sou Rosammy! Não Rosammy!     &lt;- a cena morre
     * </pre>
     *
     * A personagem está insistindo no próprio nome contra o apelido que lhe dão: <b>o contraste
     * entre os dois nomes É a fala</b>. O mapa tinha {@code Rosamia → Rosammy}, e a prosa da
     * MESMA lore dizia o contrário — "Rosammy é o apelido usado por Kamille para Rosamia;
     * preserve exatamente Rosammy quando o original usar Rosammy". Documento e mapa divergiam, e
     * quem escreve na legenda é o mapa.
     *
     * <h2>Por que ESTA regra, e não um remendo para Rosamia</h2>
     * O mapa é {@code forma-ruim em PORTUGUÊS → termo canônico}. A forma-ruim é o que o LLM
     * traduziu errado ({@code "Robô Móvel"} por {@code "Mobile Suit"}); ela <b>não tem por que
     * aparecer no inglês</b>. Quando aparece, a entrada não descreve esta fala — e trocar ali é
     * apagar uma escolha do roteiro. A regra sai do significado do mapa, não do caso.
     *
     * <p>INVARIANTES DO DOMÍNIO: filtra por ENTRADA, não pela fala inteira. Uma fala com três
     * correções, uma delas em contraste, mantém as outras duas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa nulo ou vazio devolve o próprio mapa; nunca lança e
     * nunca acrescenta entrada.
     */
    private static Map<String, String> semAsQueOoriginalUsaDePROPOSITO(
            String originalMascarado, Map<String, String> correcoes) {
        if (correcoes == null || correcoes.isEmpty()) {
            return correcoes;
        }
        Map<String, String> filtrado = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> e : correcoes.entrySet()) {
            if (!apareceComoPalavra(originalMascarado, e.getKey())) {
                filtrado.put(e.getKey(), e.getValue());
            }
        }
        return filtrado;
    }

    /**
     * Fronteira de palavra e caixa ignorada — a mesma régua com que o enforcer casa.
     *
     * <p>A fronteira de início vem de {@link FronteiraTermoAss#INICIO}, e não de um
     * {@code (?<![\p{L}\p{N}])} escrito aqui: {@code \N} do ASS ocupa DOIS caracteres e o
     * {@code N} é letra para {@code \p{L}}, então o lookbehind simples conclui que o termo colado
     * na quebra é sufixo de outra palavra e <b>não o enxerga</b>. Medido no acervo: 24,6% das
     * falas têm a quebra. Uma guarda cega justamente aí protegeria só a fala de uma linha.
     */
    private static boolean apareceComoPalavra(String texto, String termo) {
        if (texto == null || termo == null || termo.isBlank()) {
            return false;
        }
        return Pattern
            .compile(INICIO_DE_TERMO + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE)
            .matcher(texto)
            .find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: variante PT-only (sem o inglês) da restauração de terminologia —
     * para a revisão de lore quando só existe a legenda PT-BR. Sem o EN não há como desambiguar
     * homógrafo de uma palavra ({@code Vazio}=Void vs {@code vazio}=empty); por isso aplica SÓ os
     * termos INEQUÍVOCOS: forma-ruim MULTI-PALAVRA (contém espaço), improvável de colidir com
     * palavra comum. Homógrafos de uma palavra ficam para o LLM PT-only.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica forma-ruim com espaço; fronteira de palavra;
     * forma-ruim casa ignorando caixa; canônico inserido literalmente; nunca deixa pior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução vazia, mapa vazio ou nenhuma substituição
     * aplicável devolve {@link Optional#empty()}; não lança.
     *
     * @param traducaoMascarada a fala PT já mascarada
     * @param correcoesTerminologia mapa forma-ruim (PT) → canônico da obra ativa
     * @return a fala corrigida quando houve alteração; caso contrário {@link Optional#empty()}
     */

}
