package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam Thunderbolt: Bandit Flower.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamThunderboltBandit}) — reestruturacao de formato, nao
 * pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala,
 * entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>O mapa de terminologia espelha EXATAMENTE o do lado da Traducao. Divergir poe a obra em
 * {@code DIVERGENCIAS_DECLARADAS} do {@code ParidadeMapasTerminologiaTest} — divida que so se
 * assume com motivo escrito, uma obra por vez.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutaveis.
 */
@Component
public class ContextoRevisaoLoreGundamThunderboltBandit implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Thunderbolt: Bandit Flower (filme, pos One Year War / U.C. 0080).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Earth Federation, Spartan, South Seas Alliance, Principality of Zeon, Zeon remnants, Republic of Zeon, Atlas Gundam, Guncannon Aqua, Psycho Zaku, Reuse P. Device, Acguy, Gogg, Grublo, Gouf, GM, Mobile Suit, One Year War, Antarctica, Newtype.
        - Personagens: Io Fleming, Daryl Lorenz, Claudia Peer, Cornelius KaKa, Bianca Carlyle, Karla Mitchum, Vincent Pike, Monica Humphrey, Levan Fuu, Chow Ming, Bull.
        - Alertas: nomes proprios e designacoes de mecha em ingles/romanizados;
          Atlas Gundam / South Seas Alliance / Spartan / Reuse P. Device grafias oficiais.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_thunderbolt_bandit"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam Thunderbolt: Bandit Flower - Revisao de Lore"; }
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
