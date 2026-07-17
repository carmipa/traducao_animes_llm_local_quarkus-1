package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore do filme Gundam SEED FREEDOM.
 *
 * <p>INVARIANTES DO DOMÍNIO: Rising Freedom; Immortal Justice; Mighty Strike Freedom;
 * Kingdom of Foundation; Orphee Lam Tao; Agnes Giebenrath.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundamSEEDFreedom implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED FREEDOM (filme Cosmic Era).
        - Personagens (gênero): Kira Yamato (m), Lacus Clyne (f), Athrun Zala (m),
          Shinn Asuka (m), Lunamaria Hawke (f), Cagalli Yula Athha (f),
          Agnes Giebenrath (f), Orphee Lam Tao (m), Ingrid Tradoll (f),
          Daniela Chandler (f), Redelard Tradoll (f), Mu La Flaga (m),
          Meyrin Hawke (f), Yzak Joule (m), Dearka Elsman (m).
        - Facções: Compass, Kingdom of Foundation, ZAFT, Orb, Eurasia.
        - Mecha: Rising Freedom Gundam, Immortal Justice Gundam, Mighty Strike Freedom Gundam,
          Destiny Gundam Spec II, Black Knight Squad / Black Knights mecha conforme cena.
        - Regras: nomes oficiais EN dos MS; Foundation/Compass; Orphee/Agnes grafias;
          não traduzir Freedom/Justice no nome da unidade.
        - Tom: filme de ação CE, conspiração Foundation, reencontro do elenco SEED/Destiny.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED FREEDOM", LORE);

    @Override
    public String getId() {
        return "gundam_seed_freedom";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam SEED Freedom";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Kira Yamato", "Lacus Clyne", "Athrun Zala", "Shinn Asuka",
            "Agnes Giebenrath", "Orphee Lam Tao", "Rising Freedom Gundam",
            "Immortal Justice Gundam", "Mighty Strike Freedom Gundam", "Compass"
        );
    }
}
