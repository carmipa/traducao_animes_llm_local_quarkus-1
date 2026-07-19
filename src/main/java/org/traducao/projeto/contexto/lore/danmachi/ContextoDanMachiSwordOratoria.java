package org.traducao.projeto.contexto.lore.danmachi;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Sword Oratoria (spin-off Loki Familia / Aiz).
 *
 * <p>INVARIANTES DO DOMÍNIO: nomes oficiais da Loki Familia; Aiz = Sword Princess;
 * Lefiya, Finn, Riveria, Gareth, Tiona, Tione, Bete.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiSwordOratoria implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Sword Oratoria: Is It Wrong to Try to Pick Up Girls in a Dungeon? On the Side.
        - Foco: Loki Familia e Aiz Wallenstein; expedições no Dungeon; arco paralelo a DanMachi.
        - Locais: Orario, Dungeon, Knossos (quando aplicável), Babel, Guilda.
        - Familia: Loki Familia (Loki — deusa, mulher). Manter "Familia" como termo da obra.
        - Personagens (gênero): Aiz Wallenstein (f) — Sword Princess / Princesa Espadachim;
          Lefiya Viridis (f); Finn Deimne (m) — Braver; Riveria Ljos Alf (f) — Nine Hell;
          Gareth Landrock (m); Tiona Hiryute (f); Tione Hiryute (f); Bete Loga (m);
          Loki (f); Raul Nord (m); Anakity Fallas / Anya (f); Bell Cranel (m) quando cruzar.
        - Termos: Falna, Status, Level, Skill, Magic, Dungeon, Monster Rex, Irregular, Excelia.
        - Regras: Aiz Wallenstein e epíteto Sword Princess consistentes; Lefiya não adaptar;
          Finn/Riveria/Gareth/Bete/Tiona/Tione mantêm grafia; Bell ≠ "sino".
        - Tom: exploração, camaradagem da Loki Familia, mistério no Dungeon; Aiz contida, Lefiya ansiosa/determinada.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi: Sword Oratoria", LORE);

    @Override
    public String getId() {
        return "danmachi_so";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi: Sword Oratoria";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes da Loki Familia / Sword Oratoria.
     * <p>INVARIANTES DO DOMÍNIO: só elenco canônico do spin-off.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Aiz Wallenstein", "Lefiya Viridis", "Finn Deimne",
            "Riveria Ljos Alf", "Gareth Landrock", "Tiona Hiryute",
            "Tione Hiryute", "Bete Loga", "Loki",
            "Bell Cranel", "Raul Nord", "Filvis Challia",
            "Revis", "Orario", "Dungeon",
            "Knossos", "Babel", "Guild",
            "Falna", "Familia", "Status",
            "Level", "Skill", "Magic",
            "Sword Princess", "Braver", "Nine Hell",
            "Monster Rex", "Irregular", "Excelia",
            "Loki Familia"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico da terminologia DanMachi (Familia sem
     * acento + grafias erradas de nomes proprios da obra).
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachi.mapa();
    }
}
