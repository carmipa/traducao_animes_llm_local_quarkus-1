package org.traducao.projeto.contexto.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a régua da IDENTIDADE CANÔNICA de obra — a derivação que dá a
 * TODAS as lores do catálogo (e não só às duas que declararam apelidos à mão) a capacidade de
 * provar que um arquivo é, ou não é, da obra selecionada no combo. É a generalização da guarda
 * nascida do incidente medido nesta árvore: 15 caches de Gundam 0083 gravados sob
 * {@code guilty_crown}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Identidade DERIVADA de id + nome de exibição; apelidos são COMPLEMENTO.</li>
 *   <li>Casamento por SEQUÊNCIA DE PALAVRAS INTEIRAS, nunca {@code contains} permissivo.</li>
 *   <li>NÚMERO/ANO/SIGLA SÃO IDENTIDADE: os pares mínimos {@code 0083}×{@code 0080}×
 *       {@code Gundam 00}, {@code 86}×{@code 1986} e {@code Z}×{@code ZZ} ficam presos aqui.
 *       Qualquer remoção genérica de "ruído de release" reprova este teste.</li>
 *   <li>Especificidade é ordem determinística sobre casamentos exatos, não similaridade.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Afrouxar o casamento (prefixo, substring, distância) faria uma obra bloquear a tradução de
 * outra; endurecer a normalização (descartar dígitos) apagaria a identidade de obras cujo
 * título É um número.
 */
@DisplayName("identidade canônica de obra: derivação determinística e casamento por palavras inteiras")
class IdentidadeObraTest {

