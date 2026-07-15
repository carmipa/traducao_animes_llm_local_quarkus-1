package org.traducao.projeto.traducao.contexto.gundam.reconguista;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

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
}
