@echo off
rem Sobe o KRONOS em dev com a SEGUNDA OPINIAO ligada, para exercitar o modelo-recuperacao.
rem
rem Por que um script separado e nao editar o yml: "desligado por padrao" e decisao de design
rem (falha fechada) — o pipeline nao adota um segundo modelo so porque ele esta carregado no
rem servidor. Este arquivo liga o experimento sem mudar o default do projeto, e some quando
rem o experimento acabar.
rem
rem Titular x recuperacao, medido no LM Studio em 12/08/2026:
rem   sem especificar modelo      -> aya-expanse-8b        (titular)
rem   pedindo o tower             -> towerinstruct-...     (override honrado pelo servidor)
rem O tower recuperou 3 das 6 pendencias do Zeta em 11/08; a aya nunca foi testada neste papel.
rem
rem CONFERIR QUE PEGOU: o console imprime, antes de cada lote,
rem   [SEGUNDA-OPINIAO] Ligada: "towerinstruct-mistral-7b-v0.2"
rem Se aparecer DESLIGADA, a variavel nao chegou na JVM — nao interprete "nenhuma recuperacao"
rem como "o modelo nao serviu".
cd /d "%~dp0"
set TRADUTOR_LLM_MODELO_RECUPERACAO=towerinstruct-mistral-7b-v0.2
call "%~dp0gradlew.bat" quarkusDev > console-web.log 2>&1
