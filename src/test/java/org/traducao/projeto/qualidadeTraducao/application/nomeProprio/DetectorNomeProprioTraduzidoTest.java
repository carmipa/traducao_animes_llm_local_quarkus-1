package org.traducao.projeto.qualidadeTraducao.application.nomeProprio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova as três propriedades que fazem este detector valer a pena — acha o
 * nome traduzido, NÃO acusa palavra comum, e sabe dizer quando não conseguiu olhar.
 *
 * <h2>Por que dublê e não o hunspell de verdade</h2>
 * O que está sob teste é a REGRA de decisão, não a qualidade do dicionário. Com dublê, o veredito
 * de cada palavra é escolhido pelo teste, e o caso-controle do falso positivo histórico
 * ({@code Never} classificado como inglês) pode ser montado à mão. O dicionário real é exercitado
 * nos testes do próprio subpacote {@code dicionarioOrtografia}.
 */
@DisplayName("nome próprio: acha o traduzido, poupa o comum, admite quando não olhou")
class DetectorNomeProprioTraduzidoTest {

    /** Dicionário de mentira: devolve exatamente os vereditos que o teste mandar. */
    private static DetectorNomeProprioTraduzido comVereditos(Map<String, VeredictoPalavra> tabela) {
        CorretorOrtograficoLegenda dubl = new CorretorOrtograficoLegenda() {
            @Override
            public Map<String, VeredictoPalavra> classificar(Set<String> palavras) {
                Map<String, VeredictoPalavra> r = new LinkedHashMap<>();
                for (String p : palavras) {
                    VeredictoPalavra v = tabela.get(p);
                    if (v != null) {
                        r.put(p, v);
                    }
                }
                return r;
            }
        };
        return new DetectorNomeProprioTraduzido(dubl);
    }

    /**
     * O DEFEITO QUE O DETECTOR EXISTE PARA ACHAR, e o exemplo é o do próprio Javadoc do
     * {@code ContextoSemLore}: sem lore, nada impede {@code Sky} de virar {@code Céu}.
     */
    @Test
    @DisplayName("CASO DOENTE: nome desconhecido sumiu da tradução — acusa")
    void nomeDesconhecidoQueSumiuEhAcusado() {
        var d = comVereditos(Map.of("Sky", VeredictoPalavra.DESCONHECIDA));
        var v = d.verificarLote(Map.of("Take me to Sky before dawn.", "Leve-me ao Céu antes do amanhecer."));

        assertTrue(v.verificado(), "o dublê respondeu, então tem de estar verificado");
        assertTrue(v.temPerda(), "o nome Sky foi traduzido para Céu e não foi acusado");
        assertEquals(1, v.falasAfetadas());
        assertEquals(1, v.totalPerdidos());
        assertTrue(v.perdidos().values().iterator().next().contains("Sky"));
    }

    /**
     * O CASO-CONTROLE, e é o que separa este detector da heurística descartada: {@code Never} é
     * inglês comum. Sumir da tradução é o comportamento CERTO — traduzir é o trabalho.
     */
    @Test
    @DisplayName("CASO SÃO: palavra inglesa comum capitalizada some e NÃO é acusada")
    void palavraComumNaoEhAcusada() {
        var d = comVereditos(Map.of("Never", VeredictoPalavra.RESIDUO_INGLES));
        var v = d.verificarLote(Map.of("I said Never again.", "Eu disse nunca mais."));

        assertTrue(v.verificado());
        assertFalse(v.temPerda(),
            "acusou palavra inglesa comum — é exatamente o falso positivo de 57,7% que derrubou "
                + "a heurística antiga, e o dicionário existe para não repeti-lo");
    }

    @Test
    @DisplayName("nome preservado na tradução: limpo, e limpo é afirmação positiva")
    void nomePreservadoNaoAcusa() {
        var d = comVereditos(Map.of("Kaine", VeredictoPalavra.DESCONHECIDA));
        var v = d.verificarLote(Map.of("we found Kaine alive.", "encontramos Kaine vivo."));

        assertTrue(v.verificado(), "olhou");
        assertFalse(v.temPerda(), "o nome está lá");
        assertEquals(1, v.candidatasExaminadas(), "e o relatório sabe quantas examinou");
    }

    /**
     * A REGRA DE TRÊS ESTADOS. Sem dicionário, "nenhum nome perdido" é mentira — o correto é dizer
     * que não verificou.
     */
    @Test
    @DisplayName("dicionário mudo devolve NÃO VERIFICADO, nunca limpo")
    void dicionarioMudoNaoAprovaPorOmissao() {
        var d = comVereditos(Map.of());
        var v = d.verificarLote(Map.of("Take me to Sky before dawn.", "Leve-me ao Céu."));

        assertFalse(v.verificado(),
            "dicionário sem resposta produziu resultado 'limpo' — é o falso verde que a regra de "
                + "três estados proíbe");
        assertFalse(v.temPerda(), "sem veredito não se acusa ninguém");
    }

