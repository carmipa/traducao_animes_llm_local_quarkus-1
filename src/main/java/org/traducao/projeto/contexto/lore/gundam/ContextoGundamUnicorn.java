package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Gundam Unicorn / Unicorn RE:0096 (UC 0096) —
 * Laplace's Box, Unicorn/Banshee, Sleeves, Londo Bell/ECOAS.
 *
 * <p>INVARIANTES DO DOMÍNIO: Unicorn Gundam ≠ Gundam Unicórnio; Sleeves ≠ Mangas;
 * Laplace's Box; Psycho-Frame; NT-D; Full Frontal; Phenex é de NT (não misturar aqui).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamUnicorn implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn (OVA) / Mobile Suit Gundam Unicorn RE:0096 —
          Universal Century 0096.
        - Premissa: caca a Caixa de Laplace; Banagher Links e o RX-0 Unicorn Gundam;
          Earth Federation / Londo Bell / ECOAS vs Sleeves (Neo Zeon remnants) e Vist Foundation.

        === Nucleo UC ===
        - Newtype / Cyber-Newtype / Oldtype: oficiais; NUNCA "Novo Tipo".
        - Spacenoid vs Earthnoid; Minovsky particles; Psycho-Frame; NT-D; Destroy Mode /
          Unicorn Mode; psycommu; funnel; La+ (Laplace Program) quando o dialogo trouxer.
        - Mobile Suit vs Mobile Armor — distinguir rigorosamente.
        - Axis (mencoes historicas / Laplace) — NUNCA "Eixo".

        === Audrey e Mineva: a escolha de quem fala ===
        - Original "Audrey" -> saida "Audrey".   - Original "Mineva" -> saida "Mineva".
        - Sao a MESMA pessoa: Mineva Lao Zabi se apresenta como Audrey Burne. Mas a obra usa os
          dois nomes DE PROPOSITO, e a escolha diz quem esta falando e o que pensa dela.
        - O Banagher a chama de "Audrey" mesmo DEPOIS de saber quem ela e. Nao e ignorancia: e
          ele escolhendo enxergar a pessoa que ela quis ser, e nao a princesa Zabi. Trocar por
          "Mineva" ali apaga uma decisao do personagem.
        - Escreva o nome que o original escreveu. Nao resolva a identidade, nao "corrija" um
          nome pelo outro, e nao acrescente o que o original nao trouxe.

        === Protagonistas / Sleeves ===
        - Banagher Links (m); Mineva Lao Zabi, que se apresenta como Audrey Burne (f);
          Full Frontal (m — NUNCA "Frontal Completo"); Marida Cruz (f — Purge / "Sleeves");
          Suberoa Zinnerman (m); Angelo Sauper (m); Gilboa Sant (m); Tikva Sant (m) quando aparecer;
          Flaste Schole (m); Aaron Terzieff (m); Tomura (m) quando aparecer.

        === Federacao / Londo Bell / ECOAS / Nahel Argama ===
        - Riddhe Marcenas (m); Otto Midas (m); Daguza Mackle (m); Conroy Haagensen (m);
          Nigel Garrett (m); Hill Dawson (m); Liam Borrinea (f); Mihiro Oiwakken (f) quando aparecer;
          Bright Noa (m) / Ra Cailum quando a continuidade trouxer.

        === Vist / Anaheim / civis ===
        - Syam Vist (m); Cardeas Vist (m); Alberto Vist (m); Martha Vist Carbine (f);
          Takuya Irei (m); Micott Bartsch (f); Loni Garvey (f); Kai Shiden (m) cameo quando aparecer;
          Besson (m) quando aparecer.

        === Mecha ===
        - RX-0 Unicorn Gundam; Unicorn Gundam 02 Banshee / Banshee Norn;
          MSN-06S Sinanju; NZ-666 Kshatriya; MSN-001A1 Delta Plus;
          RGZ-95 ReZEL; RGM-89 Jegan; AMS-129 Geara Zulu; NZ-666 Kshatriya (Bustloff etc.);
          YAMS-132 Rozen Zulu; AMA-X7 Shamblo; Byarlant Custom; Stark Jegan; Anksha
          quando aparecerem.
        - Phenex (RX-0 Unicorn Gundam 03) NAO e unidade desta obra — aparece em NT.

        === Naves / lugares ===
        - Nahel Argama; Garencieres; Ra Cailum; Rewloola; Musaka; Magallanica;
          Industrial 7; Palau; Torrington Base; Dakar; Side colonies; Laplace Memorial.
        - Caixa de Laplace e Incidente de Laplace: TRADUZIR assim. Sao os dois unicos
          "Laplace" em portugues; Laplace Memorial e La+ (Laplace Program) ficam em ingles,
          por serem nome proprio de estacao e de sistema.

        === Orgs ===
        - Sleeves (NUNCA "Mangas"); Neo Zeon; Vist Foundation; Anaheim Electronics;
          Londo Bell; ECOAS; Earth Federation / Federation Forces.

        === Titulos de episodio (letreiro na tela) ===
        - Vem em CAIXA ALTA e sozinho, sem conversa em volta. E o caso em que menos
          contexto existe para apoiar a traducao, entao va pelo literal e nao invente.
        - "Red Comet" e o apelido do Char Aznable: mantenha "Red Comet". NUNCA
          aportuguesar por som ("Comesa", "Cometa do Vermelho").
        - "Side" com numero (Side 1..7) e colonia espacial e nao se traduz; "side"
          minusculo, sozinho, e "lado" mesmo. "Co-Prosperity Sphere" fica como esta.
        - Palavra que voce nao conhece em portugues NAO existe: "Departure" e "Partida",
          nunca "Departura". Na duvida entre inventar e manter o original, mantenha.

        === Regras ===
        - Unicorn Gundam NUNCA "Gundam Unicornio"; Full Frontal NUNCA "Frontal Completo";
          Sleeves NUNCA "Mangas"; Psycho-Frame NUNCA "moldura psicologica" generica.
        - Tom: politico-militar UC, melancolico, legado Char / Newtype / Laplace.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Unicorn", LORE);

    @Override
    public String getId() {
        return "gundam_unicorn";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam Unicorn";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco Unicorn / Sleeves / Londo Bell / Laplace.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais UC 0096; Phenex fora desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Banagher Links", "Mineva Lao Zabi", "Audrey Burne",
            "Full Frontal", "Marida Cruz", "Riddhe Marcenas",
            "Suberoa Zinnerman", "Otto Midas", "Daguza Mackle",
            "Martha Vist Carbine", "Cardeas Vist", "Alberto Vist",
            "Syam Vist", "Angelo Sauper", "Takuya Irei", "Micott Bartsch",
            "Loni Garvey", "Gilboa Sant", "Tikva Sant", "Flaste Schole",
            "Aaron Terzieff", "Conroy Haagensen", "Nigel Garrett",
            "Hill Dawson", "Liam Borrinea", "Mihiro Oiwakken", "Bright Noa",
            "Unicorn Gundam", "Banshee", "Banshee Norn",
            "Sinanju", "Kshatriya", "Delta Plus", "ReZEL", "Jegan",
            "Geara Zulu", "Rozen Zulu", "Shamblo", "Byarlant Custom",
            "Stark Jegan", "Anksha",
            "Nahel Argama", "Garencieres", "Ra Cailum", "Rewloola",
            "Musaka", "Magallanica",
            "Earth Federation", "Sleeves", "Sieg Zeon", "Neo Zeon", "Vist Foundation",
            "Anaheim Electronics", "Londo Bell", "ECOAS",
            // Apelido do Char. Entrou em 2026-07-29 porque o título do ep.5,
            // "CLASH WITH THE RED COMET", saiu como "CONFRONTO COM O COMESA DO VERMELHO".
            // Composto de duas palavras de propósito: "Comet" sozinho é substantivo comum.
            "Red Comet",
            // Título do ep.16; sem ele "CO-PROSPERITY SPHERE" virou "Esfera de Prosperidade CO".
            // NÃO se protege "Side" sozinho: a convenção do acervo é sempre com número
            // (Side 3/4/6/7 nas lores 0079, Origin, Thunderbolt, War in the Pocket), porque
            // "side" solto é "lado" e viraria letra maiúscula no meio de qualquer frase.
            "Co-Prosperity Sphere",
            // DECISÃO (Paulo, 2026-07-29): "Laplace's Box" e "Laplace Incident" SAÍRAM da
            // proteção — "Caixa de Laplace" e "Incidente de Laplace" soam naturais em PT-BR e
            // são a forma que o espectador reconhece. Continuam protegidos os que são nome
            // próprio de lugar/sistema: Laplace Memorial e La+ (Laplace Program).
            "Industrial 7", "Palau",
            "Torrington Base", "Dakar",
            "Newtype", "Cyber-Newtype", "Oldtype", "Spacenoid",
            "Earthnoid", "Minovsky", "Psycho-Frame",
            "NT-D", "Destroy Mode", "psycommu", "funnel",
            "Mobile Suit", "Mobile Armor", "Axis", "La+"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim Unicorn (Sleeves, Laplace, naves, modos).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN; canônicos dos extras
     * estão em {@link #termosProtegidos()}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Mangas", "Sleeves"),
            Map.entry("Manga", "Sleeves"),
            Map.entry("Moldura Psíquica", "Psycho-Frame"),
            Map.entry("Moldura Psiquica", "Psycho-Frame"),
            // As duas reversões saíram junto com a proteção: mantê-las faria o enforcer
            // desfazer a tradução que agora é a desejada, e o par forma-ruim->canônico só
            // existe para consertar erro, não para impor preferência já revogada.
            Map.entry("Fundação Vist", "Vist Foundation"),
            Map.entry("Fundacao Vist", "Vist Foundation"),
            Map.entry("Gundam Unicórnio", "Unicorn Gundam"),
            Map.entry("Gundam Unicornio", "Unicorn Gundam"),
            Map.entry("Eixo", "Axis"),
            Map.entry("Frontal Completo", "Full Frontal"),
            Map.entry("Modo Destruição", "Destroy Mode"),
            Map.entry("Modo Destruicao", "Destroy Mode"),

            // ---------------------------------------------------------------------------
            // Formas-ruim MEDIDAS nas 5.643 falas dos 22 episódios (cache de 2026-07-29).
            // termosProtegidos() é permissivo e não impede localização; quem GARANTE é o
            // EnforcadorTermosLore lendo este mapa. Cada entrada tem fala real por trás.
            // ---------------------------------------------------------------------------

            // "Unicorn Gundam" perdido em 10 de 45 — o mapa já cobria "Gundam Unicórnio",
            // mas não a INVERSÃO de ordem nem o híbrido.
            Map.entry("Gundam Unicorn", "Unicorn Gundam"),
            Map.entry("Unicórnio Gundam", "Unicorn Gundam"),
            Map.entry("Unicornio Gundam", "Unicorn Gundam"),

            // "Co-Prosperity Sphere" perdido em 9 de 10, em três formas distintas.
            Map.entry("Esfera de Co-Prosperidade", "Co-Prosperity Sphere"),
            Map.entry("Esfera de Prosperidade Comum", "Co-Prosperity Sphere"),
            Map.entry("Esfera de Prosperidade", "Co-Prosperity Sphere"),

            // A nave ganhou acento francês em 7 de 30.
            Map.entry("Garencières", "Garencieres"),

            // "Red Comet" perdido em 7 de 9 — é o codinome do Char, não descrição.
            Map.entry("Cometa Vermelho", "Red Comet"),

            // ÍMÃ entre colônias: "at the colony Industrial 7" saiu "Em Side 7".
            // Side 7 existe no UC, por isso a guarda importa: só troca quando o inglês
            // trazia "Industrial 7".
            Map.entry("Side 7", "Industrial 7"),

            // ÍMÃ entre unidades: "ECOAS can't investigate this by ourselves" saiu
            // "Londo Bell não pode investigar isso sozinhos" — 2 de 13.
            Map.entry("Londo Bell", "ECOAS"),

            // Base com ordem invertida e preposição, 4 de 5.
            Map.entry("Base Torrington", "Torrington Base"),
            Map.entry("base de Torrington", "Torrington Base"),

            // Ordem invertida em 3 de 7, mesmo defeito medido no Char's Counterattack.
            Map.entry("Zeon Sieg", "Sieg Zeon"),
            Map.entry("Siege Zeon", "Sieg Zeon"),

            // Aportuguesamento do termo técnico, 4 de 8.
            // Só a caixa BAIXA. A legenda também escreve "A Psycommu runaway...", mas o
            // canônico declarado em termosProtegidos é "psycommu" minúsculo, e o invariante
            // CorrecoesTerminologiaGundamUcTest exige que todo canônico do mapa esteja
            // declarado. Declarar "Psycommu" mudaria o hash do manifesto e invalidaria as
            // 5.643 falas já traduzidas — caro demais por 2 ocorrências. Fica para quando
            // houver outra mudança de lore desta obra para pagar a invalidação junto.
            Map.entry("psicommu", "psycommu"),

            // "Spacenoid" perdido em 5 de 7; o núcleo UC cobre "Espacenoide" com N, não a
            // forma sem ele. O plural fica de fora pelo mesmo motivo do Psycommu acima:
            // "Spacenoids" não está declarado.
            Map.entry("Espacoide", "Spacenoid"),

            // Feminino de Newtype — mesma lacuna encontrada no F91: o núcleo UC só traz
            // "Novo Tipo"/"Neotipo" no masculino.
            Map.entry("Nova Tipo", "Newtype")

            // NÃO entram, e a medição é a razão:
            //   Earth Federation — "Federação Terrestre" em 20 de 20, consistente com todo
            //     o acervo (CCA e F91 idem). É decisão de produto, não defeito.
            //   Mobile Suit — 17 de 73 são plural ("mobile suits") ou omissão da fala
            //     inteira; a checagem exige o singular exato.
            //   Neo Zeon (6/61), Londo Bell (2/22), Nahel Argama (1/60) — taxa baixa e as
            //     falas mostram pronome/omissão, não localização do termo.
        ));
    }
}
