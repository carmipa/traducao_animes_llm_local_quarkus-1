package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Gundam SEED (série CE 71) — sem misturar Destiny.
 *
 * <p>INVARIANTES DO DOMÍNIO: elenco SEED apenas; Shinn Asuka pertence a Destiny;
 * Coordinator/Natural; Freedom/Justice/Strike.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundamSEED implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED (Cosmic Era) — série TV SEED apenas.
        - NÃO incluir elenco exclusivo de SEED Destiny (Shinn Asuka, Lunamaria, Stella, Durandal, etc.).
          Para Destiny use o contexto gundam_seed_destiny.
        - Personagens (gênero): Kira Yamato (m), Athrun Zala (m), Lacus Clyne (f),
          Cagalli Yula Athha (f), Mu La Flaga (m), Rau Le Creuset (m), Murrue Ramius (f),
          Natarle Badgiruel (f), Flay Allster (f), Sai Argyle (m), Dearka Elsman (m),
          Yzak Joule (m), Nicol Amalfi (m), Andrew Waltfeld (m), Patrick Zala (m),
          Siegel Clyne (m), Miriallia Haw (f).
        - Facções/termos: ZAFT, Earth Alliance / OMNI, PLANTs, Coordinator, Natural,
          Blue Cosmos, Eurasian Federation, Orb Union, Archangel, Eternal.
        - Mecha: GAT-X105 Strike Gundam, ZGMF-X10A Freedom Gundam, ZGMF-X09A Justice Gundam,
          Aegis, Blitz, Duel, Buster, Providence Gundam, METEOR.
        - Regras: Coordinator/Natural/ZAFT/PLANT oficiais; Freedom/Justice/Strike como nomes;
          não traduzir Kira/Lacus/Athrun/Cagalli.
        - Tom: drama militar CE, romance, racismo Coordinator vs Natural.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED", LORE);

    @Override
    public String getId() {
        return "gundam_seed";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam SEED";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Kira Yamato", "Athrun Zala", "Lacus Clyne", "Cagalli Yula Athha",
            "Mu La Flaga", "Rau Le Creuset", "ZAFT", "PLANT", "Coordinator", "Natural",
            "Freedom Gundam", "Justice Gundam", "Strike Gundam", "Archangel"
        );
    }
}
