@echo off
rem EXPERIMENTO do Zeta: exercitar as 6 falas pendentes SEM retraduzir os 50 episodios.
rem
rem O problema que este script resolve, com numero: o cache do Zeta foi gravado com
rem mistralai/mistral-nemo-instruct-2407. Trocar o titular para a aya invalida a proveniencia e
rem manda 17.090 falas de volta ao LLM — horas — para exercitar 6.
rem
rem Duas chaves, as duas DESLIGADAS por padrao no application.yml (falha fechada):
rem
rem   TRADUTOR_REUSO_ENTRE_MODELOS=true
rem       autoriza a aya a REAPROVEITAR o que o mistral traduziu. So o que faltar vai ao LLM.
rem       O cache resultante e carimbado com modeloHerdado=mistral..., entao ele NAO serve para
rem       comparar os dois modelos — e diz isso de si mesmo, em vez de mentir.
rem       Lore diferente CONTINUA invalidando; a autorizacao vale so para o modelo.
rem
rem   TRADUTOR_LLM_MODELO_RECUPERACAO=aya-expanse-8b
rem       a aya como SEGUNDA OPINIAO. Como o titular vem do que o LM Studio tiver carregado,
rem       deixe o mistral-nemo carregado para ele ser o titular e a aya so receber o que sobrar.
rem
rem CONFERIR NO CONSOLE, antes de aceitar qualquer conclusao:
rem   [SEGUNDA-OPINIAO] Ligada: "aya-expanse-8b"
rem   [CACHE] REUSO ENTRE MODELOS: N fala(s) de "mistralai/..." reaproveitada(s) por "aya..."
rem Se o reuso nao aparecer, o titular provavelmente nao e o mistral — confira qual modelo o
rem LM Studio entrega, porque o adaptador escolhe o configurado apenas se ele estiver CARREGADO
rem em memoria, e cai no primeiro da lista quando nao esta.
rem
rem BASELINE PROTEGIDO: backups\cache-zeta-mistral-20260812 (82 de 82 caches, com a estrutura de
rem pastas). Este experimento REGRAVA o cache do Zeta; e de la que se restaura.
cd /d "%~dp0"
set TRADUTOR_REUSO_ENTRE_MODELOS=true
set TRADUTOR_LLM_MODELO_RECUPERACAO=aya-expanse-8b
call "%~dp0gradlew.bat" quarkusDev > console-web.log 2>&1
