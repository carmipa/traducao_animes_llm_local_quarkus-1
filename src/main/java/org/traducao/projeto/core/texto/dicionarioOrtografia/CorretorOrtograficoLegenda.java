package org.traducao.projeto.core.texto.dicionarioOrtografia;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROPÓSITO DE NEGÓCIO: o ponto ÚNICO por onde a legenda passa para ter acento reposto pelo
 * dicionário do sistema. É o que fecha o eixo em que a aya perde para o mistral — medido em
 * 13/08/2026 no Zeta: 119 falas com acento faltando contra 39 do mistral.
 *
 * <h2>Só uma coisa é corrigida, e de propósito</h2>
 * Das seis classificações possíveis, apenas {@link VeredictoPalavra#ACENTO_FALTANDO} autoriza
 * escrita. Resíduo de inglês, termo alemão de lore, romaji e palavra desconhecida são REPORTADOS
 * e não tocados — porque "não é português" não significa "está errado" numa legenda de anime, onde
 * {@code cockpit}, {@code Nordlicht} e {@code sasageyo} são todos legítimos.
 *
 * <p>E mesmo o acento só entra pela regra do {@link CorretorAcentoPorDicionario}: a sugestão tem
 * de ser a MESMA palavra acentuada. {@code colonia} vira {@code colônia}, nunca {@code colonial}.
 *
 * <h2>Ordem no pipeline</h2>
 * Roda DEPOIS do {@code NormalizadorAcentosComuns}, que é determinístico e instantâneo: o que a
 * lista e a regra de terminação já resolvem não chega a custar uma consulta. Sobra para o
 * dicionário o que nenhuma regra alcança — {@code fatidico}, {@code minimo}, {@code aereo},
 * {@code psicicas}, medidos no Unicorn.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Dicionário indisponível devolve o texto BYTE A BYTE igual, e o contador de "não
 *       verificado" sobe — nunca se apresenta como legenda conferida.</li>
 *   <li>Uma consulta por FALA, não por palavra. Consultas repetidas da mesma palavra num
 *       episódio são absorvidas pelo hunspell, que já responde em lote.</li>
 *   <li>Não conhece legenda, cache nem LLM: recebe texto e devolve texto.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança. Qualquer problema devolve o original.
 */
@ApplicationScoped
public class CorretorOrtograficoLegenda {

    private static final Logger log = LoggerFactory.getLogger(CorretorOrtograficoLegenda.class);

    private final ClassificadorQuatroIdiomas classificador;
    private final DicionarioOrtograficoPort portugues;
    private final AtomicInteger corrigidas = new AtomicInteger();
    private final AtomicInteger naoVerificadas = new AtomicInteger();

    public CorretorOrtograficoLegenda() {
        this.portugues = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        this.classificador = new ClassificadorQuatroIdiomas(
            portugues,
            new HunspellDicionarioAdapter("hunspell", "en_US"),
            new HunspellDicionarioAdapter("hunspell", "de_DE"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com os acentos que faltavam, sem tocar em mais nada.
     *
     * <p>INVARIANTES DO DOMÍNIO: só palavras classificadas como {@code ACENTO_FALTANDO} são
     * alteradas; tags, quebras {@code \N} e o resto do texto voltam byte a byte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o texto recebido.
     */
    public String corrigir(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        try {
            Set<String> candidatas = CorretorAcentoPorDicionario.candidatas(texto);
            if (candidatas.isEmpty()) {
                return texto;
            }
            Map<String, Set<String>> sugestoes = portugues.sugestoes(candidatas);
            if (!portugues.disponivel()) {
                naoVerificadas.incrementAndGet();
                return texto;
            }
            Map<String, String> acentos = CorretorAcentoPorDicionario.apenasAcentuacoes(sugestoes);
            if (acentos.isEmpty()) {
                return texto;
            }
            corrigidas.addAndGet(acentos.size());
            return CorretorAcentoPorDicionario.aplicar(texto, acentos);
        } catch (RuntimeException e) {
            log.debug("Correção ortográfica ignorada nesta fala: {}", e.getMessage());
            return texto;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: classifica sem corrigir — para o relatório dizer o que é resíduo de
     * inglês, termo de lore e romaji, em vez de chamar tudo de erro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio.
     */
    public Map<String, VeredictoPalavra> classificar(Set<String> palavras) {
        try {
            return classificador.classificar(palavras);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    /** Quantas palavras tiveram acento reposto nesta execução. */
    public int totalCorrigidas() {
        return corrigidas.get();
    }

    /** Quantas falas passaram SEM verificação — o estado 2, que não é aprovação. */
    public int totalNaoVerificadas() {
        return naoVerificadas.get();
    }

    /** Se o verificador respondeu ao menos uma vez. */
    public boolean disponivel() {
        return portugues.disponivel();
    }
}
