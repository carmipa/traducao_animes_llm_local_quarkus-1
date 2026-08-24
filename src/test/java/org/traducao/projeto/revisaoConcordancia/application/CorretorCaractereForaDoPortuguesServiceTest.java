package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que o caractere estranho sai da legenda e que o legítimo fica.
 *
 * <h2>Cada caso aqui é uma fala REAL do acervo, lida antes de virar teste</h2>
 * O inventário de 24/08/2026 varreu 222 arquivos e listou 15 caracteres fora do alfabeto
 * esperado. Os casos POSITIVOS são as falas que a leitura condenou; os NEGATIVOS são as que a
 * leitura ABSOLVEU — {@code "24ª Divisão"}, {@code "Læraðr"}, o letreiro do ZZ desenhado glifo a
 * glifo. Sem os negativos, a próxima "melhoria" apaga o que estava certo.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova mostrando a fala inteira, com o caractere nomeado pelo código Unicode.
 */
class CorretorCaractereForaDoPortuguesServiceTest {

    /** O espaco de largura zero, nomeado — escreve-lo literal aqui seria irrevisavel. */
    private static final String ZWSP = Character.toString(0x200B);
    private static CorretorCaractereForaDoPortuguesService corretor;
    private static boolean dicionarioDePe;

    @BeforeAll
    static void montar() {
        CorretorOrtograficoLegenda dic = new CorretorOrtograficoLegenda();
        corretor = new CorretorCaractereForaDoPortuguesService(dic);
        // Uma consulta de sonda: o verificador nasce com estado indefinido e só se declara depois
        // da primeira pergunta. Perguntar antes devolveria `false` sem nada estar errado — e essa
        // confusão já custou uma correção que "nunca acontecia" neste projeto.
        corretor.corrigir("Uma opçāo qualquer.");
        dicionarioDePe = corretor.disponivel();
    }

    @Test
    @DisplayName("POSITIVO: o espaco de largura zero some — a fala real do Zeta")
    void espacoInvisivelSai() {
        Optional<String> r = corretor.corrigir(
            "Os responsáveis " + ZWSP + ZWSP + "pela morte dela\\Nestão neste navio!");
        assertEquals(Optional.of("Os responsáveis pela morte dela\\Nestão neste navio!"), r,
            "o invisivel ficou: ele quebra comparacao de palavra em toda ferramenta a jusante");
    }

    @Test
    @DisplayName("o invisivel sai mesmo colado na quebra do ASS, sem levar a quebra junto")
    void invisivelColadoNaQuebra() {
        Optional<String> r = corretor.corrigir("Tenente Emma,\\N" + ZWSP + "proteja a frota!");
        assertEquals(Optional.of("Tenente Emma,\\Nproteja a frota!"), r,
            "a quebra \\N tem de voltar byte a byte");
    }

    @Test
    @DisplayName("POSITIVO: a exclamacao invertida sai de fala portuguesa — as 2 falas reais")
    void exclamacaoInvertidaSai() {
        assertEquals(Optional.of("Você precisa de algo? Na minha cabeça não!"),
            corretor.corrigir("Você precisa de algo? ¡Na minha cabeça não!"));
        assertEquals(Optional.of("Está prestes a disparar? Todas as unidades, atacem!"),
            corretor.corrigir("Está prestes a disparar? ¡Todas as unidades, atacem!"));
    }

    /**
     * Se a fala não se provar portuguesa, a pontuação fica. O acervo não tem espanhol hoje, e é
     * justamente por isso que o escudo existe: ele vale para a tradução de amanhã.
     */
    @Test
    @DisplayName("NEGATIVO: fala que nao se prova portuguesa mantem a pontuacao espanhola")
    void espanholDeVerdadeFicaIntacto() {
        assertEquals(Optional.empty(), corretor.corrigir("¡Hola! ¿Cual es tu nombre?"),
            "apagou pontuacao de uma fala que nao e portuguesa");
    }

    @Test
    @DisplayName("POSITIVO: o macron vira til nas falas reais do Zeta, do 86 e do 0083")
    void macronViraTil() {
        Assumptions.assumeTrue(dicionarioDePe, "dicionario fora do ar — NAO VERIFICADO, nao passou");
        assertEquals(Optional.of("Atrairei a atenção de Haman."),
            corretor.corrigir("Atrairei a atençāo de Haman."));
        assertEquals(Optional.of("Nesta situação, você deve se esconder!"),
            corretor.corrigir("Nesta situaçāo, você deve se esconder!"));
        Optional<String> duas = corretor.corrigir(
            "Os Titans estão tentando destruir\\Na instalaçāo de comunicaçāo.");
        assertEquals(Optional.of("Os Titans estão tentando destruir\\Na instalação de comunicação."),
            duas, "duas trocas na mesma fala deslocaram uma a outra");
    }

