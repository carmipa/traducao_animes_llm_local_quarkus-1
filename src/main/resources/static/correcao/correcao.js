import { logNoConsole } from '../js/app.js';

export function initCorrecao() {
    const btnLimpar = document.getElementById('btn-limpar-cache');
    const btnScraping = document.getElementById('btn-scraping-google');
    const btnRevisarCache = document.getElementById('btn-revisar-cache');
    const btnTermoEnsaio = document.getElementById('btn-reforcar-terminologia-ensaio');
    const btnTermoAplicar = document.getElementById('btn-reforcar-terminologia-aplicar');
    const alvoCaixa = document.getElementById('correcao-alvo');
    const alvoTexto = document.getElementById('correcao-alvo-texto');
    const travaTexto = document.getElementById('op-trava-terminologia');

    /**
     * PROPÓSITO DE NEGÓCIO: guarda o alvo em que o ENSAIO de terminologia foi medido, para que a
     * aplicação só destrave enquanto continuar valendo para o mesmo alvo.
     * INVARIANTES DO DOMÍNIO: {@code null} significa "nenhum ensaio nesta sessão" — estado inicial
     * e estado após qualquer troca de pasta ou de contexto.
     */
    let alvoEnsaiado = null;

    /**
     * PROPÓSITO DE NEGÓCIO: envia aos três modos o mesmo alvo e o contexto de
     * fallback necessário para caches antigos sem proveniência.
     * INVARIANTES DO DOMÍNIO: contexto vazio nunca substitui a proveniência de
     * caches novos; o backend decide a lore arquivo por arquivo.
     * COMPORTAMENTO EM CASO DE FALHA: devolve objeto válido com strings vazias.
     */
    const montarRequisicao = () => ({
        entrada: document.getElementById('correcao-entrada')?.value.trim() || '',
        contextoId: document.getElementById('correcao-contexto')?.value || ''
    });

    /**
     * PROPÓSITO DE NEGÓCIO: identidade textual do alvo atual, usada para comparar o alvo do ensaio
     * com o alvo do clique em aplicar.
     * INVARIANTES DO DOMÍNIO: inclui o contexto, porque trocar a obra muda a terminologia medida.
     * COMPORTAMENTO EM CASO DE FALHA: campos ausentes viram string vazia; nunca lança.
     */
    const chaveAlvo = () => {
        const { entrada, contextoId } = montarRequisicao();
        return `${entrada}\u0000${contextoId}`;
    };

    /**
     * PROPÓSITO DE NEGÓCIO: escreve na tela o que a próxima operação vai atingir. O campo de pasta
     * vazio significa "acervo INTEIRO", que é o oposto do que um campo em branco costuma sugerir —
     * deixar isso implícito é o que transforma um clique de rotina num estrago em lote.
     * INVARIANTES DO DOMÍNIO: só reflete o estado dos campos; não altera nada.
     * COMPORTAMENTO EM CASO DE FALHA: elementos ausentes fazem a função não ter efeito.
     */
    const atualizarAlvo = () => {
        if (!alvoCaixa || !alvoTexto) return;
        const { entrada } = montarRequisicao();
        if (entrada) {
            alvoCaixa.classList.remove('op-alvo-acervo');
            alvoTexto.innerHTML = 'Alvo: <strong>somente esta pasta</strong> — <code></code>';
            alvoTexto.querySelector('code').textContent = entrada;
        } else {
            alvoCaixa.classList.add('op-alvo-acervo');
            alvoTexto.innerHTML = 'Alvo: <strong>o acervo INTEIRO</strong> — todos os <code>*.cache.json</code> '
                + 'da pasta padrão <code>cache</code>, inclusive as pastas de comparação. '
                + 'Informe uma pasta acima para restringir.';
        }
    };

    /**
     * PROPÓSITO DE NEGÓCIO: mantém a trava do "Aplicar" da terminologia coerente com o ensaio.
     * INVARIANTES DO DOMÍNIO: o botão só fica clicável quando existe um ensaio DESTA sessão feito
     * no MESMO alvo — trocar a pasta ou a obra invalida a medição e volta a travar. A trava é
     * contra aplicar às cegas, não prova de que o ensaio deu certo: se o ensaio falhar, o console
     * mostra a falha e o operador decide.
     * COMPORTAMENTO EM CASO DE FALHA: botão ausente faz a função não ter efeito.
     */
    const atualizarTravaTerminologia = () => {
        if (!btnTermoAplicar) return;
        const medidoAqui = alvoEnsaiado !== null && alvoEnsaiado === chaveAlvo();
        btnTermoAplicar.disabled = !medidoAqui;
        if (!travaTexto) return;
        travaTexto.classList.toggle('oculto', medidoAqui);
        travaTexto.textContent = alvoEnsaiado === null
            ? 'Destrava após o ensaio'
            : 'O alvo mudou — ensaie de novo';
    };

    /**
     * PROPÓSITO DE NEGÓCIO: exige confirmação quando a operação vai escrever no acervo inteiro.
     * INVARIANTES DO DOMÍNIO: só pergunta quando a pasta está em branco (raiz do cache). Com uma
     * pasta informada não aparece diálogo nenhum — a proteção mira exatamente onde está o risco.
     * COMPORTAMENTO EM CASO DE FALHA: recusa (devolve false) se o operador cancelar.
     *
     * @param acao descrição da operação, na primeira linha do diálogo
     */
    const confirmarAlvoRaiz = (acao) => {
        if (montarRequisicao().entrada) return true;
        return window.confirm(
            `${acao}\n\n`
            + 'A pasta do cache está EM BRANCO, então o alvo é o acervo INTEIRO: todos os arquivos '
            + '*.cache.json da pasta padrão "cache", inclusive as pastas de comparação.\n\n'
            + 'Para agir sobre uma obra só, cancele e informe a pasta dela.\n\n'
            + 'Continuar no acervo inteiro?');
    };

    /**
     * PROPÓSITO DE NEGÓCIO: evita cliques repetidos enquanto a manutenção aceita
     * ainda está na fila ou em execução.
     * INVARIANTES DO DOMÍNIO: o "Aplicar" da terminologia NÃO é reabilitado em bloco — ele volta
     * pela trava, senão o fim de qualquer operação destravaria o botão sem ensaio nenhum.
     * COMPORTAMENTO EM CASO DE FALHA: reabilita os botões no bloco finally.
     */
    const acompanharConclusao = async () => {
        [btnLimpar, btnScraping, btnRevisarCache, btnTermoEnsaio, btnTermoAplicar]
            .filter(Boolean).forEach(btn => { btn.disabled = true; });
        try {
            for (;;) {
                const resposta = await fetch('/api/pipeline/status', { cache: 'no-store' });
                if (!resposta.ok) break;
                const dados = await resposta.json();
                if (dados.mensagem === 'livre') break;
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        } catch (erro) {
            logNoConsole('console-correcao', `Não foi possível acompanhar o estado da fila: ${erro.message}`, 'aviso');
        } finally {
            [btnLimpar, btnScraping, btnRevisarCache, btnTermoEnsaio]
                .filter(Boolean).forEach(btn => { btn.disabled = false; });
            atualizarTravaTerminologia();
        }
    };

    /**
     * PROPÓSITO DE NEGÓCIO: dispara o reforço de terminologia sobre o cache já gravado.
     * INVARIANTES DO DOMÍNIO: ensaio e aplicação são ROTAS distintas, não um parâmetro — a ação
     * que reescreve o acervo não pode depender de um booleano invertido por engano. A aplicação
     * ainda pede confirmação explícita, porque age em lote sobre trabalho já pronto.
     * COMPORTAMENTO EM CASO DE FALHA: registra o erro no console da página e reabilita os botões.
     */
    const dispararReforco = async (aplicar) => {
        if (aplicar && !confirmarAlvoRaiz('Aplicar a terminologia oficial ao cache já gravado.')) {
            return;
        }
        if (aplicar && !window.confirm(
            'Isto REESCREVE o cache já gravado, aplicando a terminologia oficial de cada obra.\n\n'
            + 'Há backup e escrita atômica por arquivo, mas rode o ENSAIO antes para ver o que muda.\n\n'
            + 'Aplicar agora?')) {
            return;
        }
        const body = montarRequisicao();
        const alvoDoDisparo = chaveAlvo();
        const rota = aplicar ? '/api/reforcar-terminologia-aplicar' : '/api/reforcar-terminologia-ensaio';

        /**
         * A recusa por escrita desligada não é falha: é a trava fazendo o trabalho dela.
         * Explica O QUE aconteceu, POR QUE a trava existe e O QUE fazer — em vez de
         * despejar o JSON da resposta atrás de um "Falha:" vermelho.
         */
        function explicarEscritaBloqueada(mensagemDoServidor) {
            const cx = 'console-correcao';
            logNoConsole(cx, '', 'aviso');
            logNoConsole(cx, '─────────────────────────────────────────────────────────────', 'aviso');
            logNoConsole(cx, '  ESCRITA NO ACERVO DESLIGADA — nada foi lido nem escrito', 'aviso');
            logNoConsole(cx, '─────────────────────────────────────────────────────────────', 'aviso');
            logNoConsole(cx, '', 'aviso');
            logNoConsole(cx, '  Isto NÃO é um erro. A trava recusou o pedido ANTES de abrir', 'aviso');
            logNoConsole(cx, '  qualquer arquivo — seu cache está exatamente como estava.', 'aviso');
            logNoConsole(cx, '', 'aviso');
            logNoConsole(cx, '  POR QUE ELA EXISTE', 'aviso');
            logNoConsole(cx, '  O cache guarda tradução já paga ao LLM e NÃO é versionado', 'aviso');
            logNoConsole(cx, '  pelo Git. Reescrever por engano não tem "desfazer" — só o', 'aviso');
            logNoConsole(cx, '  backup por arquivo que a própria operação cria.', 'aviso');
            logNoConsole(cx, '', 'aviso');
            logNoConsole(cx, '  O QUE VOCÊ PODE FAZER AGORA', 'aviso');
            logNoConsole(cx, '  1) O ENSAIO roda sempre e não precisa de autorização.', 'aviso');
            logNoConsole(cx, '     Ele mostra, arquivo por arquivo, o que mudaria.', 'aviso');
            logNoConsole(cx, '  2) Se o ensaio acusar 0 alterações, ligar a escrita não', 'aviso');
            logNoConsole(cx, '     muda nada: falta o mapa de terminologia daquela obra.', 'aviso');
            logNoConsole(cx, '', 'aviso');
            logNoConsole(cx, '  PARA AUTORIZAR DE VERDADE', 'aviso');
            logNoConsole(cx, '  application.yml →', 'aviso');
            logNoConsole(cx, '      correcao-cache:', 'aviso');
            logNoConsole(cx, '        reforco-terminologia-aplicar-habilitado: true', 'aviso');
            logNoConsole(cx, '  A configuração vincula no BOOT: é preciso reiniciar a', 'aviso');
            logNoConsole(cx, '  aplicação para valer.', 'aviso');
            logNoConsole(cx, '', 'aviso');
            if (mensagemDoServidor) {
                logNoConsole(cx, `  Resposta do servidor: ${mensagemDoServidor}`, 'info');
            }
            logNoConsole(cx, '─────────────────────────────────────────────────────────────', 'aviso');
        }
        logNoConsole('console-correcao',
            aplicar ? 'Aplicando terminologia oficial ao cache...' : 'Ensaiando reforço de terminologia (nada será escrito)...',
            'info');
        if (body.entrada) logNoConsole('console-correcao', `Pasta de Cache: ${body.entrada}`, 'info');

        try {
            const res = await fetch(rota, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (!res.ok) {
                const bruto = await res.text();
                let mensagem = bruto;
                try {
                    mensagem = (JSON.parse(bruto) || {}).mensagem || bruto;
                } catch (_) {
                    // corpo não-JSON (proxy, 500 cru): mostra como veio, sem esconder.
                }
                // 409 é PRÉ-CONDIÇÃO de segurança, não erro de execução: o
                // CorrecaoCacheController recusa antes de enfileirar e nada foi lido nem
                // escrito. Tratar como "Falha" em vermelho ensinava errado — o operador via
                // um erro onde a proteção tinha funcionado.
                if (res.status === 409) {
                    explicarEscritaBloqueada(mensagem);
                    return;
                }
                throw new Error(mensagem || 'Erro no reforço de terminologia');
            }
            const dados = await res.json();
            logNoConsole('console-correcao', dados.mensagem || 'Aceito pela fila.', 'sucesso');
            await acompanharConclusao();
            if (!aplicar) {
                // Destrava só DEPOIS que a fila esvaziou: o número medido já está no console,
                // que é justamente o que o operador precisa ter visto antes de aplicar.
                alvoEnsaiado = alvoDoDisparo;
                atualizarTravaTerminologia();
                logNoConsole('console-correcao',
                    'Ensaio concluído — o botão "Aplicar" desta caixa está liberado para este mesmo alvo.', 'info');
            }
        } catch (erro) {
            logNoConsole('console-correcao', `Falha: ${erro.message}`, 'erro');
        }
    };

    if (btnTermoEnsaio) {
        btnTermoEnsaio.addEventListener('click', () => dispararReforco(false));
    }
    if (btnTermoAplicar) {
        btnTermoAplicar.addEventListener('click', () => dispararReforco(true));
    }

    if (btnLimpar) {
        btnLimpar.addEventListener('click', async () => {
            if (!confirmarAlvoRaiz('Limpar as falhas do cache de tradução.')) return;
            const body = montarRequisicao();
            const entrada = body.entrada;
            logNoConsole('console-correcao', 'Disparando limpeza de cache de tradução...', 'info');
            if (entrada) logNoConsole('console-correcao', `Pasta de Cache: ${entrada}`, 'info');

            try {
                const res = await fetch('/api/corrigir-cache', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro ao limpar cache');
                }

                const data = await res.json();
                logNoConsole('console-correcao', 'Limpeza aceita pela fila; acompanhe o resultado real abaixo.', 'sucesso');
                if (data.mensagem) {
                    logNoConsole('console-correcao', data.mensagem, 'info');
                }
                await acompanharConclusao();
            } catch (err) {
                logNoConsole('console-correcao', `Erro na limpeza: ${err.message}`, 'erro');
            }
        });
    }

    if (btnScraping) {
        btnScraping.addEventListener('click', async () => {
            if (!confirmarAlvoRaiz('Preencher as lacunas do cache via Google Tradutor.')) return;
            const body = montarRequisicao();
            const entrada = body.entrada;
            logNoConsole('console-correcao', 'Disparando corretor via Scraping Google Tradutor...', 'info');
            if (entrada) logNoConsole('console-correcao', `Pasta de Cache: ${entrada}`, 'info');

            try {
                const res = await fetch('/api/corrigir-scraping', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro no scraping de correção');
                }

                const data = await res.json();
                logNoConsole('console-correcao', 'Processamento de raspagem de correção iniciado!', 'sucesso');
                if (data.mensagem) {
                    logNoConsole('console-correcao', data.mensagem, 'info');
                }
                await acompanharConclusao();
            } catch (err) {
                logNoConsole('console-correcao', `Erro no scraping: ${err.message}`, 'erro');
            }
        });
    }

    if (btnRevisarCache) {
        btnRevisarCache.addEventListener('click', async () => {
            if (!confirmarAlvoRaiz('Revisar a concordância PT-BR do cache com o LLM local.')) return;
            const body = montarRequisicao();
            const entrada = body.entrada;
            const contextoId = body.contextoId;
            logNoConsole('console-correcao', 'Disparando revisão de concordância PT-BR no cache...', 'info');
            if (entrada) logNoConsole('console-correcao', `Pasta de Cache: ${entrada}`, 'info');
            if (contextoId) logNoConsole('console-correcao', `Contexto: ${contextoId}`, 'info');

            try {
                const res = await fetch('/api/revisar-cache', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro na revisão de concordância do cache');
                }

                const data = await res.json();
                logNoConsole('console-correcao', 'Revisão de concordância do cache iniciada!', 'sucesso');
                if (data.mensagem) {
                    logNoConsole('console-correcao', data.mensagem, 'info');
                }
                await acompanharConclusao();
            } catch (err) {
                logNoConsole('console-correcao', `Erro na revisão do cache: ${err.message}`, 'erro');
            }
        });
    }

    // O espelho do alvo e a trava reagem a QUALQUER mudança dos dois campos. O "Limpar Campos"
    // é tratado à parte porque o handler compartilhado grava o valor direto, sem disparar evento.
    ['correcao-entrada', 'correcao-contexto'].forEach(id => {
        const campo = document.getElementById(id);
        if (!campo) return;
        ['input', 'change'].forEach(evento => campo.addEventListener(evento, () => {
            atualizarAlvo();
            atualizarTravaTerminologia();
        }));
    });
    document.querySelector('#panel-correcao .btn-clear-form')?.addEventListener('click', () => {
        setTimeout(() => { atualizarAlvo(); atualizarTravaTerminologia(); }, 0);
    });

    atualizarAlvo();
    atualizarTravaTerminologia();
}
