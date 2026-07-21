package org.traducao.projeto.traducao.contextocena;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: a "régua" determinística do conjunto-ouro (D0) — dado um trecho de
 * português, detecta se ele carrega flexão de gênero MASCULINA, FEMININA, ambas (AMBÍGUO)
 * ou nenhuma (NEUTRO), olhando adjetivos/particípios predicativos marcados. Serve para
 * PROVAR objetivamente, sem LLM e sem verdade-base, que uma tradução como "Estou certa"
 * (dita por um homem) viola o gênero esperado — e que "Tenho certeza" é uma saída neutra
 * aceitável. É o instrumento de medição da futura correção por contexto de cena (linha-base
 * do A/B); NÃO é código de produção nem altera o pipeline.
 *
 * <p>INVARIANTES DO DOMÍNIO: casamento por PALAVRA INTEIRA (fronteira de palavra), sensível
 * só à forma marcada — "certa" casa, "certeza"/"certamente" não; a decisão é puramente
 * lexical sobre a lista fechada de formas conhecidas, sem inferir falante. Marcas dos dois
 * gêneros na mesma fala resultam em {@link GeneroFlexao#AMBIGUO} (nunca uma escolha
 * arbitrária); ausência total de marca resulta em {@link GeneroFlexao#NEUTRO}. A lista é um
 * núcleo v0 focado nos casos reais do 08th e nas formas mais comuns — não pretende ser
 * exaustiva; ampliá-la é evolução posterior, não requisito do D0.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} ou em branco devolve
 * {@link GeneroFlexao#NEUTRO} (nada a medir), nunca lança.
 */
public final class VerificadorPropriedadeGenero {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    // Adjetivos/particípios predicativos na forma FEMININA (núcleo v0 + casos do 08th).
    private static final Pattern MARCA_FEMININA = Pattern.compile(
        "\\b(certa|salva|cansada|pronta|sozinha|obrigada|louca|exausta|preocupada|segura|"
            + "ferida|machucada|morta|viva|perdida|assustada|nervosa|ocupada|animada|surpresa|"
            + "satisfeita|irritada|confusa|ansiosa|fraca|linda|brava|calada|quieta)\\b", FLAGS);

    // Adjetivos/particípios predicativos na forma MASCULINA (par das femininas acima).
    private static final Pattern MARCA_MASCULINA = Pattern.compile(
        "\\b(certo|salvo|cansado|pronto|sozinho|obrigado|louco|exausto|preocupado|seguro|"
            + "ferido|machucado|morto|vivo|perdido|assustado|nervoso|ocupado|animado|surpreso|"
            + "satisfeito|irritado|confuso|ansioso|fraco|lindo|bravo|calado|quieto)\\b", FLAGS);

    private VerificadorPropriedadeGenero() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: classifica a flexão de gênero de um trecho de PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: fem + masc presentes → {@link GeneroFlexao#AMBIGUO};
     * só fem → FEMININO; só masc → MASCULINO; nenhum → NEUTRO. Casamento por palavra inteira.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null}/branco → {@link GeneroFlexao#NEUTRO}.
     */
    public static GeneroFlexao detectar(String texto) {
        if (texto == null || texto.isBlank()) {
            return GeneroFlexao.NEUTRO;
        }
        boolean fem = MARCA_FEMININA.matcher(texto).find();
        boolean masc = MARCA_MASCULINA.matcher(texto).find();
        if (fem && masc) {
            return GeneroFlexao.AMBIGUO;
        }
        if (fem) {
            return GeneroFlexao.FEMININO;
        }
        if (masc) {
            return GeneroFlexao.MASCULINO;
        }
        return GeneroFlexao.NEUTRO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa se um candidato de tradução VIOLA o gênero esperado —
     * ou seja, exibe flexão do gênero OPOSTO ao que a lore/cena determina para o papel.
     * <p>INVARIANTES DO DOMÍNIO: só há violação quando a flexão detectada é o oposto exato
     * do esperado (esperado M × detectado F, ou esperado F × detectado M). NEUTRO e AMBÍGUO
     * NUNCA são violação aqui (neutro é saída segura; ambíguo é caso de revisão, tratado à
     * parte). Esperado precisa ser {@link GeneroFlexao#MASCULINO} ou {@link GeneroFlexao#FEMININO}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: esperado {@code null}/NEUTRO/AMBÍGUO devolve
     * {@code false} (não há gênero definido contra o qual violar); candidato nulo → {@code false}.
     */
    public static boolean violaEsperado(GeneroFlexao esperado, String candidato) {
        if (esperado != GeneroFlexao.MASCULINO && esperado != GeneroFlexao.FEMININO) {
            return false;
        }
        GeneroFlexao detectado = detectar(candidato);
        return (esperado == GeneroFlexao.MASCULINO && detectado == GeneroFlexao.FEMININO)
            || (esperado == GeneroFlexao.FEMININO && detectado == GeneroFlexao.MASCULINO);
    }
}
