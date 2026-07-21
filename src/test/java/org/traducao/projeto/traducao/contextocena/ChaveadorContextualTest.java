package org.traducao.projeto.traducao.contextocena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.application.contextocena.ChaveadorContextual;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: subfase 6 (parte pura) — prova que a assinatura contextual é
 * determinística e SÓ-FONTE, e que distingue a MESMA fala em cenas diferentes (o que hoje
 * colapsa). É a base da chave de cache contextual, sem qualquer dependência de tradução ou
 * palpite de gênero.
 *
 * <p>INVARIANTES DO DOMÍNIO: mesmas entradas ⇒ mesma assinatura; vizinhas/índice/original/
 * estilo/política diferentes ⇒ assinaturas diferentes; nulos não lançam; 64 hex.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer colisão indevida ou instabilidade reprova.
 */
class ChaveadorContextualTest {

    private final ChaveadorContextual chaveador = new ChaveadorContextual();

    @Test
    @DisplayName("assinatura: deterministica e com 64 hex (SHA-256)")
    void deterministicaE64Hex() {
        String a = chaveador.assinatura(10, "Thank you.", "Default", List.of("Norris!"), "v1");
        String b = chaveador.assinatura(10, "Thank you.", "Default", List.of("Norris!"), "v1");
        assertEquals(a, b, "mesmas entradas devem gerar a mesma assinatura");
        assertEquals(64, a.length(), "SHA-256 hex tem 64 caracteres");
    }

    @Test
    @DisplayName("assinatura: MESMA fala em cenas diferentes gera chaves diferentes (nao colapsa)")
    void mesmaFalaCenasDiferentes() {
        // "Thank you." dito por personagens diferentes, com vizinhas diferentes:
        String cenaAina = chaveador.assinatura(10, "Thank you.", "Default", List.of("Norris, protect me!"), "v1");
        String cenaShiro = chaveador.assinatura(200, "Thank you.", "Default", List.of("Karen, cover me!"), "v1");
        assertNotEquals(cenaAina, cenaShiro,
            "a mesma fala em cenas distintas NAO pode colapsar na mesma chave");
    }

    @Test
    @DisplayName("assinatura: muda com indice, estilo, original, vizinhas e versao da politica")
    void sensibilidadePorCampo() {
        String base = chaveador.assinatura(10, "Thank you.", "Default", List.of("Norris!"), "v1");
        assertNotEquals(base, chaveador.assinatura(11, "Thank you.", "Default", List.of("Norris!"), "v1"), "indice");
        assertNotEquals(base, chaveador.assinatura(10, "Thanks.", "Default", List.of("Norris!"), "v1"), "original");
        assertNotEquals(base, chaveador.assinatura(10, "Thank you.", "Italic", List.of("Norris!"), "v1"), "estilo");
        assertNotEquals(base, chaveador.assinatura(10, "Thank you.", "Default", List.of("Karen!"), "v1"), "vizinhas");
        assertNotEquals(base, chaveador.assinatura(10, "Thank you.", "Default", List.of("Norris!"), "v2"), "politica");
        assertNotEquals(base, chaveador.assinatura(10, "Thank you.", "Default", List.of(), "v1"), "sem vizinhas");
    }

    @Test
    @DisplayName("assinatura: tolera nulos sem lancar e continua deterministica")
    void toleraNulos() {
        String a = chaveador.assinatura(0, null, null, null, null);
        String b = chaveador.assinatura(0, null, null, null, null);
        assertEquals(a, b);
        assertEquals(64, a.length());
    }
}
