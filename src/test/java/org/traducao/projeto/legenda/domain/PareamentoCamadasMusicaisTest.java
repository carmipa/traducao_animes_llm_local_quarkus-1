package org.traducao.projeto.legenda.domain;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Camada;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Janela;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Original;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Par;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conjunto-ouro do pareamento das duas camadas de karaokê, calibrado com a estrutura real medida no
 * Guilty Crown ep4: {@code OP Roma} 10/10 e {@code ED Roma L1} 11/11 têm parceira no mesmo tempo,
 * enquanto o diálogo {@code Default} tem 0/253.
 */
class PareamentoCamadasMusicaisTest {

    private static Camada romaji(int indice, long inicio, long fim) {
        return new Camada(indice, "OP Roma", "Sonna sekai ni nokosareta boku wa", inicio, fim, true, 100);
    }

    private static Camada traducao(int indice, long inicio, long fim) {
        return new Camada(indice, "OP", "In this world I was left behind", inicio, fim, false, 20);
    }

    private static Camada dialogo(int indice, long inicio, long fim) {
        return new Camada(indice, "Default", "What are you doing here?!", inicio, fim, false, 25);
    }

    @Test
    void extraiJanelaDoPrefixoDeEventoAss() {
        Optional<Janela> janela = PareamentoCamadasMusicais.janelaDoPrefixo(
            "Dialogue: 0,0:01:23.45,0:01:26.78,ED_S2_roma,,0,0,0,,");

        assertTrue(janela.isPresent());
        assertEquals(8345, janela.get().inicioCs());
        assertEquals(8678, janela.get().fimCs());
    }

    @Test
    void aceitaDialetoSsaComMarkedEIgnoraOrdemDeColunas() {
        Optional<Janela> janela = PareamentoCamadasMusicais.janelaDoPrefixo(
            "Dialogue: Marked=0,0:00:05.00,0:00:07.50,OP_S2_roma,,0000,0000,0000,,");

        assertTrue(janela.isPresent());
        assertEquals(500, janela.get().inicioCs());
        assertEquals(750, janela.get().fimCs());
    }

    @Test
    void recusaPrefixoSemDoisTemposOuComFimAntesDoInicio() {
        assertTrue(PareamentoCamadasMusicais.janelaDoPrefixo(null).isEmpty());
        assertTrue(PareamentoCamadasMusicais.janelaDoPrefixo("Dialogue: 0,0:00:05.00,").isEmpty());
        assertTrue(PareamentoCamadasMusicais.janelaDoPrefixo("Comment: 0,sem tempo aqui,").isEmpty());
        assertTrue(PareamentoCamadasMusicais.janelaDoPrefixo(
            "Dialogue: 0,0:00:09.00,0:00:07.00,OP,,0,0,0,,").isEmpty());
    }

    @Test
    void sobreposicaoUsaAMenorJanelaEntaoLinhaCurtaContidaDaUm() {
        // Padrão real do karaokê: a camada traduzida costuma ter janela um pouco maior.
        assertEquals(1.0, PareamentoCamadasMusicais.sobreposicao(romaji(1, 500, 700), traducao(2, 480, 760)), 0.001);
        assertEquals(0.0, PareamentoCamadasMusicais.sobreposicao(romaji(1, 500, 700), traducao(2, 700, 900)), 0.001);
        assertEquals(0.0, PareamentoCamadasMusicais.sobreposicao(romaji(1, 500, 500), traducao(2, 500, 700)), 0.001);
        assertEquals(0.0, PareamentoCamadasMusicais.sobreposicao(null, traducao(2, 500, 700)), 0.001);
    }

    @Test
    void pareiaRomajiComTraducaoNoMesmoTempoEElegeORomajiPeloEstilo() {
        List<Par> pares = PareamentoCamadasMusicais.parear(List.of(
            traducao(2, 480, 760),
            romaji(1, 500, 700)));

        assertEquals(1, pares.size());
        Par par = pares.getFirst();
        assertEquals(Original.SEGUNDA, par.original(), "quem começa antes é a primeira: a tradução");
        assertEquals(1, par.camadaPreservar().orElseThrow().indice());
        assertEquals(2, par.camadaTraduzir().orElseThrow().indice());
    }

