package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Gundam SEED Destiny — elenco e mecha da sequela.
 *
 * <p>INVARIANTES DO DOMÍNIO: Shinn Asuka; Minerva; Destiny/Impulse/Strike Freedom;
 * Gilbert Durandal; LOGOS.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundamSEEDDestiny implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED Destiny (Cosmic Era) — sequela de SEED.
        - Personagens (gênero): Shinn Asuka (m), Kira Yamato (m), Athrun Zala (m),
          Lacus Clyne (f) / Meer Campbell (f — sósia), Cagalli Yula Athha (f),
          Lunamaria Hawke (f), Stella Loussier (f), Rey Za Burrel (m),
          Gilbert Durandal (m), Neo Roanoke / Mu La Flaga (m), Meyrin Hawke (f),
          Heine Westenfluss (m), Talia Gladys (f), Andrew Waltfeld (m), Yzak Joule (m).
        - Facções/termos: ZAFT, Earth Alliance, PLANTs, LOGOS, Destiny Plan / Plano Destiny,
          Extended, Coordinator, Natural, Minerva, Archangel, Eternal.
        - Mecha: ZGMF-X42S Destiny Gundam, ZGMF-X20A Strike Freedom Gundam,
          ZGMF-X19A Infinite Justice Gundam, ZGMF-X56S Impulse Gundam,
          Saviour, Chaos, Abyss, Gaia, Legend Gundam.
        - Regras: Shinn/Lunamaria/Stella/Durandal/Rey grafias oficiais; Destiny Plan como nome;
          não reduzir Destiny Gundam a "Destino" no nome da unidade.
        - Tom: guerra CE contínua, manipulação política, trauma Extended, rivalidade Shinn/Kira.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED Destiny", LORE);

    @Override
    public String getId() {
        return "gundam_seed_destiny";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam SEED Destiny";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shinn Asuka", "Kira Yamato", "Athrun Zala", "Lacus Clyne", "Lunamaria Hawke",
            "Stella Loussier", "Gilbert Durandal", "Rey Za Burrel", "Minerva", "LOGOS",
            "Destiny Gundam", "Strike Freedom Gundam", "Impulse Gundam", "ZAFT"
        );
    }
}
