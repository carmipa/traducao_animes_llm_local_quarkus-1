package org.traducao.projeto.core.io;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: declara as raízes de disco que o navegador de pastas da
 * interface tem permissão de listar — o acervo de animes e a própria pasta de
 * trabalho do KRONOS.
 *
 * <h2>Por que isto existe</h2>
 * Até 06/08/2026 a escolha de pasta na interface era feita por um diálogo NATIVO
 * do Windows, aberto pelo servidor via {@code powershell.exe}. Isso funcionava
 * porque servidor e usuário eram a mesma máquina. Dentro de um contêiner Linux
 * não há {@code powershell.exe}, nem Windows Forms, nem display gráfico: os 11
 * botões "Procurar..." de 7 telas simplesmente deixariam de funcionar.
 *
 * <p>A substituição é um navegador servido pelo próprio servidor. Só que um
 * navegador de disco exposto por HTTP precisa de limite — sem ele, qualquer
 * requisição listaria a máquina inteira. Estas raízes são esse limite.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li>Lista VAZIA não significa "libera tudo": significa que nada pode ser
 *       navegado. A falha é fechada por construção.</li>
 *   <li>Raiz declarada que não existe no disco é ignorada em silêncio — a mesma
 *       configuração precisa servir Windows ({@code C:/animes}) e contêiner
 *       ({@code /acervo}) sem exigir dois arquivos.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: chave ausente no YAML deixa a lista nula;
 * {@link #raizesOuVazio()} normaliza para lista vazia, que recusa tudo. Nunca
 * lança.
 *
 * <p>NOTA DE FORMA: é classe com construtor sem argumentos e setter, não record.
 * A extensão {@code quarkus-spring-boot-properties} recusa record no build
 * ("must contain a no-arg constructor"), e é o mesmo molde de
 * {@code TradutorProperties}.
 */
@ConfigurationProperties(prefix = "navegador")
public class NavegacaoProperties {

    private List<String> raizes = List.of();

    public NavegacaoProperties() {
    }

    public NavegacaoProperties(List<String> raizes) {
        this.raizes = raizes;
    }

    public List<String> getRaizes() {
        return raizes;
    }

    public void setRaizes(List<String> raizes) {
        this.raizes = raizes;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega as raízes configuradas sem obrigar cada
     * chamador a repetir a checagem de nulo.
     *
     * <p>INVARIANTES DO DOMÍNIO: ausência de configuração vira lista vazia, e
     * lista vazia recusa toda navegação. É deliberado que o caminho preguiçoso
     * seja o seguro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; devolve lista vazia.
     */
    public List<String> raizesOuVazio() {
        return raizes == null ? List.of() : raizes;
    }
}
