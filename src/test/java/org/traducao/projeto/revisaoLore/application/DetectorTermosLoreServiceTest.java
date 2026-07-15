package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorTermosLoreServiceTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    /**
     * PROPÓSITO DE NEGÓCIO: preserva tecnologias oficiais declaradas pela lore.
     * <p>INVARIANTES DO DOMÍNIO: psycho-frame não é resíduo inglês nesta obra.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
     */
    @Test
    void naoSinalizaTermoInglesPreservadoPelaLore() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "The psycho-frame is responding.",
            "O psycho-frame está respondendo.",
            "Termos oficiais: psycho-frame, Newtype."
        );

        assertFalse(resultado.suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita títulos e conceitos oficialmente localizados.
     * <p>INVARIANTES DO DOMÍNIO: Terra, Século Universal e Princesa são PT-BR.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
     */
    @Test
    void aceitaEquivalenciasLocalizadasDeTermosCapitalizados() {
        assertFalse(detector.auditar("Earth is in the Universal Century.",
            "A Terra está no Século Universal.").suspeito());
        ResultadoDeteccaoLore titulos = detector.auditar(
            "Princess Mineva issued the Laplace Declaration.",
            "A princesa Mineva emitiu a Declaração de Laplace.");
        assertFalse(titulos.suspeito(), () -> titulos.motivos().toString());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: evita enviar cognatos PT-BR legítimos ao LLM.
     * <p>INVARIANTES DO DOMÍNIO: cosmos e crime existem nos dois idiomas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
     */
    @Test
    void naoSinalizaCognatosValidosEmPortugues() {
        assertFalse(detector.auditar("Fly through the cosmos.", "Voe pelo cosmos.").suspeito());
        assertFalse(detector.auditar("It is a crime.", "Isso é um crime.").suspeito());
    }

    @Test
    void detectaNarrativeTraduzidoLiteralmenteEmNomeCanonico() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "The RX-9 Narrative Gundam is launching.",
            "O RX-9 Gundam Narrativo vai decolar."
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("narrativo")));
    }

    @Test
    void detectaNomeCompostoPreservadoApenasEmParte() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Mobile Suit Gundam Narrative is connected to Unicorn.",
            "Mobile Suit Gundam Narrativo esta ligado ao Unicorn."
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("preservado apenas em parte")));
    }

    @Test
    void naoSinalizaNomeCanonicoPreservado() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "The RX-9 Narrative Gundam is launching.",
            "O RX-9 Narrative Gundam vai decolar."
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void naoConsideraTagsComoNomesPropriosDivergentes() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "[[TAG0]]Beans never seem to go down smoothly for me.[[TAG1]]",
            "Os feijões nunca parecem descer suavemente para mim."
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void naoSinalizaPalavrasComunsCapitalizadasNoInicioDaFala() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Passing defense line one! Beginning countdown!",
            "Passando pela primeira linha de defesa! Iniciando a contagem regressiva!"
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void naoDetectaPalavraInglesaDentroDePalavraPortuguesa() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Just a vaporization bomb. What else would it be?",
            "Apenas uma bomba de vaporização. O que mais seria?"
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void aceitaVariantesPtBrDeEarthFederation() {
        ResultadoDeteccaoLore federacaoTerrestre = detector.auditar(
            "Two Earth Federation units are approaching.",
            "Duas unidades da Federação Terrestre estão se aproximando."
        );
        ResultadoDeteccaoLore federacaoDaTerra = detector.auditar(
            "Two Earth Federation units are approaching.",
            "Duas unidades da Federação da Terra estão se aproximando."
        );

        assertFalse(federacaoTerrestre.suspeito());
        assertFalse(federacaoDaTerra.suspeito());
    }

    @Test
    void sinalizaFederationQuandoFicaEmInglesNaTraducao() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Let the Federation have Odessa and the Earth!",
            "Deixe a Federation ficar com Odessa e a Terra!"
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("traduzivel permaneceu em ingles")));
    }

    @Test
    void sinalizaTermosRelevantesDeLoreNoInicioDaFalaQuandoDivergentes() {
        ResultadoDeteccaoLore gundamOmitido = detector.auditar(
            "Gundam is approaching!",
            "O robo gigante está se aproximando!"
        );
        ResultadoDeteccaoLore zeonOmitido = detector.auditar(
            "Zeon forces are attacking.",
            "As forças inimigas estão atacando."
        );

        assertTrue(gundamOmitido.suspeito());
        assertTrue(gundamOmitido.motivos().stream().anyMatch(m -> m.contains("inconsistente")));
        assertTrue(zeonOmitido.suspeito());
        assertTrue(zeonOmitido.motivos().stream().anyMatch(m -> m.contains("inconsistente")));
    }

    @Test
    void naoSinalizaQuebraDeFrasesComPalavrasComunsCapitalizadas() {
        ResultadoDeteccaoLore res1 = detector.auditar(
            "Yes. Someone very important to us.",
            "Sim. Alguém muito importante para nós."
        );
        ResultadoDeteccaoLore res2 = detector.auditar(
            "She shall live on inside of us. Forever.",
            "Ela continuará viva dentro de nós. Para sempre."
        );
        ResultadoDeteccaoLore res3 = detector.auditar(
            "How can you all look so calm?!",
            "Como todos vocês podem parecer tão calmos!"
        );
        ResultadoDeteccaoLore res4 = detector.auditar(
            "We're sad, but...",
            "Estamos tristes, mas..."
        );

        assertFalse(res1.suspeito());
        assertFalse(res2.suspeito());
        assertFalse(res3.suspeito());
        assertFalse(res4.suspeito());
    }

    @Test
    void naoTrataNomeAposAbreviacaoComoInicioDeFrase() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "We were at Dr. Flanagan's institute.",
            "Estávamos no instituto do doutor."
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Flanagan")));
    }

    @Test
    void sinalizaShinTraduzidoComoCanelaMesmoNoInicioDaFala() {
        ResultadoDeteccaoLore vocativo = detector.auditar(
            "Shin!",
            "Canela!"
        );
        ResultadoDeteccaoLore nomeCompleto = detector.auditar(
            "Shin! Shinei Nouzen!",
            "Canela! Shinei Nouzen!"
        );

        assertTrue(vocativo.suspeito());
        assertTrue(vocativo.motivos().stream().anyMatch(m -> m.contains("canela") || m.contains("Shin")));
        assertTrue(nomeCompleto.suspeito());
        assertTrue(nomeCompleto.motivos().stream().anyMatch(m -> m.contains("canela") || m.contains("Shin")));
    }

    private static final String LORE_86 =
        "- Obra: 86. Personagens: Shinei \"Shin\" Nouzen, Lena. Faccoes: Legion, San Magnolia. "
            + "Termos: Juggernaut, Handler, Processor, dud rounds.";
    private static final String LORE_GUNDAM_SEED =
        "- Obra: Gundam SEED. Mobile suits: Freedom, Justice, Destiny. Faccoes: ZAFT, Orb.";

    @Test
    void comLoreDaObraNaoAplicaRegraLiteralDeOutraFranquia() {
        // "freedom"→"liberdade" é proteção do Gundam SEED; no 86, "liberdade"
        // é a tradução correta e não pode ser sinalizada.
        ResultadoDeteccaoLore no86 = detector.auditar(
            "We fight for freedom!",
            "Nós lutamos pela liberdade!",
            LORE_86
        );
        ResultadoDeteccaoLore noSeed = detector.auditar(
            "The Freedom is launching!",
            "A Liberdade está decolando!",
            LORE_GUNDAM_SEED
        );

        assertFalse(no86.suspeito());
        assertTrue(noSeed.suspeito());
        assertTrue(noSeed.motivos().stream().anyMatch(m -> m.contains("liberdade")));
    }

    @Test
    void comLoreDaObraMantemProtecaoDosTermosDaPropriaObra() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Shin!",
            "Canela!",
            LORE_86
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("canela") || m.contains("Shin")));
    }

    @Test
    void comLoreDaObraNaoSinalizaTermoSolteiroDeOutraFranquia() {
        // "Zeon" no início da fala só é lore em Gundam; com o lore do 86 ativo
        // a palavra capitalizada vira início de frase comum.
        ResultadoDeteccaoLore no86 = detector.auditar(
            "Zeon forces are attacking.",
            "As forças inimigas estão atacando.",
            LORE_86
        );

        assertFalse(no86.suspeito());
    }

    @Test
    void semLoreInformadoMantemComportamentoGlobal() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "We fight for freedom!",
            "Nós lutamos pela liberdade!"
        );

        assertTrue(resultado.suspeito());
    }

    @Test
    void sinalizaDudRoundsTraduzidoComoRodadasAleatorias() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Those are dud rounds.",
            "Essas são rodadas aleatórias."
        );
        ResultadoDeteccaoLore cache = detector.auditar(
            "Those dud rounds landed around there.",
            "Aquelas rodadas fracassadas caíram ali perto."
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("rodadas aleat")));
        assertTrue(cache.suspeito());
        assertTrue(cache.motivos().stream().anyMatch(m -> m.contains("rodadas fracassadas")));
    }
}
