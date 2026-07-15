import { mostrarAlerta, logNoConsole } from '../js/app.js';

const PAINEL_HTML = 'traducaoKaraoke/traducaoKaraoke.html?v=1.1';

/**
 * PROPÓSITO DE NEGÓCIO: monta o formulário e o terminal visual da Tradução de
 * Karaokê dentro do painel da SPA.
 *
 * INVARIANTES DO DOMÍNIO: o fragmento deve ser carregado uma única vez por página
 * e sempre representar a versão atual, incluindo o console de acompanhamento.
 *
 * COMPORTAMENTO EM CASO DE FALHA: lança um erro quando o servidor não entrega o
 * fragmento; a inicialização converte essa falha em uma mensagem visível no painel.
 */
async function carregarPainelHtml() {
    const painel = document.getElementById('panel-traducao-karaoke');
    if (!painel || painel.dataset.moduloCarregado === 'true') {
        return painel;
    }

    const resposta = await fetch(PAINEL_HTML, { cache: 'no-store' });
    if (!resposta.ok) {
        throw new Error(`Falha ao carregar ${PAINEL_HTML}`);
    }

    painel.innerHTML = await resposta.text();
    painel.dataset.moduloCarregado = 'true';
    return painel;
}

export async function initTraducaoKaraoke() {
    try {
        await carregarPainelHtml();
        vincularEventos();
        document.dispatchEvent(new CustomEvent('traducao-karaoke:painel-carregado'));
    } catch (err) {
        console.error('[Tradução de Karaokê] Erro ao carregar painel:', err);
        const painel = document.getElementById('panel-traducao-karaoke');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar o painel da Tradução de Karaokê.</p></div>';
        }
    }
}

function vincularEventos() {
    const form = document.getElementById('form-traducao-karaoke');
    if (!form) return;

    const btnSimular = document.getElementById('btn-traducao-karaoke-simular');
    const btnAplicar = document.getElementById('btn-traducao-karaoke-aplicar');
    const consoleId = 'console-traducao-karaoke';

    if (btnSimular) {
        btnSimular.addEventListener('click', async () => {
            if (!validarForm()) return;
            await executarOperacao('/api/traducao-karaoke/simular', 'Simulação da Tradução de Karaokê (Dry-Run)');
        });
    }

    if (btnAplicar) {
        btnAplicar.addEventListener('click', async (e) => {
            e.preventDefault();
            if (!validarForm()) return;
            await executarOperacao('/api/traducao-karaoke/aplicar', 'Tradução de Karaokê (LLM)');
        });
    }

    function validarForm() {
        const entrada = document.getElementById('traducao-karaoke-entrada').value.trim();
        if (!entrada) {
            mostrarAlerta('Preencha a pasta com as legendas que deseja traduzir.', 'aviso');
            return false;
        }
        return true;
    }

    async function executarOperacao(url, descricao) {
        const entrada = document.getElementById('traducao-karaoke-entrada').value.trim();
        const contexto = document.getElementById('traducao-karaoke-contexto');
        const contextoId = contexto && contexto.value ? contexto.value : null;

        logNoConsole(consoleId, `Iniciando ${descricao}...`, 'info');

        const botoes = form.querySelectorAll('button');
        botoes.forEach(b => b.disabled = true);

        try {
            const resp = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    caminhoOrigem: entrada,
                    contextoId: contextoId
                })
            });

            if (resp.ok) {
                const contentType = resp.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const dados = await resp.json();
                    if (dados.mensagem) {
                        logNoConsole(consoleId, dados.mensagem, 'sucesso');
                    }
                }
            } else {
                let msgErro = `Erro HTTP ${resp.status}`;
                try {
                    const errorObj = await resp.json();
                    msgErro = errorObj.error || msgErro;
                } catch (e) {
                    msgErro = await resp.text();
                }
                logNoConsole(consoleId, `Falha na operação: ${msgErro}`, 'erro');
                mostrarAlerta(`Erro: ${msgErro}`, 'erro');
            }
        } catch (e) {
            logNoConsole(consoleId, `Erro de rede: ${e.message}`, 'erro');
            mostrarAlerta('Erro de conexão ao servidor.', 'erro');
        } finally {
            botoes.forEach(b => b.disabled = false);
        }
    }
}
