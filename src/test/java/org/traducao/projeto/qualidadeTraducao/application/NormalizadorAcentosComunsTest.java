package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link NormalizadorAcentosComuns} — repor acento só
 * nas formas que sem ele NUNCA são palavra válida, sem tocar homógrafos nem sentido.
 *
 * <p>INVARIANTES DO DOMÍNIO: só o dicionário curado; fronteira de palavra; caixa preservada;
 * homógrafos ({@code esta}, {@code e}, {@code as vezes}) intocados.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer correção indevida ou faltante reprova.
 */
class NormalizadorAcentosComunsTest {

    private final NormalizadorAcentosComuns norm = new NormalizadorAcentosComuns();

    @Test
    @DisplayName("repõe acento em formas inequívocas (nao/voce/tambem/ate)")
    void corrigeFormasInequivocas() {
        assertEquals("Não, você também vem até aqui?",
            norm.normalizar("Nao, voce tambem vem ate aqui?"));
    }

    @Test
    @DisplayName("repõe infância (caso do 08th)")
    void corrigeInfancia() {
        assertEquals("Desde a infância, tantos dias.",
            norm.normalizar("Desde a infancia, tantos dias."));
    }

    @Test
    @DisplayName("preserva a caixa do achado (primeira maiúscula e tudo maiúsculo)")
    void preservaCaixa() {
        assertEquals("Ninguém! NÃO!", norm.normalizar("Ninguem! NAO!"));
    }

    @Test
    @DisplayName("distingue voce de voces (mais longa primeiro)")
    void distingueVoceDeVoces() {
        assertEquals("vocês e você", norm.normalizar("voces e voce"));
    }

    @Test
    @DisplayName("NÃO toca homógrafos nem palavras válidas (esta/e/as vezes)")
    void naoTocaHomografos() {
        // 'esta'(this), 'e'(and), 'as vezes'(the times) são ambíguos: ficam intocados.
        String s = "esta e as vezes que lutei";
        assertEquals("esta e as vezes que lutei", norm.normalizar(s));
    }

    @Test
    @DisplayName("NÃO casa fragmento dentro de palavra (ate em Kate/mate)")
    void naoCasaFragmento() {
        assertEquals("Kate comeu mate", norm.normalizar("Kate comeu mate"));
    }

    @Test
    @DisplayName("preserva tags de estilo ao corrigir")
    void preservaTags() {
        assertEquals("{\\i1}Não vou além.", norm.normalizar("{\\i1}Nao vou alem."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as seis formas MEDIDAS na auditoria de 07/08/2026.
     *
     * <p>Auditadas 10.918 falas das duas obras traduzidas na noite de 06/08
     * (Gundam 0083 Stardust Memory e Gundam 08th MS Team): 74 falas saíram sem
     * acento, dominadas por {@code capitao} (35) e {@code entao} (26). As frases
     * abaixo são as reais, não inventadas.
     */
    @Test
    @DisplayName("as seis formas medidas na auditoria das duas obras Gundam")
    void formasMedidasNaAuditoria() {
        assertEquals("sem permissão, Capitão Sinapus",
            norm.normalizar("sem permissão, Capitao Sinapus"));
        assertEquals("Então você pode me contatar.",
            norm.normalizar("Entao você pode me contatar."));
        assertEquals("Os arquivos estão todos prontos.",
            norm.normalizar("Os arquivos estao todos prontos."));
        assertEquals("Sim, você tem razão.", norm.normalizar("Sim, você tem razao."));
        assertEquals("Frente de baixa pressão", norm.normalizar("Frente de baixa pressao"));
        assertEquals("companheiros de esquadrão",
            norm.normalizar("companheiros de esquadrão"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a forma colada numa quebra {@code \N} do ASS é a que
     * mais escapa de qualquer conferência ingênua — o {@code N} é letra, então
     * uma fronteira com {@code \b} conclui que o termo é sufixo de outra palavra
     * e não o enxerga.
     *
     * <p>Foi exatamente assim que a minha primeira medição achou 3 defeitos onde
     * havia 5: os dois {@code estao} estavam colados numa quebra.
     */
    @Test
    @DisplayName("corrige a forma colada na quebra \\N do ASS")
    void corrigeColadoNaQuebra() {
        assertEquals("O Unicorn e o Banshee\\Nestão se puxando!",
            norm.normalizar("O Unicorn e o Banshee\\Nestao se puxando!"));
        assertEquals("A missão falhou.\\NEntão recuem.",
            norm.normalizar("A missao falhou.\\NEntao recuem."));
    }

    /** A família -ção acompanha, com singular e plural. */
    @Test
    @DisplayName("familia -ao/-cao, no singular e no plural")
    void familiaTerminacao() {
        assertEquals("missão / missões", norm.normalizar("missao / missoes"));
        assertEquals("posição / posições", norm.normalizar("posicao / posicoes"));
        assertEquals("informação / informações", norm.normalizar("informacao / informacoes"));
        assertEquals("irmão / irmãos", norm.normalizar("irmao / irmaos"));
        assertEquals("três", norm.normalizar("tres"));
        assertEquals("mãe", norm.normalizar("mae"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE da ampliação. A invariante do mapa é que
     * a forma sem acento NUNCA seja palavra válida — e estas são.
     *
     * <p>{@code sera} ficou de fora de propósito apesar de ter aparecido na
     * auditoria: pode ser nome próprio, e uma ocorrência não paga o risco de
     * estragar um nome. {@code pai}, {@code ideia}, {@code apoio} e
     * {@code tenente} não levam acento nenhum — foram 301 falsos positivos na
     * minha primeira medição, e entrar com eles no mapa corromperia o texto.
     */
    @Test
    @DisplayName("NAO toca palavra que ja esta certa nem nome proprio")
    void naoTocaPalavraCorreta() {
        String s = "O tenente Sera teve a ideia com apoio do pai";
        assertEquals(s, norm.normalizar(s));
        assertEquals("Em breve sera realidade.", norm.normalizar("Em breve sera realidade."));
    }
}
