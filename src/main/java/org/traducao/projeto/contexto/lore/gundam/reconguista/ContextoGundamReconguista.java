package org.traducao.projeto.contexto.lore.gundam.reconguista;

import java.util.Map;
import java.util.Set;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamReconguista implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Gundam Reconguista in G / G-Reco, Regild Century.
        - Faccao/forcas: Capital Tower, Capital Guard, Capital Army, Ameria, Towasanga, Venus Globe, Dorette Fleet.
        - Principais nomes: Bellri Zenam, Aida Surugan/Aida Rayhunton, Raraiya Monday, Noredo Nug, Luin Lee, Mask, Klim Nick, Mick Jack, Cumpa Rusita, Wilmit Zenam.
        - Mobile suits e tecnologia: G-Self, G-Arcane, Montero, Mack Knife, Kabakali, G-Lucifer, Photon Battery, Universal Standard.
        - Termos do mundo: Capital Tower, Photon Battery, SU-Cordism, Rayhunton Code, Amerian Army, Towasangan. Mantenha nomes proprios em ingles/romanizados.
        - Tom: aventura sci-fi colorida com politica fragmentada; Bellri e jovem/impulsivo, Aida e determinada, Mask mistura rivalidade e ressentimento.
        - Nao converter "Reconguista" para "Reconquista" quando se referir ao titulo; mantenha Reconguista.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Gundam Reconguista in G", LORE);

    @Override
    public String getId() { return "gundam_greco"; }
    @Override
    public String getNomeExibicao() { return "Gundam: Reconguista in G"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: elenco, mechas, naves, facções e terminologia canônica desta obra
     * que a tradução deve preservar no original — proteção upfront contra localização indevida.
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais; conjunto imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bellri Zenam", "Aida Surugan", "Aida Rayhunton",
            "Raraiya Monday", "Noredo Nug", "Luin Lee",
            "Mask", "Klim Nick", "Mick Jack",
            "Cumpa Rusita", "Wilmit Zenam", "G-Self",
            "G-Arcane", "Montero", "Mack Knife",
            "Kabakali", "Capital Tower", "Capital Guard",
            "Capital Army", "Ameria", "Towasanga",
            "Venus Globe", "Dorette Fleet", "Photon Battery",
            "Rayhunton Code", "Regild Century", "Reconguista",
            "Mobile Suit"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico do núcleo UC (Newtype, Mobile Suit, Beam
     * Saber/Rifle, Mobile Armor, Oldtype) mais os termos próprios desta obra.
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Reconquista", "Reconguista"),
            Map.entry("Bateria de Fóton", "Photon Battery")
        ));
    }
}