    @Test
    void dialogoNaoFormaParPorqueNaoTemCamadaSimultanea() {
        // O controle do conjunto-ouro: 0 de 253 falas do Default têm parceira.
        List<Par> pares = PareamentoCamadasMusicais.parear(List.of(
            dialogo(1, 100, 300),
            dialogo(2, 320, 500),
            dialogo(3, 520, 700)));

        assertTrue(pares.isEmpty());
    }

    @Test
    void naoPareiaQuandoSobreposicaoFicaAbaixoDoMinimo() {
        // 50 de 200 centésimos = 25% da menor janela: tangência, não é o mesmo verso.
        List<Par> pares = PareamentoCamadasMusicais.parear(List.of(
            romaji(1, 500, 700),
            traducao(2, 650, 850)));

        assertTrue(pares.isEmpty());
    }

    @Test
    void cadaCamadaEntraEmNoMaximoUmPar() {
        // Três linhas simultâneas (romaji + tradução + uma terceira): a terceira sobra.
        Camada terceira = new Camada(3, "OP2", "otra capa", 500, 700, false, 20);

        List<Par> pares = PareamentoCamadasMusicais.parear(List.of(
            romaji(1, 500, 700),
            traducao(2, 500, 700),
            terceira));

        assertEquals(1, pares.size());
        List<Integer> emPares = List.of(pares.getFirst().primeira().indice(), pares.getFirst().segunda().indice());
        assertFalse(emPares.contains(3), "a camada sem par não pode ser inventada dentro de um par");
    }

    @Test
    void quandoNenhumEstiloDeclaraRomajiDecidePeloConteudoComMargem() {
        Camada bilingueMaisJapones = new Camada(1, "Song", "kimi no tame tokka icchatte", 500, 700, false, 80);
        Camada bilingueMenos = new Camada(2, "Song", "But no matter how bad a fight", 500, 700, false, 30);

        Par par = PareamentoCamadasMusicais.parear(List.of(bilingueMaisJapones, bilingueMenos)).getFirst();

        assertEquals(Original.PRIMEIRA, par.original());
        assertSame(bilingueMaisJapones, par.camadaPreservar().orElseThrow());
    }

    @Test
    void empateDeConteudoDeclaraIndecisoEmVezDeChutar() {
        // Caso Sawano: letra deliberadamente bilíngue dos dois lados. Chutar aqui ou deixa o
        // karaokê sem tradução ou destrói o romaji — o viés é não decidir.
        Camada a = new Camada(1, "Song ENG", "Nano nano wa the sky", 500, 700, false, 55);
        Camada b = new Camada(2, "Song ENG", "kimi wa the light", 500, 700, false, 50);

        Par par = PareamentoCamadasMusicais.parear(List.of(a, b)).getFirst();

        assertEquals(Original.INDECISO, par.original());
        assertTrue(par.camadaPreservar().isEmpty());
        assertTrue(par.camadaTraduzir().isEmpty());
    }

    @Test
    void estiloSoDecideQuandoApenasUmLadoDeclaraRomaji() {
        Camada ambosMarcados1 = new Camada(1, "OP Roma", "aaa", 500, 700, true, 90);
        Camada ambosMarcados2 = new Camada(2, "ED Roma", "bbb", 500, 700, true, 20);

        assertEquals(Original.PRIMEIRA,
            PareamentoCamadasMusicais.decidirOriginal(ambosMarcados1, ambosMarcados2),
            "com os dois marcados o desempate cai para o conteúdo");
        assertEquals(Original.INDECISO, PareamentoCamadasMusicais.decidirOriginal(null, ambosMarcados2));
    }

    @Test
    void listaVaziaOuUnicaCamadaNaoProduzPar() {
        assertTrue(PareamentoCamadasMusicais.parear(null).isEmpty());
        assertTrue(PareamentoCamadasMusicais.parear(List.of()).isEmpty());
        assertTrue(PareamentoCamadasMusicais.parear(List.of(romaji(1, 500, 700))).isEmpty());
    }
}
