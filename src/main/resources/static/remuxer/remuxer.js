import { logNoConsole } from '../js/app.js';

/**
 * PROPÓSITO DE NEGÓCIO: coleta as opções da etapa final, solicita o remux seguro
 * e encaminha ao console o aceite ou a recusa real da API.
 * INVARIANTES DO DOMÍNIO: offset é inteiro dentro de 24 horas; política de
 * preservação das legendas é sempre enviada explicitamente.
 * COMPORTAMENTO EM CASO DE FALHA: HTTP 400/409 e falha de rede são exibidos sem
 * anunciar que o remux começou.
 */
export function initRemuxer() {
    const form = document.getElementById('form-remuxer');
    if (!form) return;

    // Evita duplicar o listener se a inicialização for executada múltiplas vezes
    if (form.dataset.listenerRegistered === 'true') return;
    form.dataset.listenerRegistered = 'true';

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.style.opacity = '0.6';
            submitBtn.style.cursor = 'not-allowed';
        }

        const entrada = document.getElementById('remuxer-videos').value.trim();
        const saida = document.getElementById('remuxer-legendas').value.trim();
        const syncOffsetRaw = document.getElementById('remuxer-sync-offset').value.trim();
        const syncOffsetMs = syncOffsetRaw ? parseInt(syncOffsetRaw, 10) : null;
        const preservarLegendasOriginais = document.getElementById('remuxer-preservar-legendas').checked;

        if (syncOffsetMs !== null && (!Number.isInteger(syncOffsetMs) || Math.abs(syncOffsetMs) > 86400000)) {
            logNoConsole('console-remuxer', 'Sincronismo inválido: use um inteiro entre -86400000 e 86400000 ms.', 'erro');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.style.opacity = '';
                submitBtn.style.cursor = '';
            }
            return;
        }

        logNoConsole('console-remuxer', 'Solicitando remux de vídeos com legendas traduzidas...', 'info');
        logNoConsole('console-remuxer', `Pasta de Vídeos: ${entrada}`, 'info');
        if (saida) logNoConsole('console-remuxer', `Pasta de Legendas: ${saida}`, 'info');
        if (syncOffsetMs) logNoConsole('console-remuxer', `Sincronismo manual: ${syncOffsetMs}ms`, 'info');
        logNoConsole('console-remuxer', preservarLegendasOriginais
            ? 'Faixas originais: serão preservadas.'
            : 'Faixas originais: serão substituídas pela nova PT-BR.', 'info');

        try {
            const res = await fetch('/api/remuxar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ entrada, saida, syncOffsetMs, preservarLegendasOriginais })
            });

            if (!res.ok) {
                const erroTexto = await res.text();
                let mensagem = erroTexto;
                try {
                    mensagem = JSON.parse(erroTexto).mensagem || erroTexto;
                } catch (_) {
                    // Resposta textual permanece como está.
                }
                throw new Error(mensagem || 'Erro interno ao iniciar remuxer');
            }

            const data = await res.json();
            logNoConsole('console-remuxer', 'Remuxer aceito pela fila; acompanhe o resultado real abaixo.', 'sucesso');
            if (data.mensagem) {
                logNoConsole('console-remuxer', data.mensagem, 'info');
            }

        } catch (err) {
            logNoConsole('console-remuxer', `Erro ao iniciar remuxer: ${err.message}`, 'erro');
        } finally {
            // Re-habilita após 3 segundos para evitar cliques múltiplos em rajada (debounce)
            setTimeout(() => {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.style.opacity = '';
                    submitBtn.style.cursor = '';
                }
            }, 3000);
        }
    });
}
