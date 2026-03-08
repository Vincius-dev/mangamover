@echo off
:: =============================================================================
:: MangaMover - Script de desenvolvimento (Windows)
:: Uso: dev.bat <comando>
:: =============================================================================

setlocal enabledelayedexpansion

set JAR=target\mangamover-1.0.0.jar
set JACOCO_REPORT=target\site\jacoco\index.html

if "%1"=="" goto :usage
if "%1"=="help"          goto :usage
if "%1"=="--help"        goto :usage
if "%1"=="-h"            goto :usage
if "%1"=="build"         goto :build
if "%1"=="test"          goto :test
if "%1"=="test-v"        goto :test_v
if "%1"=="coverage"      goto :coverage
if "%1"=="coverage-open" goto :coverage_open
if "%1"=="run"           goto :run
if "%1"=="build-run"     goto :build_run
if "%1"=="clean"         goto :clean
if "%1"=="clean-build"   goto :clean_build
if "%1"=="package"       goto :package
if "%1"=="verify"        goto :verify

echo [ERR]  Comando desconhecido: '%1'
goto :usage

:: ---------------------------------------------------------------------------
:usage
echo.
echo   MangaMover dev.bat
echo.
echo   Uso: dev.bat ^<comando^>
echo.
echo   Comandos disponíveis:
echo     build          Compila o projeto e gera o JAR
echo     test           Executa os testes unitários
echo     test-v         Executa os testes com output detalhado
echo     coverage       Executa testes e gera relatório JaCoCo
echo     coverage-open  Gera relatório JaCoCo e abre no browser
echo     run            Executa a aplicação (requer JAR compilado)
echo     build-run      Compila e executa em seguida
echo     clean          Remove arquivos compilados (target\)
echo     clean-build    Limpa e recompila
echo     package        Empacota sem rodar testes
echo     verify         Compila, testa e verifica
echo     help           Exibe esta ajuda
echo.
goto :eof

:: ---------------------------------------------------------------------------
:build
echo [INFO] Compilando projeto...
call mvn package -DskipTests
if errorlevel 1 ( echo [ERR]  Falha na compilação. & exit /b 1 )
echo [OK]   JAR gerado em: %JAR%
goto :eof

:: ---------------------------------------------------------------------------
:test
echo [INFO] Executando testes...
call mvn test -q
if errorlevel 1 ( echo [ERR]  Testes falharam. & exit /b 1 )
echo [OK]   Todos os testes passaram.
goto :eof

:: ---------------------------------------------------------------------------
:test_v
echo [INFO] Executando testes (verbose)...
call mvn test
goto :eof

:: ---------------------------------------------------------------------------
:coverage
echo [INFO] Executando testes com cobertura JaCoCo...
call mvn test
if errorlevel 1 ( echo [ERR]  Testes falharam. & exit /b 1 )
if exist "%JACOCO_REPORT%" (
    echo.
    echo [OK]   Relatório gerado em: %JACOCO_REPORT%
) else (
    echo [WARN] Relatório JaCoCo não encontrado.
)
goto :eof

:: ---------------------------------------------------------------------------
:coverage_open
call :coverage_inner
if exist "%JACOCO_REPORT%" (
    echo [INFO] Abrindo relatório no browser...
    start "" "%JACOCO_REPORT%"
)
goto :eof

:coverage_inner
echo [INFO] Executando testes com cobertura JaCoCo...
call mvn test
if errorlevel 1 ( echo [ERR]  Testes falharam. & exit /b 1 )
if exist "%JACOCO_REPORT%" (
    echo [OK]   Relatório gerado em: %JACOCO_REPORT%
) else (
    echo [WARN] Relatório JaCoCo não encontrado.
)
goto :eof

:: ---------------------------------------------------------------------------
:run
if not exist "%JAR%" (
    echo [ERR]  JAR não encontrado. Execute 'dev.bat build' primeiro.
    exit /b 1
)
echo [INFO] Iniciando MangaMover...
java -jar "%JAR%"
goto :eof

:: ---------------------------------------------------------------------------
:build_run
call :build
if errorlevel 1 exit /b 1
call :run
goto :eof

:: ---------------------------------------------------------------------------
:clean
echo [INFO] Limpando target\...
call mvn clean -q
if errorlevel 1 ( echo [ERR]  Falha ao limpar. & exit /b 1 )
echo [OK]   Limpo.
goto :eof

:: ---------------------------------------------------------------------------
:clean_build
call :clean
if errorlevel 1 exit /b 1
call :build
goto :eof

:: ---------------------------------------------------------------------------
:package
echo [INFO] Empacotando (sem testes)...
call mvn package -DskipTests -q
if errorlevel 1 ( echo [ERR]  Falha ao empacotar. & exit /b 1 )
echo [OK]   JAR gerado em: %JAR%
goto :eof

:: ---------------------------------------------------------------------------
:verify
echo [INFO] Verificando projeto completo (compile + test + package)...
call mvn verify
if errorlevel 1 ( echo [ERR]  Verificação falhou. & exit /b 1 )
echo [OK]   Verificação concluída.
goto :eof
