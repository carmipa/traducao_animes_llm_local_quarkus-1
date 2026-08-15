package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam (0079).
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundam0079}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreGundam0079 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam (0079) — One Year War / Universal Century 0079.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Earth Federation, Principality of Zeon, White Base, One Year War, Minovsky Particles, Mega Particle Cannon, Minovsky Ultracompact Fusion Reactor, Mobile Suit, Mobile Armor, Newtype, Cyber-Newtype, Oldtype, Spacenoid, Earthnoid, RX-78-2 Gundam, RX-75 Guntank, RX-77 Guncannon, MS-06 Zaku II, MS-07 Gouf, MS-09 Dom, MSN-02 Zeong, MAN-08 Elmeth, Side 7, Loum, Solomon, A Baoa Qu, Jaburo.
        - Personagens: Amuro Ray, Char Aznable/Casval Rem Deikun, Bright Noa, Sayla Mass/Artesia Som Deikun, Lalah Sune, Kai Shiden, Hayato Kobayashi, Ryu Jose, Sleggar Law, Mirai Yashima, Fraw Bow, Degwin Sodo Zabi, Gihren Zabi, Kycilia Zabi, Dozle Zabi, Garma Zabi, Ramba Ral, M'Quve, Crowley Hamon.
        - Alertas: White Base / Gundam / Zaku / Zeon / Federation oficiais;
          Newtype NUNCA vira "Novo Tipo"; Mobile Suit e Mobile Armor nao trocam de rotulo;
          Base Branca → White Base.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_0079"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam (Série original / Filme Trilogy) - Revisao de Lore"; }
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
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Base Branca", "White Base")
        ));
    }
}
