package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

@Component
public class ContextoRevisaoLoreGundam0083 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0083: Stardust Memory, Universal Century U.C. 0083.
        - Regra central: manter nomes oficiais de personagens, operacoes, faccoes, naves, mobile suits, mobile armors, armas e termos UC.
        - Personagens: Kou Uraki, Anavel Gato, Nina Purpleton, South Burning, Cima Garahau, Aiguille Delaz, Chuck Keith, Mora Bascht, Bernard Monsha, Alpha A. Bate, Chap Adel.
        - Faccao/organizacoes: Earth Federation, Federation, Principality of Zeon, Delaz Fleet, Zeon, Anaheim Electronics, Titans.
        - Operacoes/eventos: Operation Stardust, Naval Review, Colony Drop, One Year War, Universal Century, U.C.
        - Naves: Albion, Gwaden, Komusai, Musai, Birmingham.
        - Mobile suits/armors: Gundam GP01 Zephyranthes, Gundam GP01Fb Full Burnern, Gundam GP02A Physalis, Gundam GP03 Dendrobium, Neue Ziel, Gelgoog Marine, Dom Tropen, Dra-C, GM Custom, GM Cannon II.
        - Termos UC: mobile suit, mobile armor, beam rifle, beam saber, bazooka, mega particle cannon, Minovsky particles, Newtype.
        - Alertas: Stardust Memory nao deve virar "Memoria de Poeira Estelar" quando for titulo; Physalis, Zephyranthes, Dendrobium e Neue Ziel mantem grafia oficial.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_0083"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam 0083: Stardust Memory - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Frota Delaz", "Delaz Fleet"),
            Map.entry("Dendróbio", "Dendrobium"),
            Map.entry("Dendrobio", "Dendrobium"),
            Map.entry("Novo Alvo", "Neue Ziel"),
            Map.entry("Memória de Poeira Estelar", "Stardust Memory"),
            Map.entry("Memoria de Poeira Estelar", "Stardust Memory"),
            Map.entry("Titãs", "Titans"),
            Map.entry("Titas", "Titans")
        ));
    }
}
