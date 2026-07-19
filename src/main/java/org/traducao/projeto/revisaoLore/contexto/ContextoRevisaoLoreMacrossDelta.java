package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Delta (Série TV).
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDelta implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta (Série TV).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler, Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Messer Ihlefeld, Keith Aero Windermere, Heinz Nerich Windermere, Walküre, Windermere, Var Syndrome, VF-31 Siegfried, Chaos, GERWALK, Battroid, Valkyrie, Overtechnology, Fold.
        - Premissa: Em 2067 no aglomerado de Brisingr, a Síndrome Var causa fúria incontrolável em humanos e Zentradi. A Unidade Tática Musical Walküre usa ondas de canção (Fold Waves) protegidas pelo Esquadrão Delta (Delta Flight) para conter a epidemia contra o Reino de Windermere.
        - Unidade Tática Musical Walküre (Todas Femininas):
        - Reino de Windermere & Cavaleiros Aéreos (Aerial Knights):
        - Organizações e Termos: Chaos (corporação militar privada), NUNS (New United Nations Spacy), Walküre, Delta Flight, Aerial Knights, Windermere, Planeta Ragna, Var Syndrome (Síndrome Var), Fold Waves (Ondas Fold), Protocultura.
        - Mechas/Naves: VF-31 Siegfried, Sv-262 Draken III, Macross Elysion.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_delta"; }
    @Override public String getNomeExibicao() { return "Macross Delta (Série TV) - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.mapa();
    }
}
