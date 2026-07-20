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

    private static final Map<String, String> MAPA_ZZ = Map.of(
        "Eixo", "Axis",
        "Titãs", "Titans",
        "Novo Tipo", "Newtype",
        "Plê", "Ple",
        "Plee", "Ple");

    @Test
    @DisplayName("ZZ: restaura Eixo->Axis e Plê->Ple")
    void restauraAxisEPleZz() {
        assertEquals(
            "Tripulantes da Argama, eu sou Mashymre Cello do Axis.",
            enforcador.reforcar(
                "Crew of the Argama, I am Mashymre Cello of Axis.",
                "Tripulantes da Argama, eu sou Mashymre Cello do Eixo.",
                MAPA_ZZ));
        assertEquals(
            "Por favor, Ple!",
            enforcador.reforcar("Please, Ple!", "Por favor, Plê!", MAPA_ZZ));
    }

    private static final Map<String, String> MAPA_MACROSS = Map.of(
        "Valquíria", "Valkyrie",
        "Valquiria", "Valkyrie",
        "Veritech", "Valkyrie",
        "Zentraedi", "Zentradi");

    @Test
    @DisplayName("Macross: restaura Valquíria->Valkyrie e bloqueia Veritech")
    void restauraValkyrieMacross() {
        assertEquals(
            "A Valkyrie transformou em Battroid.",
            enforcador.reforcar(
                "The Valkyrie transformed into Battroid.",
                "A Valquíria transformou em Battroid.",
                MAPA_MACROSS));
        assertEquals(
            "Pilote a Valkyrie.",
            enforcador.reforcar("Pilot the Valkyrie.", "Pilote a Veritech.", MAPA_MACROSS));
    }

    @Test
    @DisplayName("Macross Delta: restaura Walküre/Var Syndrome/Delta Flight")
    void restauraMacrossDelta() {
        var mapa = org.traducao.projeto.contexto.lore.macross.CorrecoesTerminologiaMacrossDelta.mapa();
        assertEquals(
            "A Walküre canta contra a Var Syndrome.",
            enforcador.reforcar(
                "Walküre sings against the Var Syndrome.",
                "A Walkure canta contra a Síndrome Var.",
                mapa));
        assertEquals(
            "Delta Flight protege o show.",
            enforcador.reforcar(
                "Delta Flight protects the show.",
                "Esquadrão Delta protege o show.",
                mapa));
        assertEquals(
            "Yami_Q_Ray enfrenta Heimdall na Macross Gigant.",
            enforcador.reforcar(
                "Yami_Q_Ray faces Heimdall on the Macross Gigant.",
                "Yami Q-Ray enfrenta Heimdal na Gigante Macross.",
                mapa));
    }

    @Test
    @DisplayName("Origin/Zeta/Unicorn/DYRL: restaura formas-ruim canônicas")
    void restauraUcEMacrossClasse() {
        var origin = org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc.comExtras(
            Map.ofEntries(
                Map.entry("Base Branca", "White Base"),
                Map.entry("Zion", "Zeon"),
                Map.entry("Triângulo Negro", "Black Tri-Stars"),
                Map.entry("Eduardo Mass", "Edouard Mass")
            ));
        assertEquals(
            "A White Base partiu de Zeon.",
            enforcador.reforcar(
                "The White Base left Zeon.",
                "A Base Branca partiu de Zion.",
                origin));
        assertEquals(
            "Black Tri-Stars e Edouard Mass.",
            enforcador.reforcar(
                "Black Tri-Stars and Edouard Mass.",
                "Triângulo Negro e Eduardo Mass.",
                origin));

        var zeta = org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc.comExtras(
            Map.of("Cem Estilos", "Hyaku Shiki", "Titãs", "Titans"));
        assertEquals(
            "Titans pilotam o Hyaku Shiki.",
            enforcador.reforcar(
                "Titans pilot the Hyaku Shiki.",
                "Titãs pilotam o Cem Estilos.",
                zeta));

        var unicorn = org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc.comExtras(
            Map.of("Mangas", "Sleeves", "Caixa de Laplace", "Laplace's Box"));
        assertEquals(
            "Os Sleeves querem a Laplace's Box.",
            enforcador.reforcar(
                "The Sleeves want Laplace's Box.",
                "Os Mangas querem a Caixa de Laplace.",
                unicorn));

        var dyrl = org.traducao.projeto.contexto.lore.macross.CorrecoesTerminologiaMacrossDyrl.mapa();
        assertEquals(
            "Protoculture une Zentradi e Meltrandi.",
            enforcador.reforcar(
                "Protoculture unites Zentradi and Meltrandi.",
                "Protocultura une Zentradi e Meltrandi.",
                dyrl));

        var m2 = org.traducao.projeto.contexto.lore.macross.CorrecoesTerminologiaMacross2.mapa();
        assertEquals(
            "A Emulator canta o Minmay Attack.",
            enforcador.reforcar(
                "The Emulator sings the Minmay Attack.",
                "A Emulador canta o Ataque Minmay.",
                m2));
    }

    @Test
    @DisplayName("Guilty Crown: restaura Funerária/Vazio/Coveiro canônicos")
    void restauraGuiltyCrown() {
        var mapa = org.traducao.projeto.contexto.lore.guiltycrown.CorrecoesTerminologiaGuiltyCrown.mapa();
        assertEquals(
            "Ele entrou na Funeral Parlor com o Void Genome.",
            enforcador.reforcar(
                "He joined Funeral Parlor with the Void Genome.",
                "Ele entrou na Funerária com o Genoma do Vazio.",
                mapa));
        assertEquals(
            "Undertaker lidera o ataque.",
            enforcador.reforcar(
                "Undertaker leads the attack.",
                "Coveiro lidera o ataque.",
                mapa));
        // Regressão real (Guilty Crown E14): o LLM manteve "Void" em inglês e só traduziu
        // "Genome" -> "Genoma Void", forma que escapava do mapa ("Genoma do Vazio"/"Genoma Vazio").
        assertEquals(
            "Shu herdou o Void Genome.",
            enforcador.reforcar(
                "Shu inherited the Void Genome.",
                "Shu herdou o Genoma Void.",
                mapa));
    }

    @Test
    @DisplayName("#2: não corrompe homógrafo comum — só a forma capitalizada (Void) é restaurada")
    void naoCorrompeHomografoComum() {
        // 'Vazio' = Void (nome próprio); 'vazio' = empty (palavra comum). Só o 1º vira Void.
        String r = enforcador.reforcar(
            "The Void made everything empty.",
            "O Vazio deixou tudo vazio.",
            Map.of("Vazio", "Void"));
        assertEquals("O Void deixou tudo vazio.", r);
    }

    @Test
    @DisplayName("#2: restaura no máximo tantas formas-ruim quanto o canônico aparece no original")
    void restauraNoMaximoOcorrenciasDoCanonico() {
        // 'Void' aparece 2x no original -> restaura os 2 'Vazio' capitalizados; 'vazio' (empty) fica.
        String r = enforcador.reforcar(
            "Void here, Void there, all empty.",
            "Vazio aqui, Vazio ali, tudo vazio.",
            Map.of("Vazio", "Void"));
        assertEquals("Void aqui, Void ali, tudo vazio.", r);
    }

    @Test
    @DisplayName("08th: composto meio-traduzido 'Móveis Suits' -> 'Mobile Suits' mesmo com EN minúsculo")
    void restauraCompostoMeioTraduzidoMobileSuits() {
        // O LLM meio-traduziu ("Mobile"->"Móveis", manteve "Suits") e o EN veio minúsculo
        // ("Mobile suits!"). Termo multi-palavra é reconhecido case-insensitive no original.
        var mapa = org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc.mapa();
        assertEquals("Mobile Suits!",
            enforcador.reforcar("Mobile suits!", "Móveis Suits!", mapa));
    }

    @Test
    @DisplayName("multi-palavra: 'Mobile Suit' minúsculo no EN dispara restauração de 'traje móvel'")
    void restauraMobileSuitComEnMinusculo() {
        // Termo técnico composto não colide com palavra comum como um nome próprio de 1 palavra;
        // por isso a checagem do canônico é case-insensitive para multi-palavra (Void/Titans, de
        // 1 palavra, seguem SENSÍVEIS à caixa — cobertos por naoAlteraTitasMitologicos/homografo).
        var mapa = org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc.mapa();
        assertEquals("Lance o Mobile Suit.",
            enforcador.reforcar("Deploy the mobile suit.", "Lance o traje móvel.", mapa));
    }
}
