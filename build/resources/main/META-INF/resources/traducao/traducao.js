import { logNoConsole, mostrarAlerta } from '../js/app.js';

let contextosCarregados = false;

/**
 * PROPÓSITO DE NEGÓCIO: prepara o formulário da tradução local e envia a decisão
 * explícita de Paulo sobre manter ou liberar a proteção de arquivos PT-BR existentes.
 *
 * INVARIANTES DO DOMÍNIO: a proteção começa sempre ativada; a liberação só é enviada
 * quando a caixa correspondente estiver marcada no momento da submissão.
 *
 * COMPORTAMENTO EM CASO DE FALHA: valida contexto e resposta HTTP, registra a falha
 * no console da interface e não inicia o acompanhamento de uma operação rejeitada.
 */
export function initTraducao() {
    const form = document.getElementById('form-traducao');
    carregarContextos();

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const entrada = document.getElementById('traducao-entrada').value.trim();
        const saida = document.getElementById('traducao-saida').value.trim();
        const contextoSelect = document.getElementById('traducao-contexto');
        const contextoId = contextoSelect ? contextoSelect.value : null;
        const permitirRetraducao = document.getElementById('traducao-liberar-protecao')?.checked === true;

        if (!contextosCarregados || !contextoId) {
            logNoConsole('console-traducao', 'Lista de contextos de tradução ainda não carregou. Aguarde ou recarregue a página antes de iniciar.', 'erro');
            return;
        }

        logNoConsole('console-traducao', 'Iniciando pipeline de tradução local via LLM...', 'info');
        logNoConsole('console-traducao', `Pasta Original: ${entrada}`, 'info');
        if (saida) logNoConsole('console-traducao', `Pasta de Saída: ${saida}`, 'info');
        if (contextoId) logNoConsole('console-traducao', `Contexto Ativo: ${contextoId}`, 'info');
        logNoConsole('console-traducao', permitirRetraducao
            ? 'Retradução integral: LIBERADA (cache anterior e arquivos PT-BR serão preservados em backup).'
            : 'Proteção de arquivos finais: ATIVADA.', permitirRetraducao ? 'aviso' : 'info');

        try {
            const reqBody = { entrada: entrada, permitirRetraducao: permitirRetraducao };
            if (saida) reqBody.saida = saida;
            if (contextoId) reqBody.contextoId = contextoId;

            const res = await fetch('/api/traduzir', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(reqBody)
            });

            if (!res.ok) {
                const erroTexto = await res.text();
                let msg = 'Erro interno ao iniciar tradução';
                try {
                    const parsed = JSON.parse(erroTexto);
                    if (parsed.mensagem) msg = parsed.mensagem;
                } catch(e) {
                    if (erroTexto) msg = erroTexto;
                }
                throw new Error(msg);
            }

            const data = await res.json();
            logNoConsole('console-traducao', 'Tradução iniciada com sucesso em segundo plano!', 'sucesso');
            if (data.mensagem) {
                logNoConsole('console-traducao', data.mensagem, 'info');
            }

            iniciarAcompanhamentoTraducao();

        } catch (err) {
            logNoConsole('console-traducao', `Erro ao iniciar tradução: ${err.message}`, 'erro');
            mostrarAlerta(`Falha: ${err.message}`, 'erro');
        }
    });
}

async function carregarContextos() {
    const select = document.getElementById('traducao-contexto');
    if (!select) return;

    try {
        const response = await fetch('/api/contextos', { cache: 'no-store' });
        if (!response.ok) {
            throw new Error('Resposta HTTP ' + response.status);
        }

        const contextos = await response.json();
        if (!Array.isArray(contextos) || contextos.length === 0) {
            throw new Error('Nenhum contexto de tradução cadastrado no servidor.');
        }

        select.innerHTML = '';
        contextos.forEach(ctx => {
            const opt = document.createElement('option');
            opt.value = ctx.id;
            opt.textContent = ctx.nome;
            // Pré-seleciona o contexto padrão do servidor (ex.: DanMachi) em vez de
            // deixar o navegador escolher a primeira opção pela ordem alfabética,
            // o que faria a tradução usar a lore errada sem o usuário perceber.
            if (ctx.padrao) opt.selected = true;
            select.appendChild(opt);
        });

        contextosCarregados = true;
    } catch (err) {
        console.error('Erro ao carregar contextos:', err);
        select.innerHTML = '<option value="">Erro ao carregar — recarregue a página</option>';
        contextosCarregados = false;
    }
}

function iniciarAcompanhamentoTraducao() {
    logNoConsole('console-traducao', 'Acompanhando execução do tradutor local...', 'info');
}