    /**
     * PROPÓSITO DE NEGÓCIO: dublê mínimo de lore, para exercitar a derivação sem depender de
     * nenhuma obra real do catálogo.
     * <p>INVARIANTES DO DOMÍNIO: retornos fixos; sem I/O.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private record LoreFake(String id, String nome, Set<String> apelidos) implements ProvedorContexto {
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return "prompt"; }
        @Override public Set<String> apelidosPasta() { return apelidos; }
    }

    private static IdentidadeObra identidade(String id, String nome, String... apelidos) {
        return IdentidadeObra.de(new LoreFake(id, nome, Set.of(apelidos)));
    }

    // ------------------------------------------------------------------ derivação

    /**
     * PROPÓSITO DE NEGÓCIO: a identidade sai de graça do que TODA lore já é obrigada a
     * declarar. Sem isso, as 59 obras continuariam sem cobertura até alguém escrever apelidos
     * uma a uma — e o incidente permaneceria possível em todas as não escritas.
     */
    @Test
    @DisplayName("identidade é derivada de id e nome de exibição, sem nenhum apelido declarado")
    void derivaDeIdENomeDeExibicaoSemApelidos() {
        IdentidadeObra identidade = identidade("gundam_zz", "Mobile Suit Gundam ZZ");

        assertEquals(Set.of("gundam zz", "mobile suit gundam zz"), identidade.nomesCanonicos());
        assertTrue(identidade.reconhece("[Joseki] Mobile Suit Gundam ZZ COMPLETE (1986)(BD AV1 1080p Opus)"));
        assertTrue(identidade.reconhece("Gundam ZZ"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os apelidos entram como COMPLEMENTO — cobrem a grafia que o id e o
     * rótulo de UI não alcançam (aqui, a pasta real usa a letra {@code Z} e a lore, {@code Zeta}).
     */
    @Test
    @DisplayName("apelidos declarados COMPLEMENTAM a identidade derivada (não a substituem)")
    void apelidosComplementamADerivacao() {
        IdentidadeObra identidade = identidade("gundam_zeta", "Mobile Suit Zeta Gundam", "Z Gundam");

        assertTrue(identidade.nomesCanonicos().containsAll(
            Set.of("gundam zeta", "mobile suit zeta gundam", "z gundam")),
            "o apelido soma-se ao derivado; nada é descartado");
        assertTrue(identidade.reconhece("[Joseki] Mobile Suit Z Gundam COMPLETE (1985)"),
            "só o apelido cobre a pasta real, que grafa Z em vez de Zeta");
        assertTrue(identidade.reconhece("Mobile Suit Zeta Gundam"),
            "e o derivado continua valendo");
    }

    @Test
    @DisplayName("apelido idêntico ao derivado não duplica: a identidade é um conjunto")
    void apelidoIgualAoDerivadoNaoDuplica() {
        IdentidadeObra identidade = identidade("guilty_crown", "Guilty Crown", "Guilty Crown", "GUILTY.CROWN");

        assertEquals(Set.of("guilty crown"), identidade.nomesCanonicos());
    }

    @Test
    @DisplayName("provedor nulo ou nomes em branco degradam para identidade vazia, que nunca reconhece")
    void degradaParaIdentidadeVaziaEmVezDeLancar() {
        IdentidadeObra nula = IdentidadeObra.de(null);
        IdentidadeObra branca = identidade("   ", "  ---  ");

        assertEquals(Set.of(), nula.nomesCanonicos());
        assertEquals(Set.of(), branca.nomesCanonicos());
        assertFalse(nula.reconhece("Qualquer Pasta"));
        assertFalse(branca.reconhece("   "));
        assertEquals(0, branca.especificidadeEm("Qualquer Pasta"));
    }

    // ------------------------------------------------------------------ normalização

    @Test
    @DisplayName("normalização ignora caixa, acento, pontuação e colchetes de fansub")
    void normalizacaoIgnoraCaixaAcentoEPontuacao() {
        IdentidadeObra identidade = identidade("guilty_crown", "Guilty Crown");

        assertTrue(identidade.reconhece("[Anime Time] Guilty Crown + OVA [BD][1080p]"));
        assertTrue(identidade.reconhece("GUILTY_CROWN"));
        assertTrue(identidade.reconhece("guilty.crown"));
        assertTrue(identidade.reconhece("Guílty Crówn"));
    }

    @Test
    @DisplayName("normalização é a mesma para pasta, id e apelido (fonte única)")
    void normalizacaoEhUnica() {
        // O grupo de fansub NÃO é removido: nada é descartado por "parecer ruído de release".
        // O que impede o ruído de identificar a obra é a exigência de palavras inteiras.
        assertEquals("joseki mobile suit gundam 0083 stardust memory",
            IdentidadeObra.normalizar("[Joseki] Mobile.Suit_Gundam-0083: Stardust Memory!"));
        assertEquals("", IdentidadeObra.normalizar(null));
        assertEquals("", IdentidadeObra.normalizar("---"));
    }

    // ------------------------------------------------------------------ palavras inteiras

    /**
     * PROPÓSITO DE NEGÓCIO: reconhecer por substring faria a palavra {@code "Crown"} de outra
     * obra bloquear uma tradução legítima — a guarda passaria a errar no sentido caro.
     */
    @Test
    @DisplayName("casa só como sequência de palavras INTEIRAS, nunca por substring ou prefixo")
    void casaSoComoSequenciaDePalavrasInteiras() {
        IdentidadeObra crown = identidade("guilty_crown", "Guilty Crown");

        assertFalse(crown.reconhece("The Crown Season 1"), "'Crown' sozinho não é Guilty Crown");
        assertFalse(crown.reconhece("Guiltycrown"), "sem separador não há duas palavras");
        assertFalse(crown.reconhece("Guilty"), "prefixo não é identidade");
        assertFalse(crown.reconhece("Crown Guilty"), "a ORDEM das palavras faz parte do nome");
        assertTrue(crown.reconhece("Guilty Crown"));
    }

    // ---------------------------------------------- números, anos e siglas SÃO identidade

    /**
     * PROPÓSITO DE NEGÓCIO: par mínimo que proíbe qualquer remoção genérica de "ruído de
     * release". {@code 0083} e {@code 0080} são obras DIFERENTES cuja única diferença é o
     * dígito final; apagar números por parecerem versão/episódio fundiria as duas.
     */
    @Test
    @DisplayName("par mínimo 0083 × 0080 × Gundam 00: o número É a identidade da obra")
    void numeroDaObraNaoEhRuidoDeRelease() {
        IdentidadeObra stardust = identidade("gundam_0083", "Mobile Suit Gundam 0083: Stardust Memory");
        IdentidadeObra pocket = identidade("gundam_0080", "Mobile Suit Gundam 0080: War in the Pocket");

        assertTrue(stardust.reconhece("[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991)"));
        assertFalse(stardust.reconhece("[Joseki] Mobile Suit Gundam 0080 War in the Pocket COMPLETE (1989)"));
        assertTrue(pocket.reconhece("[Joseki] Mobile Suit Gundam 0080 War in the Pocket COMPLETE (1989)"));
        assertFalse(pocket.reconhece("[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991)"));

        assertFalse(stardust.reconhece("Mobile Suit Gundam 00 Season 2"), "Gundam 00 é outra obra");
        assertFalse(pocket.reconhece("Mobile Suit Gundam 00 Season 2"), "Gundam 00 é outra obra");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: par mínimo REAL desta árvore — a obra cujo título é {@code "86"}
     * contra o ANO {@code (1986)} que aparece na pasta de Gundam ZZ. Se {@code "86"} casasse
     * por substring, toda pasta com ano terminado em 86 seria acusada de ser outra obra.
     */
    @Test
    @DisplayName("par mínimo 86 × (1986): o título numérico casa por palavra inteira e não invade o ano")
    void tituloNumericoNaoCasaDentroDeAno() {
        IdentidadeObra oitentaSeis = identidade("eight_six", "86 (Eighty-Six)", "86");

        assertTrue(oitentaSeis.reconhece("86 Part 1"));
        assertTrue(oitentaSeis.reconhece("86 Part 2"));
        assertFalse(oitentaSeis.reconhece("[Joseki] Mobile Suit Gundam ZZ COMPLETE (1986)(BD AV1 1080p Opus)"),
            "1986 é UMA palavra: o ano nunca pode ser lido como o título 86");
        assertFalse(oitentaSeis.reconhece("Break Blade [BD 1080p x265]"),
            "86 não pode aparecer dentro de x265/1080p");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: par mínimo de SIGLA — {@code Z Gundam} contra {@code Gundam ZZ}.
     * Uma letra só ainda é uma palavra inteira, e a ordem das palavras separa as duas obras.
     */
    @Test
    @DisplayName("par mínimo Z Gundam × Gundam ZZ: sigla de uma letra é palavra inteira e a ordem importa")
    void siglaCurtaNaoConfundeObras() {
        IdentidadeObra zeta = identidade("gundam_zeta", "Mobile Suit Zeta Gundam", "Z Gundam");
        IdentidadeObra zz = identidade("gundam_zz", "Mobile Suit Gundam ZZ");

        assertTrue(zeta.reconhece("[Joseki] Mobile Suit Z Gundam COMPLETE (1985)"));
        assertFalse(zeta.reconhece("[Joseki] Mobile Suit Gundam ZZ COMPLETE (1986)"),
            "em 'Gundam ZZ' não existe a sequência 'Z Gundam'");
        assertTrue(zz.reconhece("[Joseki] Mobile Suit Gundam ZZ COMPLETE (1986)"));
        assertFalse(zz.reconhece("[Joseki] Mobile Suit Z Gundam COMPLETE (1985)"));
    }

    @Test
    @DisplayName("sigla alfanumérica (NT, F91, 08th) sobrevive à normalização")
    void siglaAlfanumericaSobrevive() {
        IdentidadeObra f91 = identidade("gundam_f91", "Mobile Suit Gundam F91");
        IdentidadeObra msTeam = identidade("gundam_08ms", "Mobile Suit Gundam: The 08th MS Team", "08th MS Team");

        assertTrue(f91.reconhece("Mobile Suit Gundam F91 [BD]"));
        assertFalse(f91.reconhece("Mobile Suit Gundam F 91"), "F91 é uma palavra só, não duas");
        assertTrue(msTeam.reconhece("[Joseki] Mobile Suit Gundam The 08th MS Team COMPLETE (1996)"));
        assertTrue(msTeam.reconhece("08th MS Team"));
    }

    // ------------------------------------------------------------------ especificidade

    /**
     * PROPÓSITO DE NEGÓCIO: sem especificidade, a pasta {@code "Macross 7 Encore"} seria
     * reivindicada tanto pela série quanto pelo OVA e a execução seria bloqueada por uma
     * ambiguidade que não existe no mundo real.
     */
    @Test
    @DisplayName("especificidade: mais palavras vence; no empate, o nome mais longo")
    void especificidadeOrdenaEntradaMaisPrecisaAcimaDaMaisGenerica() {
        IdentidadeObra serie = identidade("macross_7", "Macross 7 (Série TV)");
        IdentidadeObra encore = identidade("macross_7_encore", "Macross 7 Encore");

        int pesoSerie = serie.especificidadeEm("Macross 7 Encore");
        int pesoEncore = encore.especificidadeEm("Macross 7 Encore");

        assertTrue(pesoSerie > 0, "a série também reconhece a pasta — é o mesmo franchise");
        assertTrue(pesoEncore > pesoSerie,
            "a entrada mais precisa precisa vencer, ou franchise e temporada empatariam em ambiguidade");
        assertEquals(0, encore.especificidadeEm("Macross 7"),
            "o inverso não vale: 'Macross 7' sozinho não é o Encore");
    }

    @Test
    @DisplayName("especificidade é 0 exatamente quando não reconhece")
    void especificidadeZeroEquivaleANaoReconhecer() {
        IdentidadeObra identidade = identidade("sidonia", "Knights of Sidonia");

        assertEquals(0, identidade.especificidadeEm("Break Blade Movies"));
        assertFalse(identidade.reconhece("Break Blade Movies"));
        assertTrue(identidade.especificidadeEm("Knights of Sidonia [BD]") > 0);
        assertTrue(identidade.reconhece("Knights of Sidonia [BD]"));
        assertEquals(0, identidade.especificidadeEm(null));
        assertEquals(0, identidade.especificidadeEm("   "));
    }

    @Test
    @DisplayName("identidade é imutável: o conjunto devolvido não aceita mutação")
    void identidadeEhImutavel() {
        IdentidadeObra identidade = identidade("danmachi", "DanMachi (Geral)");

        assertThrowsUnsupported(() -> identidade.nomesCanonicos().add("intruso"));
    }

    private static void assertThrowsUnsupported(Runnable acao) {
        try {
            acao.run();
        } catch (UnsupportedOperationException esperado) {
            return;
        }
        throw new AssertionError("a identidade canônica precisa ser imutável");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a derivação NÃO inventa variações — não quebra o título em pedaços
     * nem gera acrônimos. Um catálogo que inventa nomes cria colisões que ninguém escreveu.
     */
    @Test
    @DisplayName("a derivação não fabrica variações do título")
    void derivacaoNaoInventaVariacoes() {
        IdentidadeObra identidade = identidade("gundam_unicorn", "Mobile Suit Gundam Unicorn");

        assertEquals(Set.of("gundam unicorn", "mobile suit gundam unicorn"), identidade.nomesCanonicos());
        assertFalse(identidade.reconhece("Unicorn"), "pedaço solto do título não é identidade");
        assertFalse(identidade.reconhece("MSGU"), "acrônimo inventado não é identidade");
    }

    @Test
    @DisplayName("apelidos nulos são ignorados em vez de explodir no meio do catálogo")
    void apelidosNulosSaoIgnorados() {
        IdentidadeObra identidade = IdentidadeObra.de(new LoreFake("x", "Obra X", null));

        assertEquals(Set.of("x", "obra x"), identidade.nomesCanonicos());
        assertTrue(identidade.reconhece("Obra X"));
    }
}
