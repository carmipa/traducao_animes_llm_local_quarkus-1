package org.traducao.projeto.core.texto.gramatica;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: dublê do revisor gramatical para os testes que NÃO são sobre gramática.
 *
 * <h2>Por que existe, e por que ele é "mudo" e não "vazio"</h2>
 * O caso de uso da 3.3 passou a ter dois corretores em 23/08/2026. Os testes do corretor de
 * gênero e do console continuam sendo sobre outra coisa, e carregar o LanguageTool neles custaria
 * 1,2 s por classe para exercitar código que não está sendo testado ali.
 *
 * <p>O dublê declara-se <b>INDISPONÍVEL</b>, e isso é escolha, não preguiça: assim os testes que o
 * usam exercitam o caminho em que o revisor não subiu — que é justamente o caminho onde o projeto
 * exige que zero correções apareça como <i>NÃO VERIFICADO</i>, e não como <i>está limpo</i>.
 * Um dublê que dissesse "disponível, sem achados" esconderia essa distinção do teste.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: não tem falha; devolve sempre o mesmo.
 */
public class RevisorGramaticalMudo implements RevisorGramaticalPort {

    public static final String MOTIVO = "duble de teste: revisor gramatical nao carregado";

    @Override
    public List<AchadoGramatical> revisar(String texto) {
        return List.of();
    }

    @Override
    public boolean disponivel() {
        return false;
    }

    @Override
    public String motivoDaIndisponibilidade() {
        return MOTIVO;
    }
}
