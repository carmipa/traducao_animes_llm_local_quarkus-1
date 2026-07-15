package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

import java.util.Set;

@Component
public class ContextoGundamNT implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam NT (Narrative) / Kidou Senshi Gundam NT. Filme de 2018 do Universal Century, continuação temática de Mobile Suit Gundam Unicorn.
        - Período: U.C. 0097, um ano depois do Incidente de Laplace. A revelação da Carta original do Universal Century tornou pública a existência dos Newtypes, mas a política e a guerra continuam presas aos mesmos interesses.
        - Conflito central: O retorno do RX-0 Unicorn Gundam 03 Phenex dispara a Operation Phoenix Hunt. A Earth Federation, Luio & Co., a Republic of Zeon e remanescentes ligados aos Sleeves disputam a captura do Phenex.
        - Tom da obra: drama sci-fi militar e espiritual, melancólico, pesado e filosófico. O filme mistura conspiração política, trauma infantil, culpa, experimentos Newtype, destino, morte e a ideia de que o psycho-frame toca algo além da vida.

        === Personagens Detalhados ===
        - Jona Basta (homem): Piloto da Earth Federation e do RX-9 Narrative Gundam. Um dos "Miracle Children". Carrega culpa por Rita e ressentimento por Michele. Fala de modo direto, contido e emocionalmente ferido. Evite torná-lo heroico demais; ele é hesitante, marcado por trauma e busca redenção.
        - Michele Luio / Michelle Luio (mulher): Executiva de Luio & Co. e uma das "Miracle Children". Inteligente, manipuladora, elegante e movida por culpa. Usa frieza corporativa e cálculo político para esconder desespero emocional. Preferir "Michele Luio" como grafia canônica, mas reconhecer "Michelle" se vier na legenda.
        - Rita Bernal (mulher): Verdadeira Newtype entre os três "Miracle Children". Ligada ao Phenex e à dimensão espiritual do psycho-frame. Figura etérea, compassiva e trágica. Quando aparecer como memória/voz, manter tom delicado, distante e quase sagrado.
        - Zoltan Akkanen (homem): Piloto da Republic of Zeon, Cyber Newtype fracassado associado ao projeto de recriar Char Aznable. Instável, cruel, teatral e ressentido. Pilota o Sinanju Stein e depois o II Neo Zeong. Fala com sarcasmo, raiva e grandiosidade doentia.
        - Iago Haakana (homem): Comandante da Shezarr Unit. Militar competente e prático da Earth Federation. Fala com autoridade profissional, sem exagero emocional.
        - Brick Teclato (homem): Assistente de Michele em Luio & Co. Educado, discreto e leal. Tom corporativo, formal e contido.
        - Mineva Lao Zabi (mulher): Figura política de Zeon vista em conexão com os eventos de Unicorn. Serena, nobre e cautelosa.
        - Banagher Links (homem): Piloto ligado ao Unicorn Gundam, aparece como ponte com Gundam Unicorn. Fala de modo calmo, empático e esperançoso.
        - Monaghan Bakharo (homem): Ministro da Republic of Zeon. Político calculista, interessado em manipular os eventos ligados ao Phenex.

        === Organizações, Facções e Forças ===
        - Earth Federation / Earth Federation Forces: Manter em inglês quando aparecer como nome oficial; "Federação Terrestre" pode ser usado em fala natural quando o original for genérico.
        - Shezarr Unit: Unidade da Earth Federation envolvida na caça ao Phenex.
        - Luio & Co.: Corporação poderosa que apoia Michele e opera por interesses próprios. Manter exatamente assim.
        - Republic of Zeon: Remanescente político de Zeon. Manter em inglês quando aparecer como nome oficial.
        - Sleeves: Remanescentes Neo Zeon. Manter "Sleeves" como nome de facção.
        - Titans / Newtype Labs: Referências ao passado de experimentos com Cyber Newtypes e trauma dos Miracle Children.

        === Mobile Suits, Mobile Armors e Tecnologia ===
        - RX-9 Narrative Gundam: Mobile suit principal de Jona. Pode aparecer com A-Packs, B-Packs e C-Packs; manter esses nomes em inglês.
        - RX-0 Unicorn Gundam 03 Phenex / Phenex: Mobile suit dourado de psycho-frame, ligado a Rita. Não traduzir "Phenex" para "Fênix" quando for o nome da unidade.
        - RX-0 Unicorn Gundam e RX-0 Unicorn Gundam 02 Banshee: Referências de Gundam Unicorn.
        - MSN-06S-2 Sinanju Stein: Mobile suit de Zoltan. Manter "Sinanju Stein".
        - NZ-999 II Neo Zeong: Mobile armor gigantesco usado por Zoltan. Manter "II Neo Zeong"; não traduzir "II" como "segundo" em nome técnico.
        - ARX-014S Silver Bullet Suppressor: Unidade associada a Banagher. Manter nome oficial.
        - Psycho-Frame: Tecnologia central, ligada a Newtypes, consciência, ressonância e fenômenos quase espirituais. Manter "psycho-frame" ou "Psycho-Frame" conforme o original.
        - NT-D, Newtype, Cyber Newtype, Minovsky particles, beam rifle, beam saber, funnels, psychowaves: Manter em inglês/forma oficial quando forem termos técnicos.

        === Temas Recorrentes ===
        - Culpa e redenção de Jona e Michele em relação a Rita.
        - Newtypes como possibilidade humana, mas explorados por política, guerra e experimentos.
        - O psycho-frame como fronteira entre tecnologia, alma, memória e morte.
        - Continuidade direta do legado de Gundam Unicorn: Laplace Incident, Unicorn, Banshee, Mineva e Banagher.
        - Conspiração política: Federation, Republic of Zeon, Luio & Co. e remanescentes Neo Zeon tentando controlar a narrativa pública.

        === Regras Específicas de Tradução ===
        - Manter sempre em inglês ou forma oficial: Mobile Suit Gundam NT, Narrative Gundam, Phenex, Unicorn Gundam, Banshee, Sinanju Stein, II Neo Zeong, Silver Bullet Suppressor, Psycho-Frame, Newtype, Cyber Newtype, Operation Phoenix Hunt, Laplace Incident, Universal Century, U.C., Earth Federation, Republic of Zeon, Luio & Co., Shezarr Unit, Sleeves, Minovsky particles, beam rifle, beam saber.
        - "Narrative" e "Narrative Gundam" sao nomes oficiais; nunca traduzir como "Narrativo" ou "Gundam Narrativo".
        - "Operation Phoenix Hunt" pode ser explicado em português apenas se o original já soar como nome de operação narrado; como nome oficial, manter em inglês.
        - Não traduzir "Phenex" como "Fênix" quando se referir ao RX-0 Unicorn Gundam 03 Phenex.
        - Comunicação militar: curta, objetiva e técnica ("Alvo confirmado", "Contato com o Phenex", "Retomar formação", "Unidade inimiga se aproximando", "Danos no frame").
        - Falas filosóficas/Newtype: manter tom sério e poético, sem soar místico barato ou explicativo demais.
        - Evitar gírias brasileiras modernas e humor fora de contexto. A linguagem deve ser adulta, sóbria e cinematográfica.
        - Gênero: Jona, Zoltan, Iago, Brick, Banagher, Monaghan = masculino | Michele/Michelle, Rita, Mineva, Ellic = feminino.

        - Estilo desejado: legendas PT-BR naturais e dramáticas, preservando a terminologia oficial de Gundam e a continuidade com Gundam Unicorn.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam NT (Narrative)", LORE);

    @Override
    public String getId() {
        return "gundam_nt";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam NT (Narrative)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece identificadores oficiais que podem
     * permanecer iguais ao original sem serem tratados como falha de tradução.
     * <p>INVARIANTES DO DOMÍNIO: a lista não integra o prompt nem altera o hash
     * da lore/cache; contém somente nomes confirmados no artefato da obra.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o detector volta a tratar o conteúdo
     * desconhecido como pendência, sem apagar ou traduzir automaticamente.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of("Banchi 18", "Metis", "Fransson");
    }
}
