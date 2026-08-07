package org.traducao.projeto.core.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: decide se um caminho pedido pela interface pode ser
 * navegado, e lista o conteúdo quando pode. É o único ponto que autoriza leitura
 * de disco a partir de texto vindo do navegador.
 *
 * <h2>O prejuízo que originou a guarda</h2>
 * O navegador de pastas nasceu para substituir o diálogo nativo do Windows, que
 * não existe dentro de um contêiner. Só que trocar um diálogo LOCAL por um
 * endpoint HTTP muda a natureza do risco: o diálogo só podia ser aberto por quem
 * estava sentado na máquina, o endpoint atende qualquer requisição que chegue.
 * Sem limite de raiz, {@code ?caminho=C:/Users} devolveria a pasta pessoal
 * inteira, e {@code ?caminho=/} devolveria o sistema de arquivos do contêiner.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Falha fechada.</b> Sem raiz configurada, nada é navegável. A ausência
 *       de configuração nunca libera.</li>
 *   <li>A comparação é feita sobre o caminho REAL ({@link Path#toRealPath}), não
 *       sobre o texto. Sem isso, {@code /acervo/../etc} e um link simbólico
 *       apontando para fora passariam por serem, na string, "filhos" da raiz.</li>
 *   <li><b>Cegueira e vazio são sinais DIFERENTES.</b> "Nenhuma raiz
 *       configurada" e "esta pasta não tem subpastas" não podem produzir a mesma
 *       resposta — quem opera precisa distinguir um sistema mal configurado de
 *       uma pasta legitimamente vazia.</li>
 *   <li>Raiz declarada que não existe é ignorada, permitindo que uma única
 *       configuração sirva Windows e contêiner.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nunca devolve lista parcial silenciosa.
 * Caminho inválido, inexistente, fora da raiz ou ilegível produz
 * {@link NavegacaoRecusadaException} com o {@link Motivo} correspondente, que o
 * controller converte em código HTTP distinto.
 */
@Component
public class GuardaRaizNavegacao {

    private static final Logger log = LoggerFactory.getLogger(GuardaRaizNavegacao.class);

    /** Por que a navegação foi recusada. Cada valor vira um HTTP diferente. */
    public enum Motivo {
        /** Nenhuma raiz existente configurada: o servidor está cego, não vazio. */
        SEM_RAIZ_CONFIGURADA,
        /** O caminho existe, mas está fora de toda raiz permitida. */
        FORA_DA_RAIZ,
        /** O caminho não existe ou não é uma pasta. */
        NAO_ENCONTRADO,
        /** Texto que sequer forma um caminho válido para este sistema. */
        CAMINHO_INVALIDO
    }

    /** Recusa de navegação com motivo explícito — nunca uma lista vazia disfarçada. */
    public static class NavegacaoRecusadaException extends RuntimeException {
        private final transient Motivo motivo;

        public NavegacaoRecusadaException(Motivo motivo, String mensagem) {
            super(mensagem);
            this.motivo = motivo;
        }

        public Motivo motivo() {
            return motivo;
        }
    }

    /** Uma pasta listável, no formato que a interface consome. */
    public record Pasta(String nome, String caminho) {}

    private final NavegacaoProperties propriedades;

    public GuardaRaizNavegacao(NavegacaoProperties propriedades) {
        this.propriedades = propriedades;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega as raízes que de fato existem nesta máquina,
     * para a interface oferecer os pontos de partida corretos sem tentar navegar
     * um caminho que só faz sentido no outro ambiente.
     *
     * <p>INVARIANTES DO DOMÍNIO: só entra raiz que existe e é diretório. A lista
     * devolvida é o conjunto EXATO que {@link #listar} vai aceitar como ancestral.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: raiz com texto inválido ou ilegível é
     * descartada com log de aviso; nunca lança.
     */
    public List<Path> raizesExistentes() {
        List<Path> encontradas = new ArrayList<>();
        for (String bruta : propriedades.raizesOuVazio()) {
            if (bruta == null || bruta.isBlank()) {
                continue;
            }
            try {
                Path candidata = Path.of(bruta.trim());
                if (Files.isDirectory(candidata)) {
                    encontradas.add(candidata.toRealPath());
                }
            } catch (InvalidPathException | IOException e) {
                log.warn("Raiz de navegacao ignorada ({}): {}", bruta, e.getMessage());
            }
        }
        return encontradas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista as subpastas de um caminho pedido pela
     * interface, depois de provar que ele está dentro de uma raiz permitida.
     *
     * <p>INVARIANTES DO DOMÍNIO: a autorização acontece ANTES de qualquer leitura
     * e sobre o caminho real. Caminho em branco é interpretado como "quero as
     * raízes", não como raiz do sistema de arquivos. A ordenação é alfabética e
     * insensível a caixa, para a lista não mudar de ordem entre Windows e Linux.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link NavegacaoRecusadaException}
     * com motivo. Nunca devolve lista vazia para representar recusa — pasta
     * legitimamente sem subpastas devolve lista vazia com sucesso, e essa
     * diferença é o ponto.
     *
     * @param caminhoPedido texto vindo da interface; em branco significa "as raízes"
     */
    public List<Pasta> listar(String caminhoPedido) {
        List<Path> raizes = raizesExistentes();
        if (raizes.isEmpty()) {
            throw new NavegacaoRecusadaException(Motivo.SEM_RAIZ_CONFIGURADA,
                "Nenhuma raiz de navegacao configurada existe nesta maquina. "
                    + "Confira a chave 'navegador.raizes' — o servidor esta cego, nao vazio.");
        }

        if (caminhoPedido == null || caminhoPedido.isBlank()) {
            return raizes.stream()
                .map(r -> new Pasta(r.toString(), r.toString()))
                .toList();
        }

        Path real = resolverReal(caminhoPedido);
        if (raizes.stream().noneMatch(real::startsWith)) {
            throw new NavegacaoRecusadaException(Motivo.FORA_DA_RAIZ,
                "Caminho fora das raizes permitidas: " + real);
        }

        try (var fluxo = Files.list(real)) {
            return fluxo
                .filter(Files::isDirectory)
                .map(p -> new Pasta(p.getFileName().toString(), p.toString()))
                .sorted(Comparator.comparing(Pasta::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        } catch (IOException e) {
            throw new NavegacaoRecusadaException(Motivo.NAO_ENCONTRADO,
                "Falha ao ler a pasta " + real + ": " + e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista os ARQUIVOS de um caminho autorizado, para os
     * dois botões do Auditor de Conteúdo que escolhem um {@code .ass} em vez de
     * uma pasta.
     *
     * <h2>INVARIANTES DO DOMÍNIO</h2>
     * <ul>
     *   <li>A autorização é a MESMA de {@link #listar(String)} — reaproveitada,
     *       não reescrita. Duplicar a checagem de raiz criaria o cenário em que
     *       uma das duas cópias é corrigida e a outra não.</li>
     *   <li>Caminho em branco devolve lista vazia: raiz não tem arquivo a
     *       oferecer antes de o operador entrar em alguma pasta.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mesma
     * {@link NavegacaoRecusadaException} da listagem de pastas.
     */
    public List<Pasta> listarArquivos(String caminhoPedido) {
        if (caminhoPedido == null || caminhoPedido.isBlank()) {
            return List.of();
        }
        // Reaproveita integralmente a autorização: se listar() recusa, aqui recusa.
        listar(caminhoPedido);

        Path real = resolverReal(caminhoPedido);
        try (var fluxo = Files.list(real)) {
            return fluxo
                .filter(Files::isRegularFile)
                .map(p -> new Pasta(p.getFileName().toString(), p.toString()))
                .sorted(Comparator.comparing(Pasta::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        } catch (IOException e) {
            throw new NavegacaoRecusadaException(Motivo.NAO_ENCONTRADO,
                "Falha ao ler os arquivos de " + real + ": " + e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a pasta acima da atual, quando ela ainda está
     * dentro de uma raiz — é o "subir um nível" do navegador.
     *
     * <p>INVARIANTES DO DOMÍNIO: o pai de uma raiz é {@code null}, nunca o
     * diretório acima dela. Sem isso, clicar em "subir" na raiz escaparia do
     * limite pela porta da frente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho inválido ou fora da raiz devolve
     * {@code null} em vez de lançar — "não há pai" é resposta legítima aqui.
     */
    public String paiPermitido(String caminhoPedido) {
        if (caminhoPedido == null || caminhoPedido.isBlank()) {
            return null;
        }
        try {
            Path real = resolverReal(caminhoPedido);
            List<Path> raizes = raizesExistentes();
            if (raizes.stream().anyMatch(real::equals)) {
                return null;
            }
            Path pai = real.getParent();
            if (pai == null || raizes.stream().noneMatch(pai::startsWith)) {
                return null;
            }
            return pai.toString();
        } catch (NavegacaoRecusadaException e) {
            return null;
        }
    }

    /**
     * Converte o texto em caminho real. Existir é pré-requisito de
     * {@link Path#toRealPath}, e é justamente isso que faz link simbólico e
     * {@code ..} serem resolvidos antes da comparação com a raiz.
     */
    private Path resolverReal(String caminhoPedido) {
        Path bruto;
        try {
            bruto = Path.of(caminhoPedido.trim());
        } catch (InvalidPathException e) {
            throw new NavegacaoRecusadaException(Motivo.CAMINHO_INVALIDO,
                "Caminho invalido: " + e.getMessage());
        }
        if (!Files.isDirectory(bruto)) {
            throw new NavegacaoRecusadaException(Motivo.NAO_ENCONTRADO,
                "Pasta inexistente: " + bruto);
        }
        try {
            return bruto.toRealPath();
        } catch (IOException e) {
            throw new NavegacaoRecusadaException(Motivo.NAO_ENCONTRADO,
                "Pasta ilegivel: " + bruto);
        }
    }
}
