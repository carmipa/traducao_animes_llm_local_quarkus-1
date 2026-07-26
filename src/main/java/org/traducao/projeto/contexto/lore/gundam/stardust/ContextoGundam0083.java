package org.traducao.projeto.contexto.lore.gundam.stardust;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam 0083: Stardust Memory (OVA UC 0083) —
 * GP series, Delaz Fleet, Operation Stardust e germes dos Titans.
 *
 * <p>INVARIANTES DO DOMÍNIO: Stardust Memory; Delaz Fleet; Anavel Gato; Dendrobium; Physalis;
 * Neue Ziel; Titans ≠ Titãs; Newtype oficial; tom militar adulto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundam0083 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0083: Stardust Memory (OVA) — Universal Century U.C. 0083.
        - Premissa: roubo do Gundam GP02A Physalis pela Delaz Fleet; perseguição pela Albion;
          Operation Stardust / Colony Drop; conspiracao que prepara os Titans.
        - Tom: drama militar sombrio, honra/dever, fanatismo Zeon, tragedia Kou/Nina/Gato.
          Evitar girias modernas; dialogo militar objetivo.

        === Nucleo UC ===
        - Newtype / Oldtype; Spacenoid vs Earthnoid; Minovsky particles;
          Mega Particle Cannon; Beam Rifle / Beam Saber.
        - Mobile Suit vs Mobile Armor (Neue Ziel e Mobile Armor).
        - Earth Federation; Principality of Zeon / Zeon remnants; One Year War (legado).

        === Roster — Federacao / Albion / Anaheim ===
        - Kou Uraki (m); Nina Purpleton (f); South Burning (m); Eiphar Synapse (m);
          Chuck Keith (m); Mora Bascht (f); Bernard Monsha (m); Chap Adel (m);
          Alpha A. Bate (m) quando aparecer; John Kowen (m); Adelheid Bernard (f) quando aparecer.
        - Jamitov Hymem (m); Bask Om (m) — germes dos Titans.

        === Roster — Delaz Fleet / Zeon ===
        - Anavel Gato (m) — Nightmare of Solomon (so quando o EN trouxer o epiteto).
        - Aiguille Delaz (m); Cima Garahau (f); Kelly Layzner (m).

        === Mecha ===
        - RX-78GP01 Gundam GP01 Zephyranthes; GP01Fb Full Burnern;
          RX-78GP02A Gundam GP02A Physalis; RX-78GP03 Gundam GP03 Dendrobium /
          Dendrobium Orchis; AGX-04 Gerbera Tetra; AMA-X2 Neue Ziel;
          RGM-79N GM Custom; RGC-83 GM Cannon II;
          Gelgoog Marine; Dom Tropen; Dra-C; Val-Walo quando aparecerem.

        === Naves / orgs / operacoes ===
        - Albion; La Vie en Rose; Gwaden; Birmingham; Musai / Komusai quando aparecerem.
        - Delaz Fleet; Anaheim Electronics; Titans (NUNCA Titãs); Earth Federation.
        - Operation Stardust; Colony Drop; Solar System II; Naval Review.

        === Regras duras ===
        - Stardust Memory NUNCA "Memoria de Poeira Estelar" como titulo.
        - Physalis / Zephyranthes / Dendrobium / Neue Ziel / Gerbera Tetra grafias oficiais.
        - Delaz Fleet NUNCA "Frota Delaz"; Titans NUNCA "Titas"; Sieg Zeon! permanece.
        - Patentes: Lieutenant→Tenente, Captain→Capitao, Admiral→Almirante (nome proprio intacto).
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Mobile Suit Gundam 0083: Stardust Memory",
        LORE
    );

    @Override
    public String getId() {
        return "gundam_0083";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam 0083: Stardust Memory";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: vocabulário que identifica esta obra pelo nome da pasta, para a
     * guarda obra×contexto. Nasceu do incidente medido nesta árvore: 15 caches desta obra
     * foram gravados com {@code proveniencia.contextoId = "guilty_crown"} e ~4.442 entradas
     * produzidas sob a lore errada (um {@code "No."} virou uma frase sobre Shu Ouma e o
     * Funeral Parlor). Com estes apelidos, o mesmo clique errado no combo é BLOQUEADO
     * antes da primeira chamada ao LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: complementam a identidade canônica DERIVADA do id e do nome de
     * exibição, cobrindo o subtítulo ({@code "Stardust Memory"}) e a grafia curta que os grupos
     * usam quando abreviam {@code "Mobile Suit"}. O {@code 0083} É a identidade desta obra e por
     * isso está aqui: o par mínimo com {@code "Gundam 0080"} (War in the Pocket) é o último
     * dígito, e apagar números por parecerem "ruído de release" fundiria as duas OVAs. Já
     * {@code "0083"} SOZINHO fica de fora de propósito — sem a palavra {@code "Gundam"} ao lado,
     * um número solto no nome da pasta não é prova de obra nenhuma.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Set<String> apelidosPasta() {
        return Set.of("Gundam 0083", "Stardust Memory");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco 0083, GP series, Delaz Fleet e operações canônicas.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais Stardust Memory / UC 0083.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Kou Uraki", "Anavel Gato", "Nina Purpleton",
            "South Burning", "Eiphar Synapse", "Aiguille Delaz",
            "Cima Garahau", "Kelly Layzner", "Chuck Keith",
            "Mora Bascht", "Bernard Monsha", "Chap Adel", "Alpha A. Bate",
            "Jamitov Hymem", "Bask Om", "Adelheid Bernard", "John Kowen",
            "Nightmare of Solomon",
            "Gundam GP01 Zephyranthes", "Full Burnern", "Zephyranthes",
            "Gundam GP02A Physalis", "Physalis",
            "GP03 Dendrobium", "Dendrobium", "Dendrobium Orchis",
            "Gerbera Tetra", "Neue Ziel", "GM Custom", "GM Cannon II",
            "Gelgoog Marine", "Dom Tropen", "Dra-C", "Val-Walo",
            "Albion", "La Vie en Rose", "Gwaden", "Birmingham",
            "Earth Federation", "Zeon", "Principality of Zeon",
            "Delaz Fleet", "Anaheim Electronics", "Titans",
            "Operation Stardust", "Colony Drop", "Solar System II", "Naval Review",
            "Stardust Memory", "Sieg Zeon",
            "Mobile Suit", "Mobile Armor", "Gundam", "Beam Saber", "Beam Rifle",
            "Newtype", "Oldtype", "Minovsky", "Mega Particle Cannon", "One Year War"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim próprias de 0083 (Delaz, Dendrobium, Titans…).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN; canônicos dos extras
     * estão em {@link #termosProtegidos()}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Frota Delaz", "Delaz Fleet"),
            Map.entry("Dendróbio", "Dendrobium"),
            Map.entry("Dendrobio", "Dendrobium"),
            Map.entry("Novo Alvo", "Neue Ziel"),
            Map.entry("Memória de Poeira Estelar", "Stardust Memory"),
            Map.entry("Memoria de Poeira Estelar", "Stardust Memory"),
            Map.entry("Titãs", "Titans"),
            Map.entry("Titas", "Titans"),
            Map.entry("Operação Stardust", "Operation Stardust"),
            Map.entry("Operacao Stardust", "Operation Stardust"),
            Map.entry("Queda de Colônia", "Colony Drop"),
            Map.entry("Queda de Colonia", "Colony Drop"),
            Map.entry("Sistema Solar II", "Solar System II"),
            Map.entry("Pesadelo de Solomon", "Nightmare of Solomon"),
            Map.entry("Físalis", "Physalis"),
            Map.entry("Fisalis", "Physalis"),
            Map.entry("Principado de Zeon", "Principality of Zeon")
        ));
    }
}
