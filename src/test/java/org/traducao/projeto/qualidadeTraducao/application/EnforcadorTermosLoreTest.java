package org.traducao.projeto.qualidadeTraducao.application;

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

    /**
     * PROPÓSITO DE NEGÓCIO: o teto de ocorrências protege o HOMÓGRAFO COMUM MINÚSCULO, e só ele.
     * Aplicá-lo também às capitalizadas produzia falas MEIO corrigidas — o pior desfecho, porque
     * a auditoria seguinte da revisão de lore aprova a linha (basta UMA ocorrência canônica para o
     * detector parar de acusar) e o defeito é gravado no {@code .ass} e logado em verde.
     *
     * <p>Caso medido com o mapa real de ZZ ({@code "Eixo"→"Axis"}).
     */
    @Test
    @DisplayName("duas formas-ruim CAPITALIZADAS são ambas restauradas, mesmo com o canônico 1x no EN")
    void capitalizadasNaoSaoLimitadasPeloTeto() {
        assertEquals("Precisamos deter o Axis antes que o Axis caia!",
            enforcador.reforcar("We must stop Axis before it falls!",
                "Precisamos deter o Eixo antes que o Eixo caia!", Map.of("Eixo", "Axis")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prende o furo que a revisão externa achou no teto por caixa. Em
     * português TODA frase começa com maiúscula, então a caixa na abertura de período é
     * obrigatória e não distingue o nome próprio da palavra comum. Tratá-la como evidência fazia
     * o teto vazar exatamente onde ele existe para segurar.
     */
    @Test
    @DisplayName("maiúscula de INÍCIO DE FRASE não é evidência: o teto continua valendo")
    void maiusculaDeAberturaDePeriodoNaoEscapaDoTeto() {
        // O inglês traz "Void" UMA vez; as duas ocorrências em PT abrem período.
        assertEquals("Void é perigoso. Vazio deve ser destruído.",
            enforcador.reforcar("We must destroy the Void.",
                "Vazio é perigoso. Vazio deve ser destruído.", Map.of("Vazio", "Void")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a distinção tem de ser POSICIONAL, não de caixa. Maiúscula no MEIO
     * da frase foi escolha de quem escreveu — é prova de nome próprio — e continua isenta do
     * teto, que é o que conserta a fala meio corrigida do caso Axis.
     */
    @Test
    @DisplayName("maiúscula no MEIO da frase segue sendo prova: as duas são restauradas")
    void maiusculaNoMeioDaFraseSegueIsentaDoTeto() {
        assertEquals("Precisamos deter o Axis antes que o Axis caia!",
            enforcador.reforcar("We must stop Axis before it falls!",
                "Precisamos deter o Eixo antes que o Eixo caia!", Map.of("Eixo", "Axis")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a contrapartida — a proteção do homógrafo minúsculo NÃO pode
     * afrouxar junto. São os dois casos que motivaram o teto.
     */
    @Test
    @DisplayName("o homógrafo comum MINÚSCULO continua protegido pelo teto")
    void minusculaSegueProtegidaPeloTeto() {
        assertEquals("O Void deixou tudo vazio.",
            enforcador.reforcar("The Void left everything void.",
                "O Vazio deixou tudo vazio.", Map.of("Vazio", "Void")));
        assertEquals("Quattro, há quatro inimigos.",
            enforcador.reforcar("Quattro, there are four enemies.",
                "Quatro, há quatro inimigos.", Map.of("Quatro", "Quattro")));
    }

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
    @DisplayName("Break Blade: restaura un-sorcerer/Delphine/Valkyrie Squadron")
    void restauraBreakBlade() {
        var mapa = org.traducao.projeto.lore.LoreDeTeste.terminologia("break_blade_1");
        assertEquals(
            "Rygart é un-sorcerer e pilota a Delphine.",
            enforcador.reforcar(
                "Rygart is an un-sorcerer and pilots the Delphine.",
                "Rygart é Não-feiticeiro e pilota a Delfine.",
                mapa));
        assertEquals(
            "O Valkyrie Squadron serve a Athens Commonwealth.",
            enforcador.reforcar(
                "The Valkyrie Squadron serves the Athens Commonwealth.",
                "O Esquadrão Valquíria serve a Comunidade de Atenas.",
                mapa));
        assertEquals(
            "Hykelion enfrenta o Heavy Knight.",
            enforcador.reforcar(
                "Hykelion faces the Heavy Knight.",
                "Hykélion enfrenta o Cavaleiro Pesado.",
                mapa));
    }

    @Test
    @DisplayName("Macross Delta: restaura Walküre/Var Syndrome/Delta Flight")
    void restauraMacrossDelta() {
        var mapa = org.traducao.projeto.lore.LoreDeTeste.terminologia("macross_delta");
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
        var origin = comNucleoUc(
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

        var zeta = comNucleoUc(Map.of("Cem Estilos", "Hyaku Shiki", "Titãs", "Titans"));
        assertEquals(
            "Titans pilotam o Hyaku Shiki.",
            enforcador.reforcar(
                "Titans pilot the Hyaku Shiki.",
                "Titãs pilotam o Cem Estilos.",
                zeta));

        var unicorn = comNucleoUc(Map.of("Mangas", "Sleeves", "Caixa de Laplace", "Laplace's Box"));
        assertEquals(
            "Os Sleeves querem a Laplace's Box.",
            enforcador.reforcar(
                "The Sleeves want Laplace's Box.",
                "Os Mangas querem a Caixa de Laplace.",
                unicorn));

        var dyrl = org.traducao.projeto.lore.LoreDeTeste.terminologia("macross_filme1");
        assertEquals(
            "Protoculture une Zentradi e Meltrandi.",
            enforcador.reforcar(
                "Protoculture unites Zentradi and Meltrandi.",
                "Protocultura une Zentradi e Meltrandi.",
                dyrl));

        var m2 = org.traducao.projeto.lore.LoreDeTeste.terminologia("macross_2");
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
        var mapa = org.traducao.projeto.lore.LoreDeTeste.terminologia("guilty_crown");
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
        assertEquals(
            "Anti Bodies vigiam o Second Hand.",
            enforcador.reforcar(
                "Anti Bodies watch the Second Hand.",
                "Anticorpos vigiam o Segunda Mão.",
                mapa));
        assertEquals(
            "Piloto de Endlave com Genomic Resonance.",
            enforcador.reforcar(
                "Endlave pilot with Genomic Resonance.",
                "Piloto de Endslave com Ressonância Genômica.",
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
        var mapa = org.traducao.projeto.lore.LoreDeTeste.terminologia("gundam_ms_igloo");
        assertEquals("Mobile Suits!",
            enforcador.reforcar("Mobile suits!", "Móveis Suits!", mapa));
    }

    @Test
    @DisplayName("multi-palavra: 'Mobile Suit' minúsculo no EN dispara restauração de 'traje móvel'")
    void restauraMobileSuitComEnMinusculo() {
        // Termo técnico composto não colide com palavra comum como um nome próprio de 1 palavra;
        // por isso a checagem do canônico é case-insensitive para multi-palavra (Void/Titans, de
        // 1 palavra, seguem SENSÍVEIS à caixa — cobertos por naoAlteraTitasMitologicos/homografo).
        var mapa = org.traducao.projeto.lore.LoreDeTeste.terminologia("gundam_ms_igloo");
        assertEquals("Lance o Mobile Suit.",
            enforcador.reforcar("Deploy the mobile suit.", "Lance o traje móvel.", mapa));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconstrói o que {@code CorrecoesTerminologiaGundamUc.comExtras()}
     * fazia, agora que aquela classe não existe — a lore virou o arquivo único em 2026-08-15.
     *
     * <p>INVARIANTES DO DOMÍNIO: núcleo UC + extras, extras vencendo, exatamente como o método
     * deletado. O núcleo vem de {@code gundam_ms_igloo}, que é uma das quatro obras que usavam
     * o núcleo PURO, sem extras próprios — logo o mapa dela NO ARQUIVO é o núcleo.
     *
     * <p>Por que não usar a obra real (ex.: {@code gundam_unicorn}) e pronto: estes casos testam
     * o ENFORCADOR com um mapa montado à mão, não o conteúdo do catálogo. Um deles usa
     * {@code "Caixa de Laplace" -> "Laplace's Box"}, que a lore real do Unicorn NÃO tem — trocar
     * pelo mapa da obra mudaria silenciosamente o que está sendo provado, e o teste passaria a
     * falar de outra coisa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: id ausente reprova em {@code LoreDeTeste}.
     */
    private static Map<String, String> comNucleoUc(Map<String, String> extras) {
        Map<String, String> combinado =
            new java.util.LinkedHashMap<>(org.traducao.projeto.lore.LoreDeTeste.terminologia("gundam_ms_igloo"));
        combinado.putAll(extras);
        return java.util.Collections.unmodifiableMap(combinado);
    }}
