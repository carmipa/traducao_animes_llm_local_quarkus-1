import { logNoConsole, mostrarAlerta } from '../js/app.js';

const PAINEL_HTML = 'revisaoConcordancia/revisaoConcordancia.html?v=1.0';

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-revisao-concordancia');
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

/**
 * PROPÓSITO DE NEGÓCIO: mantém o botão bloqueado enquanto o job está na fila/execução,
 * evitando duplo disparo (o POST só ENFILEIRA; o trabalho roda em segundo plano).
 * INVARIANTES DO DOMÍNIO: só retorna quando a fila reporta "livre".
 * COMPORTAMENTO EM CASO DE FALHA: loga aviso e retorna, liberando o botão.
 */
async function acompanharConclusao() {
    try {
        for (;;) {
            const resposta = await fetch('/api/pipeline/status', { cache: 'no-store' });
            if (!resposta.ok) break;
            const dados = await resposta.json();
            if (dados.mensagem === 'livre') break;
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    } catch (erro) {
        logNoConsole('console-revisao-concordancia', `Não foi possível acompanhar o estado da fila: ${erro.message}`, 'aviso');
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: liga o formulário ao endpoint que corrige concordância de gênero na
 * pasta PT-BR, respeitando o dry-run.
 * INVARIANTES DO DOMÍNIO: a pasta PT-BR é obrigatória; aplicar = !simular.
 * COMPORTAMENTO EM CASO DE FALHA: exibe o erro no console e reabilita o botão.
 */
function vincularEventos() {
    const btn = document.getElementById('btn-iniciar-revisao-concordancia');
    const input = document.getElementById('revisao-concordancia-entrada');
    const chkSimular = document.getElementById('revisao-concordancia-simular');
    if (!btn || !input) return;

    btn.addEventListener('click', async () => {
        const diretorioTraduzido = input.value.trim();
        if (!diretorioTraduzido) {
            mostrarAlerta('Informe a pasta com as legendas traduzidas (PT-BR)!', 'erro');
            return;
        }
        const aplicar = chkSimular ? !chkSimular.checked : true;

        logNoConsole('console-revisao-concordancia',
            `Iniciando revisão de concordância — ${diretorioTraduzido} | ${aplicar ? 'APLICAR' : 'simular (dry-run)'}`, 'info');
        btn.disabled = true;

        try {
            const res = await fetch('/api/revisar-concordancia', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ diretorioTraduzido, aplicar })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.erro || 'Falha ao iniciar revisão de concordância');
            }
            logNoConsole('console-revisao-concordancia', data.mensagem || 'Revisão de concordância iniciada.', 'sucesso');
            mostrarAlerta('Revisão de concordância iniciada! Acompanhe os logs.', 'sucesso');

            await acompanharConclusao();
            mostrarAlerta('Revisão de concordância finalizada. Confira o status no console.', 'info');
        } catch (err) {
            logNoConsole('console-revisao-concordancia', `Erro: ${err.message}`, 'erro');
            mostrarAlerta(err.message, 'erro');
        } finally {
            btn.disabled = false;
        }
    });
}

export async function initRevisaoConcordancia() {
    try {
        await carregarPainelHtml();
        vincularEventos();
    } catch (err) {
        console.error('[Revisão de Concordância] Erro ao carregar painel:', err);
        const painel = document.getElementById('panel-revisao-concordancia');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar o painel de Revisão de Concordância.</p></div>';
        }
    }
}
