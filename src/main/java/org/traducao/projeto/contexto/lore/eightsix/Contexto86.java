package org.traducao.projeto.contexto.lore.eightsix;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class Contexto86 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: 86 - Eighty-Six.
        - A Republica de San Magnolia afirma lutar com drones nao tripulados, mas envia os Eighty-Six, pessoas perseguidas como Colorata, para pilotar Juggernauts nos campos de batalha.
        - Mantenha "Eighty-Six" ou "86" como designacao social/militar; nao traduza como "oitenta e seis" salvo fala explicitamente numerica.
        - Cidadaos de pleno direito da Republica sao os "Alba" (cabelo e olhos prateados); as demais etnias foram confinadas no 86o Distrito e rotuladas coletivamente como "Colorata", termo pejorativo que justifica a propaganda dos "drones automatizados".
        - "Handler" e o cargo/posto dos operadores de radio que comandam os Processors a distancia (ex.: Lena e a Handler do Esquadrao Spearhead); nao traduza como apenas "operador", mantenha Handler como termo da obra, inclusive em formas de chamamento como "Handler One".
        - Faccao/termos: Republica de San Magnolia, Imperio de Giad, Federacao de Giad, Legion, Alba, Colorata, Para-RAID, Handler, Processor, Juggernaut, Feldress, Reginleif, Morpho.
        - Unidades: Spearhead Squadron, Nordlicht Squadron; use Esquadrao Spearhead e Esquadrao Nordlicht.
        - Principais nomes: Shinei "Shin" Nouzen, Vladilena "Lena" Milize, Raiden Shuga, Anju Emma, Theoto Rikka, Kurena Kukumila, Frederica Rosenfort, Ernst Zimmerman, Eugene Rantz.
        - PROTECAO CRITICA DE NOMES: "Shin" e sempre o apelido/nome de Shinei Nouzen. Nunca traduza "Shin" como "canela", mesmo quando a fala for apenas "Shin!", "Shin?", "Shin..." ou "Shin.".
        - Codinomes importantes: Undertaker para Shin; Bloodstained Queen para Lena quando aparecer.
        - Terminologia militar: "dud rounds" = municao falha / projeteis falhos, nao "rodadas aleatorias".
        - Temas: guerra, desumanizacao, racismo institucional, trauma, sobrevivencia e dignidade. Evite suavizar termos duros quando a cena denuncia opressao.
        - Tom: militar e emocionalmente contido; Shin e seco, Lena e formal/idealista, os membros do Spearhead usam ironia amarga e intimidade de esquadrao.
        """;

    private static final String PROMPT = ContextoPrompt.montar("86 - Eighty-Six", LORE);

    @Override
    public String getId() { return "eight_six"; }
    @Override
    public String getNomeExibicao() { return "86 (Eighty-Six)"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
