package org.traducao.projeto.raspagemCorrecao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.contexto.gundam.ContextoGundamNT;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a contingência online preserva terminologia
 * oficial declarada na lore em vez de produzir traduções literais destrutivas.
 *
 * <p>INVARIANTES DO DOMÍNIO: termos explícitos e regra “Manter sempre” são
 * protegidos; marcador perdido invalida a resposta.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer termo alterado ou marcador aceito
 * indevidamente reprova o teste.
 */
class ProtetorTermosLoreServiceTest {

    private final ProtetorTermosLoreService service = new ProtetorTermosLoreService();

    /**
     * PROPÓSITO DE NEGÓCIO: valida a preservação de nomes oficiais declarados
     * textualmente na lore.
     * <p>INVARIANTES DO DOMÍNIO: grafia e caixa do original são restauradas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: diferença textual reprova o teste.
     */
    @Test
    void preservaTermosDaRegraManterSempre() {
        String lore = "- Manter sempre em inglês ou forma oficial: Sleeves, Psycho-Frame, Phenex.";
        var protegido = service.mascarar(
            "These guys are Sleeves and use a psycho-frame.", lore, Set.of());

        assertFalse(protegido.textoMascarado().contains("Sleeves"));
        String resposta = protegido.textoMascarado()
            .replace("These guys are", "Esses caras são")
            .replace("and use a", "e usam um");
        assertEquals(
            "Esses caras são Sleeves e usam um psycho-frame.",
            service.restaurar(resposta, protegido));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede persistir tradução que perdeu um termo oficial.
     * <p>INVARIANTES DO DOMÍNIO: todo marcador criado precisa voltar intacto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: restauração devolve {@code null}.
     */
    @Test
    void rejeitaRespostaQuePerdeMarcadorDeLore() {
        var protegido = service.mascarar("Protect Phenex!", "", Set.of("Phenex"));
        assertNull(service.restaurar("Proteja a Fênix!", protegido));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reproduz a regressão real em que o Google transformou
     * Jona em Jonas e Narrative em Narrativa apesar da lore de Gundam NT.
     * <p>INVARIANTES DO DOMÍNIO: nome curto de personagem e nome oficial declarado
     * fora da linha “Manter sempre” também precisam ser mascarados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer termo visível no texto enviado
     * ao Google reprova o teste.
     */
    @Test
    void protegePersonagemENomeOficialDeclaradosEmOutrasLinhasDaLore() {
        String lore = """
            - Jona Basta (homem): piloto do Narrative Gundam.
            - Brick Teclato (homem): assistente de Michele.
            - "Narrative" e "Narrative Gundam" são nomes oficiais; nunca traduzir.
            """;

        var protegido = service.mascarar("Jona! Narrative! Tell Brick.", lore, Set.of());

        assertFalse(protegido.textoMascarado().contains("Jona"));
        assertFalse(protegido.textoMascarado().contains("Narrative"));
        assertFalse(protegido.textoMascarado().contains("Brick"));
        assertEquals("Jona! Narrative! Avise Brick.",
            service.restaurar(protegido.textoMascarado().replace("Tell", "Avise"), protegido));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia na revisão final propostas que traduzam ou
     * deformem nomes oficiais mesmo quando não passaram pelo mascaramento.
     * <p>INVARIANTES DO DOMÍNIO: Narrative, Jona, Phenex e Zoltan mantêm grafia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo não detectado reprova o teste.
     */
    @Test
    void detectaTermosCanonicosAlteradosNaPropostaFinal() {
        String lore = """
            - Jona Basta (homem): piloto.
            - Zoltan Akkanen (homem): piloto.
            - Manter sempre em inglês ou forma oficial: Phenex.
            - "Narrative" e "Narrative Gundam" são nomes oficiais.
            """;

        assertEquals(List.of("Narrative"), service.termosCanonicosAlterados(
            "Narrative!", "Narrativa!", lore, Set.of()));
        assertEquals(List.of("Jona"), service.termosCanonicosAlterados(
            "Jona!", "Joana!", lore, Set.of()));
        assertTrue(service.termosCanonicosAlterados(
            "Phenex and Zoltan", "Phenex e Zoltan", lore, Set.of()).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece vocativo canônico como conteúdo válido,
     * evitando retraduzir nomes que devem permanecer idênticos ao inglês.
     * <p>INVARIANTES DO DOMÍNIO: pontuação e tags ASS são permitidas; palavras
     * adicionais obrigam auditoria normal.
     * <p>COMPORTAMENTO EM CASO DE FALHA: classificação invertida reprova o teste.
     */
    @Test
    void reconheceFalaFormadaSomentePorNomeCanonico() {
        String lore = "- Jona Basta (homem): piloto.";
        assertTrue(service.contemSomenteTermosCanonicos("{\\i1}Jona!{\\i0}", lore, Set.of()));
        assertFalse(service.contemSomenteTermosCanonicos("Jona, pare!", lore, Set.of()));
        assertFalse(service.contemSomenteTermosCanonicos("Fransson!", lore, Set.of()));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que designações oficiais sem tradução de
     * Gundam NT não viram pendência artificial na revisão final.
     * <p>INVARIANTES DO DOMÍNIO: Banchi 18, Metis e Fransson precisam constar da
     * lore ativa e nenhuma palavra conversacional comum é aceita por esta regra.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência no glossário reprova o teste.
     */
    @Test
    void reconheceIdentificadoresCanonicosPendentesDeGundamNt() {
        ContextoGundamNT contexto = new ContextoGundamNT();
        String lore = contexto.obterPromptSistema();

        assertTrue(service.contemSomenteTermosCanonicos(
            "Banchi 18, Metis.", lore, contexto.termosProtegidos()));
        assertTrue(service.contemSomenteTermosCanonicos(
            "Fransson!", lore, contexto.termosProtegidos()));
    }
}