    /** Uma palavra sem veredito invalida o LOTE — verificação parcial não vira aprovação. */
    @Test
    @DisplayName("veredito faltando para UMA palavra derruba o lote inteiro")
    void vereditoParcialNaoAprova() {
        var d = comVereditos(Map.of("Kaine", VeredictoPalavra.DESCONHECIDA));
        Map<String, String> lote = new LinkedHashMap<>();
        lote.put("we found Kaine alive.", "encontramos Kaine vivo.");
        lote.put("beyond the Zephyr gate.", "além do portão do Zéfiro.");

        var v = d.verificarLote(lote);
        assertFalse(v.verificado(),
            "Zephyr ficou sem veredito e mesmo assim o lote foi dado como examinado");
    }

    @Test
    @DisplayName("palavra marcada NAO_VERIFICADO também derruba o lote")
    void naoVerificadoDerrubaOlote() {
        var d = comVereditos(Map.of("Sky", VeredictoPalavra.NAO_VERIFICADO));
        var v = d.verificarLote(Map.of("Take me to Sky before dawn.", "Leve-me ao Céu."));
        assertFalse(v.verificado(), "NAO_VERIFICADO foi tratado como veredito válido");
    }

    /** Fala pendente sai em branco; ausência ali é pendência, não nome traduzido. */
    @Test
    @DisplayName("tradução em branco é ignorada — pendência não vira acusação de nome")
    void pendenteNaoEhAcusada() {
        var d = comVereditos(Map.of("Sky", VeredictoPalavra.DESCONHECIDA));
        Map<String, String> lote = new LinkedHashMap<>();
        lote.put("Take me to Sky before dawn.", "");
        var v = d.verificarLote(lote);

        assertFalse(v.temPerda(), "fala pendente foi contada como nome próprio traduzido");
    }

    @Test
    @DisplayName("caixa diferente NÃO é perda: é outro defeito, e menor")
    void caixaDiferenteNaoEhPerda() {
        var d = comVereditos(Map.of("Kaine", VeredictoPalavra.DESCONHECIDA));
        var v = d.verificarLote(Map.of("we found Kaine alive.", "encontramos KAINE vivo."));
        assertFalse(v.temPerda(), "mudança de caixa contaminaria a medição de nome perdido");
    }

    @Test
    @DisplayName("busca é por palavra INTEIRA: Kaine dentro de Kaines não conta como presente")
    void buscaPorPalavraInteira() {
        var d = comVereditos(Map.of("Sky", VeredictoPalavra.DESCONHECIDA));
        var v = d.verificarLote(Map.of("go to Sky now.", "vá para Skyline agora."));
        assertTrue(v.temPerda(), "Sky foi dado como presente por estar dentro de outra palavra");
    }

    /**
     * O FALSO NEGATIVO QUE EXISTE, fixado como fato em vez de ficar como surpresa.
     *
     * <p>Medido com o hunspell real em 13/08/2026: {@code Sky} é conhecido pelo {@code en_US},
     * {@code Heinz} pelo {@code en_US} e pelo {@code de_DE}, {@code Rose} pelos três. Nenhum deles
     * chega a este detector como {@code DESCONHECIDA}, então nenhum é acusado — nem quando some
     * mesmo da tradução.
     *
     * <p><b>Não conserte "melhorando" o detector.</b> Para o dicionário, {@code Sky} e
     * {@code Never} são o MESMO caso: inglês comum capitalizado. Passar a acusar o primeiro obriga
     * a acusar o segundo, e é assim que se volta aos 323 falso positivo em 560 pendências. A
     * separação exige saber que a obra tem uma personagem chamada Sky — que é o que a lore declara
     * e o {@code sem_lore}, por definição, não tem.
     */
    @Test
    @DisplayName("limite conhecido: nome que TAMBÉM é palavra comum escapa, e é irredutível")
    void oFalsoNegativoQueExisteEstaDeclarado() {
        // Como o hunspell classificaria de fato: Sky é inglês, não DESCONHECIDA.
        var d = comVereditos(Map.of("Sky", VeredictoPalavra.RESIDUO_INGLES));
        var v = d.verificarLote(Map.of("Take me to Sky before dawn.", "Leve-me ao Céu antes do amanhecer."));

        assertTrue(v.verificado(), "olhou");
        assertFalse(v.temPerda(),
            "Se este teste mudar, o detector passou a acusar palavra comum capitalizada — e a "
                + "medição que autoriza essa mudança tem de vir junto, porque é exatamente o "
                + "comportamento que gerou 57,7% de falso positivo antes.");
    }

    @Test
    @DisplayName("bordas: mapa nulo e vazio não quebram")
    void bordas() {
        var d = comVereditos(Map.of());
        assertTrue(d.verificarLote(null).verificado());
        assertTrue(d.verificarLote(Map.of()).verificado());
        assertFalse(d.verificarLote(Map.of()).temPerda());
    }
}