    /**
     * A trava que separa este corretor da medição errada de 23/08/2026. {@code Ōsaka} é romaji
     * legítimo; {@code Õsaka} não é palavra portuguesa, então o dicionário reprova a proposta e o
     * texto fica. Nenhuma lista de exceção foi escrita para isso — quem decide é o dicionário.
     */
    @Test
    @DisplayName("NEGATIVO: macron de romaji legitimo NAO vira til — quem barra e o dicionario")
    void macronDeRomajiFica() {
        Assumptions.assumeTrue(dicionarioDePe, "dicionario fora do ar — NAO VERIFICADO, nao passou");
        assertEquals(Optional.empty(), corretor.corrigir("Chegamos a Ōsaka pela manhã."),
            "trocou o macron de um nome japones: 'Õsaka' nao existe em portugues");
        assertEquals(Optional.empty(), corretor.corrigir("O tenente gritou: Bakā!"));
    }

    @Test
    @DisplayName("NEGATIVO: ordinal, eth e grau sao CORRETOS e ficam — as falas que a leitura absolveu")
    void oQueEstaCertoFica() {
        assertEquals(Optional.empty(),
            corretor.corrigir("Eu sou o Tenente-Coronel Grethe Wenzel, comandante da 1.028ª Unidade."),
            "apagou o ordinal feminino, que e portugues correto");
        assertEquals(Optional.empty(),
            corretor.corrigir("Esquadrão de Cabos do 16º Distrito da Frente Oriental"));
        assertEquals(Optional.empty(), corretor.corrigir("Quartel General Tiwaz para Quartel General Læraðr."),
            "mexeu no nome nordico do 86 — Læraðr e nome proprio, nao defeito");
        assertEquals(Optional.empty(), corretor.corrigir("O nível de dano no campo n° 7 esta em 87%."),
            "o grau como ordinal foi declarado FORA de escopo: nao pode ser tocado por acidente");
    }

    /**
     * O letreiro do ZZ ep29: oito falas, cada uma um {@code ´} sozinho com {@code \\pos} de
     * coordenada fracionária. É tipografia desenhada glifo a glifo, e apagar quebraria o desenho.
     */
    @Test
    @DisplayName("NEGATIVO: o letreiro desenhado glifo a glifo do ZZ fica intacto")
    void letreiroTipograficoFicaIntacto() {
        assertEquals(Optional.empty(), corretor.corrigir("{\\pos(1093.2,1027.2)}´"));
        assertEquals(Optional.empty(), corretor.corrigir("{\\pos(821.2,1028.8)\\i1}´"));
    }

    @Test
    @DisplayName("NEGATIVO: o vietnamita fica — precisa de traducao, nao de troca de caractere")
    void vietnamitaFicaParaORelatorio() {
        assertEquals(Optional.empty(), corretor.corrigir(
            "{\\b1\\fscx208\\fscy208}Seis mươi, setenta, Conseguimos\\Numa participação!"),
            "inventou palavra em cima de idioma vazado — isso e relatorio, nao conserto");
    }

    /**
     * Nome de FONTE é conteúdo de tag. Se um nome de fonte trouxesse macron, corrigir ortografia
     * ali dentro trocaria a tipografia do arquivo por uma fonte que não existe.
     */
    @Test
    @DisplayName("NEGATIVO: macron dentro de tag e nome de fonte, e nao se corrige tipografia")
    void macronDentroDeTagNaoEtocado() {
        Assumptions.assumeTrue(dicionarioDePe, "dicionario fora do ar — NAO VERIFICADO, nao passou");
        String comFonte = "{\\fnAtençāo Sans}Uma fala limpa.";
        assertEquals(Optional.empty(), corretor.corrigir(comFonte),
            "corrigiu o nome da fonte: a tipografia do arquivo quebraria");
    }

    @Test
    @DisplayName("os tres defeitos na MESMA fala saem juntos")
    void tresDefeitosDeUmaVez() {
        Assumptions.assumeTrue(dicionarioDePe, "dicionario fora do ar — NAO VERIFICADO, nao passou");
        Optional<String> r = corretor.corrigir(
            "Nesta situaçāo," + ZWSP + " ¡você deve se esconder!");
        assertTrue(r.isPresent(), "nao corrigiu nada numa fala com tres defeitos");
        assertEquals("Nesta situação, você deve se esconder!", r.get());
    }

    @Test
    @DisplayName("entrada degenerada nao lanca e fala limpa nao muda")
    void degenerada() {
        assertEquals(Optional.empty(), corretor.corrigir(null));
        assertEquals(Optional.empty(), corretor.corrigir(""));
        assertEquals(Optional.empty(), corretor.corrigir("   "));
        assertEquals(Optional.empty(), corretor.corrigir("Uma fala perfeitamente correta."));
        assertFalse(corretor.corrigir("Bom dia, Kamille!").isPresent());
    }
}
