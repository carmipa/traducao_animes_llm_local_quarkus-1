package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa do Filme 1 de Break Blade / Broken Blade —
 * The Time of Awakening (O Tempo do Despertar): retorno de Rygart, invasão e
 * despertar do Under-Golem Delphine.
 *
 * <p>INVARIANTES DO DOMÍNIO: continuidade dos 6 filmes teatrais (não misturar com
 * o mangá completo); NÃO adiantar morte de Lee, captura de Cleo, Borcuse ou Hykelion
 * (filmes 2–6).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoBreakBlade1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 1: The Time of Awakening
          (ブレイク ブレイド 覚醒ノ刻 / Kakusei no Toki) — PT: O Tempo do Despertar.
        - Continuidade: 1º dos 6 filmes teatrais (Production I.G / Xebec). NÃO misturar
          com o mangá pós-filme 6 nem com a edição TV de 12 episódios como ids separados.
        - Foco deste filme: Assam caiu sob a Athens Commonwealth; Rygart Arrow (un-sorcerer)
          é chamado a Binonten por Hodr e Sigyn; descobre o Under-Golem ancestral; Zess
          lidera a Valkyrie Squadron contra a capital. NÃO cobrir cessar-fogo/Lee (filme 2).

        === Mundo / facções ===
        - Continente Cruzon: quase todos manipulam Quartz (energia/mágica tecnológica).
        - Kingdom of Krisna (capital Binonten) vs Athens Commonwealth; Assam Military Academy
          no passado dos quatro amigos; Orlando Empire no pano de fundo geopolítico.
        - Golems = mechas movidos a Quartz; Under-Golem = máquina ancestral dos Ancients.

        === Personagens (gênero) ===
        - Rygart Arrow (m) — un-sorcerer; único a ativar o Under-Golem (Delphine).
        - Hodr (m) — rei de Krisna; amigo da Assam Military Academy.
        - Sigyn Erster (f) — rainha / engenheira; esposa de Hodr; amiga de Rygart.
        - Zess (m) — oficial de Athens; irmão de Loquis; líder da Valkyrie Squadron;
          amigo de academia agora no lado inimigo.
        - Cleo Saburafu (f), Lee (f), Argath (m), Erekt (m) — Valkyrie Squadron (Athens).
        - General Baldr (m) — comando de Krisna; presença inicial no capital.
        - Loquis (m) — Secretary of War de Athens (irmão de Zess; Ilios).

        === Mecha / termos ===
        - Delphine (Under-Golem; também grafado Delphing em alguns materiais EN —
          preferir Delphine), Golem, Quartz, un-sorcerer, Heavy Knight (ainda em formação),
          Valkyrie Squadron (unidade de Zess — NÃO é Macross Valkyrie / Variable Fighter).
        - PROIBIDO traduzir Golem→"robô" genérico; manter Quartz/un-sorcerer/Under-Golem.

        === Regras ===
        - Títulos oficiais: Break Blade (JP/comum) = Broken Blade (EN Sentai).
        - Nomes: Rygart Arrow, Sigyn Erster, Cleo Saburafu, Kingdom of Krisna (não "Krishna"
          como forma preferida), Athens Commonwealth, Binonten, Cruzon, Assam.
        - Tom: despertar político e pessoal; amizade vs guerra; Rygart relutante.

        === Formas-ruim típicas (enforcer) ===
        - Não-feiticeiro/Sem-magia → un-sorcerer; Quartzo → Quartz; Delfine/Delphing → Delphine;
          Reino de Krisna/Krishna → Kingdom of Krisna; Esquadrão Valquíria → Valkyrie Squadron.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Break Blade - Filme 1 - O Tempo do Despertar", LORE);

    @Override
    public String getId() {
        return "break_blade_1";
    }

    @Override
    public String getNomeExibicao() {
        return "Break Blade - Filme 1 - O Tempo do Despertar";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do Filme 1 (despertar / Delphine).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem Borcuse/Hykelion/Girge como foco (filmes posteriores).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Rygart Arrow", "Hodr", "Sigyn Erster", "Zess",
            "Cleo Saburafu", "Lee", "Argath", "Erekt",
            "General Baldr", "Loquis", "Delphine", "Under-Golem",
            "Golem", "Golems", "Quartz", "un-sorcerer",
            "Kingdom of Krisna", "Athens Commonwealth", "Orlando Empire",
            "Valkyrie Squadron", "Binonten", "Cruzon", "Assam",
            "Assam Military Academy", "Ilios", "Broken Blade", "Break Blade",
            "The Time of Awakening", "Heavy Knight", "Ancients"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Break Blade (franquia dos 6 filmes).
     *
     * <p>INVARIANTES DO DOMÍNIO: {@link CorrecoesTerminologiaBreakBlade}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaBreakBlade.mapa();
    }
}
