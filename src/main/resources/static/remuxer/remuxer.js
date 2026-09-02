import { logNoConsole, mostrarAlerta } from '../js/app.js';
// O MESMO módulo da Tradução Local, da Revisão de Lore e da 3.3 — nunca uma cópia: invariante 10
// do projeto. Pedido de Paulo em 2026-09-02: o remux é a etapa mais longa do pipeline (vídeo de
// gigabytes por episódio, uma temporada inteira por lote) e era justamente a que terminava em
// silêncio, com o operador longe da máquina.
import { armarAvisoSonoro, tocarAvisoSonoro, mensagemDoAviso } from '../js/avisoSonoro.js';

/**
 * PROPÓSITO DE NEGÓCIO: segura o botão até a fila do pipeline reportar "livre", que é o
 * instante em que o ÚLTIMO arquivo do lote terminou de ser remuxado e publicado.
 *
 * INVARIANTES DO DOMÍNIO: o POST apenas ENFILEIRA — ele responde na hora e o trabalho roda em
 * segundo plano. Sem esta espera, "terminou", "falhou" e "ainda rodando" saem iguais para quem
 * saiu de perto, e o aviso sonoro tocaria no aceite da fila em vez de no fim do trabalho.
 *
 * COMPORTAMENTO EM CASO DE FALHA: rede caída ou status indisponível encerra a espera com aviso
 * no console e libera o botão — nunca prende o operador num botão morto.
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
        logNoConsole('console-remuxer', `Não foi possível acompanhar o estado da fila: ${erro.message}`, 'aviso');
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: mantém o cartão de destino dizendo, em português e antes do clique,
 * para onde os MKVs vão — a subpasta padrão ou a pasta escolhida.
 *
 * INVARIANTES DO DOMÍNIO: campo em branco SEMPRE significa a subpasta padrão dentro da pasta de
 * vídeos, nunca "não vai gravar"; o texto exibido é o caminho digitado, sem reinterpretação.
 *
 * COMPORTAMENTO EM CASO DE FALHA: se o cartão não existir na página (tela antiga em cache), a
 * função não faz nada e o envio do formulário continua funcionando — o resumo é conveniência,
 * não pré-requisito.
 */
function sincronizarCartaoDeDestino() {
    const campo = document.getElementById('remuxer-destino');
    const etiqueta = document.getElementById('remuxer-destino-etiqueta');
    const resumo = document.getElementById('remuxer-destino-resumo');
    if (!campo || !etiqueta || !resumo) return;

    const alvoTexto = resumo.querySelector('span:last-child');

    const atualizar = () => {
        const escolhido = campo.value.trim();
        etiqueta.textContent = escolhido ? 'pasta escolhida' : 'pasta padrão';
        etiqueta.classList.toggle('escolhido', Boolean(escolhido));
        if (!alvoTexto) return;
        alvoTexto.innerHTML = escolhido
            ? `Vai gravar em <code></code>.`
            : 'Vai gravar em <code>mkv_final_ptbr</code>, dentro da pasta de vídeos.';
        if (escolhido) {
            // textContent, não interpolação: nome de pasta do acervo tem colchete e acento, e
            // um caminho digitado não pode virar HTML.
            alvoTexto.querySelector('code').textContent = escolhido;
        }
    };

    campo.addEventListener('input', atualizar);
    campo.addEventListener('change', atualizar);
    atualizar();
}

/**
 * PROPÓSITO DE NEGÓCIO: coleta as opções da etapa final, solicita o remux seguro
 * e encaminha ao console o aceite ou a recusa real da API.
 * INVARIANTES DO DOMÍNIO: offset é inteiro dentro de 24 horas; política de
 * preservação das legendas é sempre enviada explicitamente; o destino do MKV é
 * declarado no console ANTES do envio, dizendo se é o padrão ou o escolhido — o
 * operador não deve descobrir onde o arquivo foi parar procurando no disco.
 * COMPORTAMENTO EM CASO DE FALHA: HTTP 400/409 e falha de rede são exibidos sem
 * anunciar que o remux começou.
 */
