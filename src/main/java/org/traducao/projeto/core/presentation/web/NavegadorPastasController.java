package org.traducao.projeto.core.presentation.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.io.GuardaRaizNavegacao;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: serve à interface a árvore de pastas que o SERVIDOR
 * enxerga, para que a escolha de pasta funcione onde quer que o KRONOS esteja
 * rodando — inclusive dentro de um contêiner, onde o diálogo nativo do Windows
 * não existe.
 *
 * <h2>O prejuízo que originou o endpoint</h2>
 * Os 11 botões "Procurar..." de 7 telas chamavam {@code /api/dialogo/*}, que abre
 * um {@code OpenFileDialog} do Windows executando {@code powershell.exe} NO
 * SERVIDOR. Isso só funciona porque servidor e usuário são a mesma máquina. Num
 * contêiner Linux não há powershell, Windows Forms nem display: a escolha de
 * pasta simplesmente deixaria de existir. Foram 8 amarras de Windows encontradas
 * num único controller.
 *
 * <p>Efeito colateral que vale mais que a causa: escolhendo da árvore que o
 * servidor realmente enxerga, o caminho JÁ NASCE VÁLIDO. Some a classe inteira de
 * erro em que o usuário digita um caminho que o processo não alcança e recebe
 * "0 arquivos encontrados" como se a pasta estivesse vazia.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li>Toda autorização é de {@link GuardaRaizNavegacao}. Este controller não
 *       lê disco por conta própria e não conhece raiz nenhuma.</li>
 *   <li>Cada motivo de recusa vira um HTTP DIFERENTE. "Servidor sem raiz
 *       configurada" responde 503, não 200 com lista vazia — cegueira e pasta
 *       vazia não podem ser indistinguíveis para quem opera.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nunca devolve 200 com lista vazia para
 * representar erro. Recusa carrega {@code motivo} legível no corpo, e o 200 com
 * {@code pastas: []} significa exatamente uma coisa — a pasta não tem subpastas.
 */
@RestController
@RequestMapping("/api/navegador")
public class NavegadorPastasController {

    private final GuardaRaizNavegacao guarda;

    public NavegadorPastasController(GuardaRaizNavegacao guarda) {
        this.guarda = guarda;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista as subpastas navegáveis de um caminho, ou as
     * raízes permitidas quando nenhum caminho é informado.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code caminho} em branco significa "me dê os
     * pontos de partida", nunca a raiz do sistema de arquivos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 503 sem raiz configurada, 403 fora da
     * raiz, 404 para pasta inexistente, 400 para texto que não forma caminho.
     */
    @GetMapping("/pastas")
    public ResponseEntity<Map<String, Object>> listar(
        @RequestParam(name = "caminho", required = false) String caminho,
        @RequestParam(name = "tipo", required = false, defaultValue = "pasta") String tipo) {

        try {
            List<GuardaRaizNavegacao.Pasta> pastas = guarda.listar(caminho);
            // Só os dois botões do Auditor de Conteúdo pedem arquivo; para os
            // outros 25 a lista vem vazia e a interface nem a renderiza.
            List<GuardaRaizNavegacao.Pasta> arquivos = "arquivo".equalsIgnoreCase(tipo)
                ? guarda.listarArquivos(caminho)
                : List.of();
            String pai = guarda.paiPermitido(caminho);
            return ResponseEntity.ok(Map.of(
                "atual", caminho == null ? "" : caminho,
                "pai", pai == null ? "" : pai,
                "raizes", guarda.raizesExistentes().stream().map(Path::toString).toList(),
                "pastas", pastas,
                "arquivos", arquivos
            ));
        } catch (GuardaRaizNavegacao.NavegacaoRecusadaException e) {
            return ResponseEntity.status(statusDe(e.motivo())).body(Map.of(
                "erro", e.getMessage(),
                "motivo", e.motivo().name()
            ));
        }
    }

    /**
     * Traduz o motivo da recusa em código HTTP. A separação é o ponto: um painel
     * ou script que trate tudo como "deu ruim" perde a distinção entre servidor
     * mal configurado (503, resolve-se mexendo em YAML) e tentativa de sair da
     * raiz (403, resolve-se corrigindo o pedido).
     */
    private static HttpStatus statusDe(GuardaRaizNavegacao.Motivo motivo) {
        return switch (motivo) {
            case SEM_RAIZ_CONFIGURADA -> HttpStatus.SERVICE_UNAVAILABLE;
            case FORA_DA_RAIZ -> HttpStatus.FORBIDDEN;
            case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CAMINHO_INVALIDO -> HttpStatus.BAD_REQUEST;
        };
    }
}
