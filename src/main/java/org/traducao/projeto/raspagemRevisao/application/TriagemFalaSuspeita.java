package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DecisaoFala;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: a última pergunta antes de a fala custar dinheiro — "isto está errado?".
 * Só o que sai daqui como suspeito chega ao corretor determinístico, à memória do arquivo ou a uma
 * chamada ao LLM/Google.
 *
 * <h2>Não confundir com o filtro de linha</h2>
 * {@link FiltroAuditoriaLinha} responde "isto é diálogo?" — pergunta ESTRUTURAL, sobre estilo, tags
 * e karaokê, e é feita antes de qualquer trabalho. Esta classe responde "este diálogo está errado?"
 * — pergunta SEMÂNTICA, que compara o inglês com o português. Uma placa nunca chega aqui; uma fala
 * perfeita chega e é dispensada.
 *
 * <h2>A exceção da lore vem primeiro, e não é otimização</h2>
 * Uma fala PT idêntica ao inglês normalmente é tradução que não aconteceu. Mas quando o inglês é só
 * nome próprio ou termo canônico da obra — "Hyaku Shiki", "Axis", "Newtype" —, ficar idêntico é
 * estar CERTO: o termo não se traduz. Sem esta exceção, uma obra com muitos nomes próprios veria
 * suas falas corretas mandadas ao tradutor, que devolveria "Cem Tipos". Por isso a checagem de lore
 * vem ANTES da auditoria, e não depois como um caso especial a remendar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Dispensar é sempre {@code Manter}: a fala entra no documento intocada. Dispensa NÃO é
 *       pendência — não há problema a relatar, então não conta como trabalho a fazer.</li>
 *   <li>Quando devolve {@code Suspeita}, entrega a auditoria JUNTO. Ela é a base de comparação do
 *       portão de segurança rio abaixo: sem ela não há como dizer se uma proposta melhorou. Auditar
 *       de novo mais adiante seria pagar duas vezes e arriscar duas respostas diferentes.</li>
 *   <li>NÃO imprime e NÃO conta. Ver {@link DecisaoFala}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem original inglês, a auditoria roda mesmo assim — é o modo LLM_CONCORDANCIA, que revisa a
 * concordância do português sozinho. A exceção da lore exige o original e simplesmente não se
 * aplica. Nunca lança.
 */
@Service
public class TriagemFalaSuspeita {

    private final ProtetorTermosLoreService protetorLore;
    private final AuditorProblemasLegendaService auditor;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne o conhecimento de lore e o detector de problemas.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação da triagem.
     */
    public TriagemFalaSuspeita(
            ProtetorTermosLoreService protetorLore,
            AuditorProblemasLegendaService auditor) {
        this.protetorLore = protetorLore;
        this.auditor = auditor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o que a triagem apurou sobre a fala.
     *
     * <p>INVARIANTES DO DOMÍNIO: os dois casos são excludentes por construção — ou a fala está
     * resolvida e vem com a decisão pronta, ou é suspeita e vem com a auditoria que justifica o
     * trabalho. Não existe "suspeita sem auditoria" nem "dispensada com auditoria".
     */
    public sealed interface Resultado {

        /**
         * A fala não precisa de correção.
         *
         * @param decisao sempre um {@link DecisaoFala.Manter}, com a narração quando houver
         * @param somenteTermoCanonico a fala era só nome/termo da lore e por isso nem foi à IA —
         *     marcado para o caso de uso CONTAR em vez de narrar cada uma
         */
        record Dispensada(DecisaoFala decisao, boolean somenteTermoCanonico) implements Resultado {

            /** A dispensa comum: nada a relatar, nada a contar. */
            Dispensada(DecisaoFala decisao) {
                this(decisao, false);
            }
        }

        /**
         * A fala tem problema e vale o trabalho.
         *
         * @param auditoria os motivos da suspeita, base de comparação de qualquer proposta
         */
        record Suspeita(ResultadoDeteccaoConcordancia auditoria) implements Resultado {
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se esta fala segue para correção.
     *
     * <p>INVARIANTES DO DOMÍNIO: a exceção da lore exige as TRÊS condições juntas — haver original,
     * o português ser idêntico a ele (ignorando espaçamento) e o original conter SOMENTE termos
     * canônicos. Duas delas não bastam: "Axis is falling" também contém um termo canônico, mas tem
     * texto a traduzir em volta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     *
     * @param evento a fala, usada só para a mensagem ao operador
     * @param originalEn o inglês de referência, ou {@code null} no modo sem original
     * @param traducaoAtual a fala como está hoje
     * @param temOriginalEn se há original utilizável
     * @param contexto lore e termos protegidos da obra
     */
    public Resultado triar(
        EventoLegenda evento,
        String originalEn,
        String traducaoAtual,
        boolean temOriginalEn,
        ContextoRevisao contexto
    ) {
        if (temOriginalEn
            && normalizar(originalEn).equals(normalizar(traducaoAtual))
            && protetorLore.contemSomenteTermosCanonicos(
                originalEn, contexto.lore(), contexto.termosProtegidos())) {
            // NÃO narra por evento: numa corrida do Zeta isso produziu 1.053 linhas dizendo "nada
            // aconteceu aqui", e elas enterravam os 10 achados que pediam ação. O caso de uso
            // CONTA e imprime uma linha por arquivo. Paulo, 17/08/2026: "por isso me confundia".
            return new Resultado.Dispensada(new DecisaoFala.Manter(List.of()), true);
        }

        ResultadoDeteccaoConcordancia auditoria = auditor.auditar(originalEn, traducaoAtual);
        if (!auditoria.suspeito()) {
            return new Resultado.Dispensada(new DecisaoFala.Manter(List.of()));
        }
        return new Resultado.Suspeita(auditoria);
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }
}
