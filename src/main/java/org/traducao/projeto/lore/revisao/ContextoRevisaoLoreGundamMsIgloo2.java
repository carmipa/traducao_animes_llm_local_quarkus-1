package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam MS IGLOO 2: Gravity Front.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamMsIgloo2}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreGundamMsIgloo2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam MS IGLOO 2: Gravity Front (OVA, U.C. 0079).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Earth Federation, 44th Hybrid Regiment, 301st Tank Squadron, Principality of Zeon, RTX-440 Ground Assault Type Guntank, Type 61 Tank, Zaku II, Dabude, Mobile Suit, One Year War, Odessa.
        - Personagens: Ben Barberry, Papa Sidney Lewis, Michael Colmatta, Harman Yandell, Rayban Surat, Arleen Nazon, Clyde Bettany, Milos Karppi, Doroba Kuzwayo, Elmer Snell/White Ogre, Death Deity, Kycilia Zabi.
        - Alertas: nomes proprios e designacoes de mecha em ingles/romanizados;
          Elmer Snell/White Ogre e uma unica entrada; RTX-440 / Dabude / Type 61 grafias oficiais.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_ms_igloo_2"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam MS IGLOO 2: Gravity Front - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa deterministico UC na Opcao 7 (nucleo sem extras).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho exato do lado da Traducao desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.mapa();
    }
}
