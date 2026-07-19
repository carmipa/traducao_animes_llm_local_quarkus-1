package org.traducao.projeto.contexto.lore.gundam.msteam;

import java.util.Map;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de The 08th MS Team (OVA UC 0079 — guerra terrestre).
 *
 * <p>INVARIANTES DO DOMÍNIO: Shiro Amada; Aina Sahalin; Ez-8; Apsalus; Eledore;
 * realismo anti-guerra.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundam08thMSTeam implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The 08th MS Team (機動戦士ガンダム 第08MS小隊 / Kidou Senshi Gundam Dai 08 MS Shoutai). OVA de 1996-1999, Universal Century. Uma das histórias mais realistas e aclamadas da franquia Gundam.
        - Período: U.C. 0079, durante a Guerra de Um Ano. A narrativa ocorre paralelamente aos eventos do primeiro Mobile Suit Gundam, mas foca exclusivamente na guerra terrestre, na selva, vista pelos soldados comuns.
        - Local principal: Frente de selva no Sudeste Asiático. Ambiente úmido, quente, hostil: vegetação densa, rios, montanhas, vilarejos destruídos, bases improvisadas, hospitais de campanha e trilhas de suprimento precárias.
        - Tom da obra: Realismo militar cru, anti-guerra. Mostra o medo, a exaustão, a fome, doenças tropicais, trauma psicológico, dilemas morais e o absurdo da guerra. Pouca glorificação de combates.
        - Conflito central: A Federação Terrestre tenta expulsar Zeon da Terra. Zeon mantém posições defensivas e desenvolve armas secretas (Projeto Apsalus). No centro da história está o romance impossível entre Shiro Amada e Aina Sahalin.

        === Personagens Detalhados ===
        - Shiro Amada (homem): Segundo-tenente, comandante da 08th MS Team. Idealista, teimoso, corajoso e com forte senso de moralidade. Preocupa-se genuinamente com seus subordinados e civis. Questiona ordens quando as considera erradas. Fala com convicção, honestidade e empatia. Evite torná-lo ingênuo ou cínico.
        - Aina Sahalin (mulher): Piloto de teste do Apsalus e oficial de Zeon. Nobre, serena, educada e emocionalmente contida. Leal à família, mas cada vez mais horrorizada com a crueldade da guerra e a loucura do irmão. Fala de forma elegante e refinada.
        - Karen Joshua (mulher): Sargento-veterana, piloto e médica de campo. Forte personalidade, prática, sarcástica, durona. Inicialmente cética em relação a Shiro. Muito competente e protetora da equipe.
        - Terry Sanders Jr. (homem): Sargento conhecido como "Shinigami Sanders". Carregado por culpa de sobrevivente. Reservado, fatalista e supersticioso.
        - Eledore Massis/Mathis (homem): Cabo responsável por radar, sonar e comunicações no hovertruck. Reclamão, humorístico, medroso, mas leal. Tem talento musical e sonha com dias melhores.
        - Michel Ninorich (homem): Cabo jovem, navegador e artilheiro do hovertruck. Inexperiente, ansioso, romântico. Mantém contato com a namorada B.B. através de cartas.
        - Kiki Rosita (mulher): Jovem da vila local. Impulsiva, esperta, corajosa e conhecedora da selva. Serve como guia e intermediária. Desenvolve sentimentos por Shiro.
        - Ginias Sahalin (homem): Irmão de Aina, líder do Projeto Apsalus. Gênio brilhante, mas paranoico, doente e cada vez mais megalomaníaco e cruel.
        - Norris Packard (homem): Coronel e ace de Zeon. Piloto do Gouf Custom. Honrado, calmo, leal e paternal com Aina. Representa o melhor lado militar de Zeon.
        - Outros: Kojima (comandante do batalhão), Isan Ryer (oficial frio e calculista), Alice Miller (oficial de inteligência), Yuri Kellarny, Baresto Rosita (líder guerrilheiro).

        === Unidades Militares e Tecnologia ===
        - 08th MS Team: Pequena unidade de Mobile Suits terrestres da Federação especializada em operações na selva.
        - Mobile Suits da Earth Federation/Federação Terrestre: RX-79[G] Ground Gundam/Gundam Ground Type, RX-79[G] Ez-8 Gundam Ez8 (versão reparada e melhorada de Shiro), RGM-79[G] GM Ground Type.
        - Mobile Suits de Zeon: MS-06J Zaku II Ground Type, MS-07B-3 Gouf Custom, Gouf Flight Type, Zaku Tank, Magella Attack, Apsalus I, Apsalus II e Apsalus III.
        - Apsalus: Mobile Armor experimental gigantesco com sistema Minovsky Craft para voo. Armado com mega particle cannon. Principal arma secreta de Zeon na região.
        - Veículos: Hovertruck de suporte tático (Eledore e Michel).

        === Temas Recorrentes ===
        - Custo humano da guerra (mortes de companheiros, civis presos no fogo cruzado, trauma).
        - Questionamento de ordens superiores e autoridade militar.
        - Romance entre inimigos e o conflito entre dever e sentimentos.
        - Diferença entre a guerra idealizada pelos oficiais e a realidade vivida pelos soldados na linha de frente.
        - Sobrevivência vs ideologia (principalmente entre civis e guerrilheiros locais).

        === Regras Específicas de Tradução ===
        - Manter sempre em inglês ou forma oficial: mobile suit, mobile armor, Earth Federation, Principality of Zeon, Gundam Ez8, Ground Gundam/Gundam Ground Type, Gouf Custom, Apsalus, Miller's Report, One Year War, Universal Century, U.C., Newtype (NUNCA "Novo Tipo"), Oldtype, Spacenoid, Earthnoid, Minovsky particles, Mega Particle Cannon, beam rifle, beam saber, mega particle cannon, Jaburo.
        - Apsalus e Mobile Armor (nao Mobile Suit). Ground Gundam / Ez-8 / GM Ground Type sao Mobile Suits.
        - Comunicação por rádio: curta, clara, técnica e militar ("Alvo avistado", "Contato inimigo à frente", "Danos no lado direito", "Recuar para ponto de extração", "Munição em 30%").
        - Tom das legendas: maduro, sóbrio, imersivo e emocional quando necessário. Evitar gírias brasileiras modernas ("mano", "tá ligado", "crush", etc.).
        - Gênero: Manter rigorosamente a concordância (Shiro, Ginias, Norris, Sanders, Eledore, Michel, Kojima, Ryer = masculino | Aina, Karen, Kiki, Alice, B.B. = feminino).

        - Estilo desejado: legendas de alta qualidade, dignas de uma obra clássica de Gundam. Naturalidade aliada a peso dramático quando a cena pedir.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam: The 08th MS Team", LORE);

    @Override
    public String getId() {
        return "gundam_08ms";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam: The 08th MS Team";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes e unidades da 08th MS Team.
     * <p>INVARIANTES DO DOMÍNIO: Eledore/Ez-8/Apsalus canônicos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shiro Amada", "Aina Sahalin", "Karen Joshua",
            "Terry Sanders Jr.", "Eledore Massis", "Michel Ninorich",
            "Kiki Rosita", "Ginias Sahalin", "Norris Packard",
            "Kojima", "Gundam Ez8", "Ground Gundam",
            "Gundam Ground Type", "GM Ground Type", "Zaku II Ground Type",
            "Gouf Custom", "Apsalus", "Earth Federation",
            "Principality of Zeon", "Kojima Battalion", "08th MS Team",
            "Jaburo", "One Year War", "Newtype",
            "Oldtype", "Spacenoid", "Earthnoid",
            "Minovsky", "Mega Particle Cannon", "Mobile Suit",
            "Mobile Armor"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico do núcleo UC (Newtype, Mobile Suit, Beam
     * Saber/Rifle, Mobile Armor, Oldtype) mais os termos próprios desta obra.
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Gouf Personalizado", "Gouf Custom"),
            Map.entry("Gouf Customizado", "Gouf Custom")
        ));
    }
}
