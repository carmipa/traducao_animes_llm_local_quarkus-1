package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore geral de DanMachi (contexto padrão) cobrindo termos
 * de mundo, nomes principais e regras de tradução para qualquer arco.
 *
 * <p>INVARIANTES DO DOMÍNIO: Liliruca Arde; Syr Flova; Mikoto Yamato; Familia/Falna/Dungeon.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachi implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? / DanMachi.
        - Local central: Orario, cidade-labirinto erguida sobre a Dungeon.
        - Mantenha "Dungeon" como Dungeon quando for o labirinto especifico; traduza "dungeon" generico como masmorra se o contexto pedir.
        - "Familia" e o grupo ligado a uma divindade; use Familia sem acento para o termo de lore, como em Hestia Familia, Loki Familia e Freya Familia.
        - "Falna" e a bencao/status concedida por deuses; nao traduza como simples habilidade.
        - "Status", "Level", "Skill", "Magic", "Development Ability", "Excelia" e "Adventurer" devem ser tratados como termos de RPG do mundo; em dialogo natural, use status, nivel, habilidade, magia, habilidade de desenvolvimento, excelia e aventureiro.
        - Principais nomes: Bell Cranel, Hestia, Ais Wallenstein, Liliruca Arde (apelido "Lili"), Welf Crozzo, Mikoto Yamato, Haruhime Sanjouno, Ryu Lion, Syr Flova, Freya, Ottar.
        - Lugares e grupos: Babel, Guilda, Hostess of Fertility, Rua Daedalus, Xenos, Hermes Familia, Apollo Familia, Ishtar Familia.
        - Armas e termos: Hestia Knife, Argonaut, Firebolt, Crozzo Magic Sword, Minotaur, Black Goliath, War Game.
        - Epitetos recorrentes: Ais Wallenstein e conhecida como "Sword Princess" (Princesa Espadachim); Bell e chamado de "Little Rookie" (Pequeno Novato) por outros aventureiros; Ottar e tratado como o aventureiro mais forte de Orario. Mantenha o epiteto e traduza-o de forma consistente sempre que reaparecer.
        - Bell se refere a Hestia como "Goddess" (Deusa), com mistura de respeito e carinho; preserve esse tratamento em vez de reduzir so ao nome proprio.
        - Tom: aventura/fantasia com humor leve, romance timido e momentos heroicos; Bell fala de modo sincero e respeitoso, Hestia alterna carinho e ciume, Freya soa sedutora e dominante.
        - Genero dos personagens principais (concordancia obrigatoria): Bell Cranel (m), Hestia (f), Ais Wallenstein (f), Liliruca/Lili (f), Welf Crozzo (m), Mikoto Yamato (f), Haruhime Sanjouno (f), Ryu Lion (f), Syr Flova (f), Freya (f), Ottar (m).
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi", LORE);

    @Override
    public String getId() {
        return "danmachi";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Geral)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege o núcleo canônico da lore geral DanMachi.
     * <p>INVARIANTES DO DOMÍNIO: grafias alinhadas às temporadas enriquecidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Ais Wallenstein", "Liliruca Arde", "Lili",
            "Welf Crozzo", "Mikoto Yamato", "Haruhime Sanjouno", "Ryuu Lion",
            "Syr Flova", "Freya", "Ottar", "Orario", "Dungeon", "Falna", "Familia",
            "Hestia Knife", "Firebolt", "Argonaut"
        );
    }
}
