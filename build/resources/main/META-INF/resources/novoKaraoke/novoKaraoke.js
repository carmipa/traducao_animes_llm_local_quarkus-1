import { mostrarAlerta, logNoConsole } from '../js/app.js';

const PAINEL_HTML = 'novoKaraoke/novoKaraoke.html?v=1.1';

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-novo-karaoke');
    if (!painel || painel.dataset.moduloCarregado === 'true') {
        return painel;
    }

    const resposta = await fetch(PAINEL_HTML);
    if (!resposta.ok) {
        throw new Error(`Falha ao carregar ${PAINEL_HTML}`);
    }

    painel.innerHTML = await resposta.text();
    painel.dataset.moduloCarregado = 'true';
    return painel;
}

export async function initNovoKaraoke() {
    try {
        await carregarPainelHtml();
        vincularEventos();
        document.dispatchEvent(new CustomEvent('novo-karaoke:painel-carregado'));
    } catch (err) {
        console.error('[Karaokê Simples] Erro ao carregar painel:', err);
        const painel = document.getElementById('panel-novo-karaoke');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar o painel do Karaokê Simples.</p></div>';
        }
    }
}

function vincularEventos() {
    const form = document.getElementById('form-novo-karaoke');
    if (!form) return;

    const btnSimular = document.getElementById('btn-novo-karaoke-simular');
    const btnAplicar = document.getElementById('btn-novo-karaoke-aplicar');
    const consoleId = 'console-novo-karaoke';

    if (btnSimular) {
        btnSimular.addEventListener('click', async () => {
            if (!validarForm()) return;
            await executarOperacao('/api/novo-karaoke/simular', 'Simulação da Conversão de Karaokê (Dry-Run)');
        });
    }

    if (btnAplicar) {
        btnAplicar.addEventListener('click', async (e) => {
            e.preventDefault();
            if (!validarForm()) return;
            await executarOperacao('/api/novo-karaoke/aplicar', 'Conversão de Karaokê');
        });
    }

    function validarForm() {
        const entrada = document.getElementById('novo-karaoke-entrada').value.trim();
        const saida = document.getElementById('novo-karaoke-saida').value.trim();

        if (!entrada || !saida) {
            mostrarAlerta('Preencha a pasta de entrada e a pasta de destino.', 'aviso');
            return false;
        }
        if (entrada.replace(/[\\/]+$/, '').toLowerCase() === saida.replace(/[\\/]+$/, '').toLowerCase()) {
            mostrarAlerta('A pasta de destino deve ser DIFERENTE da entrada: os arquivos lidos são preservados para auditoria.', 'aviso');
            return false;
        }
        return true;
    }

    async function executarOperacao(url, descricao) {
        const entrada = document.getElementById('novo-karaoke-entrada').value.trim();
        const saida = document.getElementById('novo-karaoke-saida').value.trim();

        logNoConsole(consoleId, `Iniciando ${descricao}...`, 'info');

        const botoes = form.querySelectorAll('button');
        botoes.forEach(b => b.disabled = true);

        try {
            const resp = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    caminhoOrigem: entrada,
                    caminhoDestino: saida
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
