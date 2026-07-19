package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link EnforcadorTermosLore} — restaurar a
 * grafia canônica de termos de lore SEM nunca deixar a linha pior.
 *
 * <p>INVARIANTES DO DOMÍNIO: só restaura quando o original contém o termo canônico;
 * não altera traduções legítimas; mapa vazio é no-op.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de guarda ou de fronteira reprova.
 */
class EnforcadorTermosLoreTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();

    private static final Map<String, String> MAPA_86 = Map.of(
        "Legião", "Legion",
        "Handler Um", "Handler One",
        "Processador", "Processor",
        "Coveiro", "Undertaker",
        "Cavaleiro da Morte", "Undertaker");

    @Test
    @DisplayName("restaura Legião->Legion quando o original tem Legion")
    void restauraLegion() {
        String r = enforcador.reforcar("The Legion's out in full force again.",
            "A Legião voltou com tudo outra vez.", MAPA_86);
        assertEquals("A Legion voltou com tudo outra vez.", r);
    }

    @Test
    @DisplayName("restaura dois termos na mesma linha (Handler Um e Cavaleiro da Morte)")
    void restauraDoisTermos() {
        String r = enforcador.reforcar("Handler One to Undertaker:",
            "Handler Um para Cavaleiro da Morte:", MAPA_86);
        assertEquals("Handler One para Undertaker:", r);
    }

    @Test
    @DisplayName("NÃO altera 'Legião' quando o original não tem 'Legion' (tradução legítima)")
    void naoAlteraSemCanonicoNoOriginal() {
        String r = enforcador.reforcar("The Roman legion marched.",
            "A legião romana marchou.", MAPA_86);
        assertEquals("A legião romana marchou.", r,
            "sem 'Legion' no original, a forma comum não pode ser tocada");
    }

    @Test
    @DisplayName("ignora caixa na forma-ruim (legião minúsculo também é restaurado)")
    void ignoraCaixa() {
        String r = enforcador.reforcar("They fear the Legion.",
            "Eles temem a legião.", MAPA_86);
        assertEquals("Eles temem a Legion.", r);
    }

    @Test
    @DisplayName("mapa vazio é no-op")
    void mapaVazioNoOp() {
        assertEquals("A Legião voltou.",
            enforcador.reforcar("The Legion is back.", "A Legião voltou.", Map.of()));
    }

    @Test
    @DisplayName("não casa fragmento parcial de palavra")
    void naoCasaFragmento() {
        // "Processador" como fragmento dentro de outra palavra não deve casar isoladamente;
        // aqui o original não tem 'Processor', então nada muda de qualquer forma.
        String r = enforcador.reforcar("Reprocess the data.", "Reprocessar os dados.", MAPA_86);
        assertEquals("Reprocessar os dados.", r);
    }

    private static final Map<String, String> MAPA_ZETA = Map.of(
        "Titãs", "Titans",
        "Titas", "Titans",
        "Quatro", "Quattro",
        "Eixo", "Axis");

    @Test
    @DisplayName("Zeta: restaura Titãs->Titans quando o original tem Titans")
    void restauraTitansZeta() {
        String r = enforcador.reforcar(
            "She was killed by the Titans.",
            "Ela foi morta pelos Titãs.",
            MAPA_ZETA);
        assertEquals("Ela foi morta pelos Titans.", r);
    }

    @Test
    @DisplayName("Zeta: NÃO altera Titãs mitológico sem Titans no original")
    void naoAlteraTitasMitologicos() {
        String r = enforcador.reforcar(
            "The Greek titans rose against Olympus.",
            "Os titãs gregos se ergueram contra o Olimpo.",
            MAPA_ZETA);
        assertEquals("Os titãs gregos se ergueram contra o Olimpo.", r);
    }

    @Test
    @DisplayName("Zeta: restaura Quatro->Quattro quando o original tem Quattro")
    void restauraQuattro() {
        String r = enforcador.reforcar(
            "Lt. Quattro's not the type of person you think he is.",
            "Tenente Quatro não é o tipo de pessoa que você pensa que ele é.",
            MAPA_ZETA);
        assertEquals("Tenente Quattro não é o tipo de pessoa que você pensa que ele é.", r);
    }

    @Test
    @DisplayName("Zeta: NÃO altera numeral quatro sem Quattro no original")
    void naoAlteraNumeralQuatro() {
        String r = enforcador.reforcar(
            "There are four mobile suits left.",
            "Restam quatro mobile suits.",
            MAPA_ZETA);
        assertEquals("Restam quatro mobile suits.", r);
    }

    @Test
    @DisplayName("Zeta: restaura Eixo->Axis quando o original tem Axis")
    void restauraAxis() {
        String r = enforcador.reforcar(
            "It must be the Axis?",
            "Pode ser o Eixo?",
            MAPA_ZETA);
        assertEquals("Pode ser o Axis?", r);
    }
}
