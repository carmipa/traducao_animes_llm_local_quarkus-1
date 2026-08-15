package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam F91.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamF91}) — reestruturacao de formato, nao pesquisa
 * nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala, entao a
 * linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>O mapa de terminologia espelha EXATAMENTE o do lado da Traducao. Divergir poe a obra em
 * {@code DIVERGENCIAS_DECLARADAS} do {@code ParidadeMapasTerminologiaTest} — divida que so se
 * assume com motivo escrito, uma obra por vez.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutaveis.
 */
@Component
public class ContextoRevisaoLoreGundamF91 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam F91, U.C. 0123 (Cosmo Babylonia War).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: F91 Gundam Formula 91, Crossbone Vanguard, VSBR (Variable Speed Beam Rifle),
          MEPE (Afterimage Effect), Bio-Computer, Denan Zon, Denan Gei, Berga-Giros,
          Vigna-Ghina, Rafflesia.
        - Personagens: Seabook Arno, Cecily Fairchild/Berah Ronah, Carozzo Ronah/Iron Mask,
          Zabine Chareux, Annamarie Bourget, Dorel Ronah, Nadia Ronah, Theo Fairchild,
          Birgit, Reese, Sam, Arthur, Azuma, Leahlee, Monica Arno, Gillet.
        - Familia Ronah: Carozzo, Dorel, Nadia e Berah sao pessoas DIFERENTES; Iron Mask e
          o codinome de Carozzo.
        - Colonias/lugares: Frontier I, Frontier II, Frontier III, Frontier IV, Frontier Side,
          Richmond, Earth Federation. "Frontier Ill" na legenda e erro de OCR: leia Frontier III.
        - Naves: Space Ark, Zamouth Garr.
        - Bugs: armas autonomas, nome proprio.
        - Cosmo Babylonia (Estado) e Cosmo Aristocracy (doutrina) sao distintos.
        - Alertas: Iron Mask nao vira "Mascara de Ferro"; Crossbone Vanguard nao vira
          "Vanguarda Crossbone" nem "Vanguard Crossbone"; Cosmo Babylonia nao vira
          "Cosmo Babilonia"; Space Ark nao vira "Arca Espacial"; Zamouth Garr nao vira
          "Garra de Zamouth"; Bugs nao vira "insetos"; Newtype nao vira "Nova Tipo".
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_f91"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam F91 - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa deterministico UC na Opcao 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho exato do lado da Traducao desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        // ESPELHO EXATO dos extras do lado da Tradução (ContextoGundamF91) — divergir põe a
        // obra em DIVERGENCIAS_DECLARADAS do ParidadeMapasTerminologiaTest. Formas-ruim
        // medidas na tradução de 2026-07-30; cada uma tem uma fala real por trás.
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Máscara de Ferro", "Iron Mask"),
            Map.entry("Vanguarda Crossbone", "Crossbone Vanguard"),
            Map.entry("Cosmo Babilônia", "Cosmo Babylonia"),
            Map.entry("insetos", "Bugs"),
            Map.entry("Arca Espacial", "Space Ark"),
            Map.entry("Fronteira I", "Frontier I"),
            Map.entry("Fronteira II", "Frontier II"),
            Map.entry("Fronteira III", "Frontier III"),
            Map.entry("Fronteira IV", "Frontier IV"),
            Map.entry("IV Fronteira", "Frontier IV"),
            Map.entry("Garra de Zamouth", "Zamouth Garr"),
            Map.entry("Zamouth Gar", "Zamouth Garr"),
            Map.entry("Vanguard Crossbone", "Crossbone Vanguard"),
            Map.entry("Vanguard da Cruz Branca", "Crossbone Vanguard"),
            Map.entry("Aristocracia Cósmica", "Cosmo Aristocracy"),
            Map.entry("Cosmo Aristocracia", "Cosmo Aristocracy"),
            Map.entry("Babylonia do Cosmo", "Cosmo Babylonia"),
            Map.entry("Cosmo Babylônia", "Cosmo Babylonia"),
            Map.entry("Nova Tipo", "Newtype"),
            Map.entry("Carozzo Ronah", "Iron Mask")
        ));
    }
}
