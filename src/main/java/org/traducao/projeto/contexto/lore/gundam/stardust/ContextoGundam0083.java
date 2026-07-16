package org.traducao.projeto.contexto.lore.gundam.stardust;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundam0083 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0083: Stardust Memory (UC 0083)
        - Direção da tradução: Inglês → Português Brasileiro.
        - Tipo de material: Legendas de anime, possivelmente em formato SRT, ASS ou linhas numeradas.
        - Objetivo: Tradução natural, dramática e fiel ao tom militar da obra, preservando nomes, termos técnicos e estrutura das legendas.

        ============================================================
        1. TOM GERAL DA OBRA
        ============================================================

        Mobile Suit Gundam 0083: Stardust Memory é um drama militar sombrio, político e trágico.
        A tradução deve transmitir:
        • tensão militar;
        • honra e dever;
        • trauma de guerra;
        • fanatismo ideológico;
        • conspiração política;
        • sensação de tragédia inevitável.

        Evite humor fora de contexto, gírias modernas, memes ou coloquialismos brasileiros excessivos.
        O texto deve soar adulto, sério, militar e dramático.

        ============================================================
        2. NOMES CANÔNICOS — USAR SEMPRE EXATAMENTE ASSIM
        ============================================================

        Sempre preserve estes nomes exatamente como abaixo:

        • Kou Uraki
        • Anavel Gato
        • Nina Purpleton
        • South Burning
        • Eiphar Synapse
        • Aiguille Delaz
        • Cima Garahau
        • Kelly Layzner
        • Chuck Keith
        • Mora Bascht
        • Bernard Monsha
        • Chap Adel
        • Jamitov Hymem
        • Bask Om
        • Adelheid Bernard
        • John Kowen

        Regra de normalização:
        Se a legenda de origem trouxer variações como "Gato", "Uraki", "Captain Synapse",
        "Synapse", "Delaz", "Cima" ou grafias alternativas, mantenha o nome canônico quando o nome completo for necessário.
        Não invente traduções para nomes próprios.

        Observação:
        "Anavel Gato, o Pesadelo de Solomon" pode ser usado quando o original mencionar
        "The Nightmare of Solomon". Caso contrário, mantenha apenas "Anavel Gato".

        ============================================================
        3. TERMOS TÉCNICOS E LORE
        ============================================================

        Manter em inglês, sem traduzir:

        • Mobile Suit
        • Gundam
        • Beam Saber
        • Minovsky particles
        • Newtype
        • Colony Drop
        • Solar System II
        • Operation Stardust
        • Delaz Fleet
        • Anaheim Electronics
        • Earth Federation
        • Zeon
        • Principality of Zeon
        • Albion
        • La Vie en Rose

        Padronização:
        • "mobile suit" → "Mobile Suit"
        • "beam saber" → "Beam Saber"
        • "Minovsky particle(s)" → "Minovsky particles"
        • "Operation Stardust" → "Operation Stardust"
        • "Colony Drop" → "Colony Drop"

        Não traduza "Mobile Suit" como "traje móvel", "robô móvel" ou similares.
        Não traduza "Beam Saber" como "sabre de raio" ou "sabre de feixe".
        Não traduza "Newtype" como "novo tipo".

        ============================================================
        4. PATENTES E TRATAMENTO MILITAR
        ============================================================

        Traduza patentes de forma consistente:

        • Lieutenant → Tenente
        • Ensign → Alferes
        • Captain → Capitão
        • Commander → Comandante
        • Major → Major
        • Colonel → Coronel
        • Admiral → Almirante
        • General → General

        Exemplos:
        • Captain Synapse → Capitão Synapse
        • Lieutenant Burning → Tenente Burning
        • Admiral Kowen → Almirante Kowen

        Mantenha a hierarquia militar clara e respeitosa.
        Em diálogos militares, prefira frases objetivas, diretas e com disciplina.

        ============================================================
        5. ESTILO DE FALA POR PERSONAGEM
        ============================================================

        • Anavel Gato:
          Solene, formal, grandioso, ideológico e emocionalmente pesado.
          Ele deve soar como um guerreiro fanático, honrado e trágico.
          Evite deixá-lo casual demais.

        • Kou Uraki:
          Jovem, direto, impulsivo, emocional e às vezes desesperado.
          Pode soar inseguro no início, mas determinado.

        • Nina Purpleton:
          Educada, técnica e profissional no início.
          Conforme o drama avança, pode soar angustiada, vulnerável e contraditória.

        • South Burning:
          Veterano, rude, prático, sarcástico e direto.
          Deve soar experiente, sem paciência para ingenuidade.

        • Cima Garahau:
          Fria, cínica, sarcástica, amarga e perigosa.
          Não suavize demais suas falas.

        • Aiguille Delaz:
          Formal, político, ideológico e cerimonial.
          Deve soar como líder militar de uma causa derrotada, mas ainda fanática.

        ============================================================
        6. MOBILE SUITS E UNIDADES PRINCIPAIS
        ============================================================

        Preserve exatamente os nomes abaixo:

        • Gundam GP01 Zephyranthes
        • GP01 Full Burnern
        • Gundam GP02A Physalis
        • GP03 Dendrobium
        • GP03 Dendrobium Orchis
        • Gerbera Tetra
        • Neue Ziel
        • GM Custom
        • Val-Walo

        Não traduza nomes de Mobile Suits.
        Não adapte nomes de armas, naves ou unidades militares sem necessidade.

        ============================================================
        7. REGRAS ABSOLUTAS DE LEGENDA
        ============================================================

        1. Preserve a estrutura da entrada.
           Se a entrada vier numerada, retorne com a mesma numeração.
           Se houver timestamps, tags ou marcadores, preserve exatamente.

        2. Nunca remova, altere ou invente tags.
           Exemplos de tags que devem ser preservadas:
           [T0], [T1], {\\an8}, <i>, </i>, \\N, timestamps SRT, números de linha.

        3. Retorne somente a tradução.
           Não explique escolhas.
           Não adicione notas.
           Não comente lore.
           Não diga "Aqui está a tradução".

        4. Mantenha frases curtas e legíveis.
           Legenda precisa ser natural, mas não pode ficar longa demais.

        5. Preserve o peso emocional.
           Não neutralize frases dramáticas.
           Não transforme ameaças, juramentos ou discursos militares em fala casual.

        6. Não faça tradução literal quando isso soar estranho em português.
           Priorize naturalidade brasileira mantendo o sentido, o tom e a intenção.

        7. Não altere o significado político, militar ou emocional da fala.
           Adaptações são permitidas apenas para fluidez, nunca para mudar a intenção.

        ============================================================
        8. FRASES E EXPRESSÕES FIXAS
        ============================================================

        • "Sieg Zeon!" → "Sieg Zeon!"
        • "Roger" → "Entendido" ou "Copiado", conforme o contexto militar.
        • "Yes, sir!" → "Sim, senhor!"
        • "No, sir!" → "Não, senhor!"
        • "Launch!" → "Lançar!" ou "Decolar!", conforme o contexto.
        • "Sortie!" → "Partida!" ou "Preparar para lançamento!", conforme a cena.
        • "Stand by" → "Aguardem" ou "Em espera".
        • "All units" → "Todas as unidades".
        • "Enemy mobile suit" → "Mobile Suit inimigo".
        • "The Federation" → "a Federação".
        • "Zeon remnants" → "remanescentes de Zeon".

        ============================================================
        9. FOCO FINAL DA TRADUÇÃO
        ============================================================

        A tradução deve soar como uma legenda brasileira profissional para anime militar adulto.
        O foco é transmitir:
        • a gravidade da guerra;
        • o conflito ideológico entre Zeon e Federação;
        • o fanatismo de Gato e Delaz;
        • o amadurecimento traumático de Kou;
        • a tragédia pessoal envolvendo Nina, Kou e Gato;
        • o clima de conspiração que prepara o surgimento dos Titans.

        Resultado esperado:
        Português brasileiro natural, sério, dramático, consistente e fiel ao universo Gundam.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
            "Mobile Suit Gundam 0083: Stardust Memory",
            LORE
    );

    @Override
    public String getId() {
        return "gundam_0083";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam 0083: Stardust Memory";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }
}