export function initRemuxer() {
    const form = document.getElementById('form-remuxer');
    if (!form) return;

    // Evita duplicar o listener se a inicialização for executada múltiplas vezes
    if (form.dataset.listenerRegistered === 'true') return;
    form.dataset.listenerRegistered = 'true';

    sincronizarCartaoDeDestino();

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.style.opacity = '0.6';
            submitBtn.style.cursor = 'not-allowed';
        }

        // A trava de obra já desabilita este botão até a escolha (2026-09-02, ordem de Paulo:
        // a 5.1 deixou de ser tela auxiliar). Esta checagem é a segunda camada: botão
        // desabilitado é UI, e o painel é reinjetado por fetch — uma reaplicação que falhe
        // deixaria o botão vivo sem ninguém ver. Regra do projeto: esconder botão não é
        // autorização.
        const selectContexto = document.getElementById('remuxer-contexto');
        const opcaoObra = selectContexto?.selectedOptions?.[0];
        if (!opcaoObra || opcaoObra.disabled) {
            logNoConsole('console-remuxer',
                'Escolha a obra antes de iniciar o remux — os MKVs publicados levam o nome dela.', 'erro');
            mostrarAlerta('Escolha a obra antes de iniciar o remux!', 'erro');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.style.opacity = '';
                submitBtn.style.cursor = '';
            }
            return;
        }
        const nomeDaObra = opcaoObra.text.trim();

        const entrada = document.getElementById('remuxer-videos').value.trim();
        // `saida` é o nome histórico do campo no contrato da API, mas o que ele carrega é a
        // pasta das LEGENDAS. Quem diz onde o MKV final é gravado é `pastaDestino`.
        const saida = document.getElementById('remuxer-legendas').value.trim();
        const campoDestino = document.getElementById('remuxer-destino');
        const pastaDestino = campoDestino ? campoDestino.value.trim() : '';
        const syncOffsetRaw = document.getElementById('remuxer-sync-offset').value.trim();
        const syncOffsetMs = syncOffsetRaw ? parseInt(syncOffsetRaw, 10) : null;
        // As legendas originais SEMPRE sobrevivem (decisão do Paulo, 2026-07-29). O campo segue
        // no corpo para não quebrar contrato, e o servidor o ignora — não existe mais caminho
        // que apague faixa. O checkbox da tela é só informativo, marcado e desabilitado.
        const preservarLegendasOriginais = true;

        if (syncOffsetMs !== null && (!Number.isInteger(syncOffsetMs) || Math.abs(syncOffsetMs) > 86400000)) {
            logNoConsole('console-remuxer', 'Sincronismo inválido: use um inteiro entre -86400000 e 86400000 ms.', 'erro');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.style.opacity = '';
                submitBtn.style.cursor = '';
            }
            return;
        }

        // Este clique É o gesto que libera o áudio no navegador — um AudioContext criado fora de
        // um gesto do usuário nasce 'suspended' e não emite som, sem erro e sem log. O estado é
        // DITO agora, em três valores: quem vai sair de perto durante um lote de horas precisa
        // saber ANTES se pode confiar no som, não depois de perder o fim.
        const estadoAviso = armarAvisoSonoro();
        logNoConsole('console-remuxer', mensagemDoAviso(estadoAviso),
            estadoAviso === 'armado' ? 'info' : 'aviso');

        logNoConsole('console-remuxer', 'Solicitando remux de vídeos com legendas traduzidas...', 'info');
        logNoConsole('console-remuxer', `Obra: ${nomeDaObra}`, 'info');
        logNoConsole('console-remuxer', `Pasta de Vídeos: ${entrada}`, 'info');
        if (saida) logNoConsole('console-remuxer', `Pasta de Legendas: ${saida}`, 'info');
        logNoConsole('console-remuxer', pastaDestino
            ? `Destino dos MKVs finais: ${pastaDestino} (escolhido)`
            : 'Destino dos MKVs finais: subpasta "mkv_final_ptbr" dentro da pasta de vídeos (padrão)', 'info');
        if (syncOffsetMs) logNoConsole('console-remuxer', `Sincronismo manual: ${syncOffsetMs}ms`, 'info');
        logNoConsole('console-remuxer',
            'Faixas originais: preservadas. A PT-BR entra como primeira opção.', 'info');

        try {
            const res = await fetch('/api/remuxar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ entrada, saida, syncOffsetMs, preservarLegendasOriginais, pastaDestino })
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

            // Daqui até "livre" é o lote inteiro rodando: um MKV de gigabytes por episódio.
            await acompanharConclusao();

            // O alerta VISUAL vem primeiro e SEMPRE: som depende de permissão do navegador, de
            // volume e de a aba não estar no mudo. Se o som fosse a única rede, "terminou"
            // ficaria indistinguível de "ainda rodando" justamente quando o navegador recusa.
            logNoConsole('console-remuxer',
                'Remux do lote concluído — o último arquivo foi publicado. Confira o status acima.', 'sucesso');
            mostrarAlerta('Remux finalizado! Confira o status no console.', 'info');
            tocarAvisoSonoro();

        } catch (err) {
            logNoConsole('console-remuxer', `Erro ao iniciar remuxer: ${err.message}`, 'erro');
        } finally {
            // O botão fica preso enquanto a fila trabalha (o `await` acima) e volta ao fim do
            // lote. O debounce de 3s que existia aqui protegia só contra a rajada de cliques —
            // e deixava o botão vivo durante horas de remux, convidando a um segundo disparo
            // que o servidor recusaria com 409.
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.style.opacity = '';
                submitBtn.style.cursor = '';
            }
        }
    });
}
