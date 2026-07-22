package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

/**
 * PROPÓSITO DE NEGÓCIO: avalia a QUALIDADE de uma tradução para o banco bilíngue — decide
 * se uma entrada de cache pode ser reaproveitada e se um resultado final é uma tradução
 * válida ou deve permanecer pendente —, isolando essa política da orquestração de
 * {@link ProcessarArquivoUseCase} (FASE F, R5).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cache só é reaproveitado quando preserva a estrutura de tags do original e passa
 *       na validação; uma tradução idêntica ao original só vale se a lore mandar mantê-la
 *       (nome, sigla, número, termo protegido).</li>
 *   <li>Vazio, tags divergentes e resíduo em inglês nunca contam como tradução concluída.</li>
 *   <li>A normalização de comparação colapsa espaços em branco para casar variações
 *       triviais sem alterar o texto persistido.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A {@link AlucinacaoDetectadaException} lançada pela VALIDAÇÃO é o canal esperado e é convertida
 * em resultado de domínio: {@link #isCacheReaproveitavel} vira {@code false} e
 * {@link #motivoFalhaFinal} vira o motivo legível (uma tradução válida devolve {@code null}).
 * Qualquer OUTRA falha inesperada das dependências (ex.: {@link NullPointerException}) NÃO é
 * ocultada — propaga para o chamador em vez de mascarar um problema real.
 */
@Component
public class AvaliadorTraducaoCache {

    private static final Logger log = LoggerFactory.getLogger(AvaliadorTraducaoCache.class);

    private final MascaradorTags mascarador;
    private final DetectorTraducaoIdenticaService detectorIdentica;
    private final ValidadorTraducaoService validador;
    private final VerificadorIdentificadorNumerico verificadorNumerico;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as blindagens de qualidade — estrutura de tags, decisão de
     * identidade legítima e validação de resíduo — que sustentam a política de reuso e a
     * validação final.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param mascarador preserva/compara a estrutura de tags entre original e tradução
     * @param detectorIdentica decide quando uma fala idêntica ao original é legítima
     * @param validador acusa resíduo gringo/preâmbulo lançando {@link AlucinacaoDetectadaException}
     */
    public AvaliadorTraducaoCache(
        MascaradorTags mascarador,
        DetectorTraducaoIdenticaService detectorIdentica,
        ValidadorTraducaoService validador,
        VerificadorIdentificadorNumerico verificadorNumerico
    ) {
        this.mascarador = mascarador;
        this.detectorIdentica = detectorIdentica;
        this.validador = validador;
        this.verificadorNumerico = verificadorNumerico;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma tradução já em cache pode ser reusada sem
     * reenviar a fala ao LLM, economizando chamadas ao modelo.
     *
     * <p>INVARIANTES DO DOMÍNIO: rejeita vazio e tags divergentes; tradução idêntica ao
     * original só é reusada quando a lore manda mantê-la; caso contrário exige passar na
     * validação de resíduo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fala que a validação acusa como ainda não
     * traduzida ({@link AlucinacaoDetectadaException}) devolve {@code false}.
     */
    public boolean isCacheReaproveitavel(String original, String traduzido) {
        if (traduzido == null || traduzido.isBlank()) {
            return false;
        }
        if (!mascarador.preservaEstruturaOriginal(original, traduzido)) {
            log.warn("Cache ignorado porque as tags divergem do original: {}", traduzido);
            return false;
        }
        if (normalizarParaComparacao(original).equals(normalizarParaComparacao(traduzido))) {
            return detectorIdentica.deveManterIdentico(original);
        }
        try {
            validador.validarFala(traduzido);
            return true;
        } catch (AlucinacaoDetectadaException e) {
            log.warn("Cache ignorado porque parece conter fala ainda nao traduzida: {}", traduzido);
            return false;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza espaços em branco para comparar original e tradução
     * sem que diferenças triviais de espaçamento escondam uma fala não traduzida.
     *
     * <p>INVARIANTES DO DOMÍNIO: colapsa sequências de espaço em um único e apara as
     * pontas; o texto normalizado é só para comparação, nunca persistido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} vira string vazia.
     */
    private String normalizarParaComparacao(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se o resultado final pode entrar no banco bilíngue como
     * uma tradução reaproveitável ou deve permanecer pendente.
     *
     * <p>INVARIANTES DO DOMÍNIO: fallback idêntico só é aceito para nome, sigla, número ou
     * termo protegido pela lore; vazio e resíduo gringo nunca são contabilizados como
     * tradução concluída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve uma justificativa legível; uma tradução
     * válida devolve {@code null}.
     */
    public String motivoFalhaFinal(String original, String traduzido) {
        if (traduzido == null || traduzido.isBlank()) {
            return "resposta vazia";
        }
        if (!mascarador.preservaEstruturaOriginal(original, traduzido)) {
            return "tags ASS/SSA ou quebras de linha divergentes do original";
        }
        if (detectorIdentica.pareceNaoTraduzida(original, traduzido)) {
            return "modelo devolveu o texto original sem tradução";
        }
        // Invariância numérica: o modelo não tem autoridade para reescrever um identificador
        // da fonte. Roda AQUI porque este portão é o mesmo do LLM e do tradutor de máquina.
        String numeroAlterado = verificadorNumerico.divergencia(original, traduzido);
        if (numeroAlterado != null) {
            return numeroAlterado;
        }
        try {
            validador.validarFala(traduzido);
            return null;
        } catch (AlucinacaoDetectadaException e) {
            return e.getMessage();
        }
    }
}
