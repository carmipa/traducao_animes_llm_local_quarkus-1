package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

@Component
public class ContextoRevisaoLore86 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: 86 - Eighty-Six.
        - Regra central: manter nomes proprios, codinomes, unidades militares, faccoes, tecnologias e termos oficiais no idioma original quando forem lore.
        - Nunca traduzir Eighty-Six como "oitenta e seis" quando for designacao social/militar.
        - Personagens: Shinei "Shin" Nouzen, Vladilena "Lena" Milize, Raiden Shuga, Anju Emma, Theoto Rikka, Kurena Kukumila, Frederica Rosenfort, Ernst Zimmerman, Eugene Rantz.
        - PROTECAO CRITICA: "Shin" e sempre o apelido/nome de Shinei Nouzen. Se a traducao atual tiver "Canela", "canela" ou variantes no lugar de Shin, corrija para "Shin" mantendo a pontuacao original.
        - Codinomes: Undertaker, Handler One, Bloodstained Queen. Manter exatamente quando aparecerem como codinome/titulo.
        - Faccao/termos: Republica de San Magnolia, Imperio de Giad, Federacao de Giad, Legion, Alba, Colorata, Para-RAID, Handler, Processor, Juggernaut, Feldress, Reginleif, Morpho.
        - Unidades: Spearhead Squadron, Nordlicht Squadron; aceitar Esquadrao Spearhead e Esquadrao Nordlicht como forma PT-BR consistente.
        - Terminologia militar: "dud rounds" = municao falha / projeteis falhos, nunca "rodadas aleatorias".
        - Nao corrigir estilo da fala; corrigir somente nome, local, organizacao, unidade, objeto, tecnologia ou termo de lore traduzido errado.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "eight_six"; }
    @Override public String getNomeExibicao() { return "86 (Eighty-Six) - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: formas-ruim de terminologia do 86 que a revisão restaura
     * deterministicamente — mesmo mapa da tradução local ({@code Contexto86}).
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; só aplica quando
     * o original EN contém o canônico na grafia exata.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return Map.of(
            "Legião", "Legion",
            "Handler Um", "Handler One",
            "Processador", "Processor",
            "Coveiro", "Undertaker",
            "Cavaleiro da Morte", "Undertaker"
        );
    }
}
