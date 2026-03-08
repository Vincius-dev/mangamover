#!/usr/bin/env bash
# =============================================================================
# MangaMover - Script de desenvolvimento
# Uso: ./dev.sh <comando>
# =============================================================================

set -euo pipefail

JAR="target/mangamover-1.0.0.jar"
MAIN="com.mangamover.Main"
JACOCO_REPORT="target/site/jacoco/index.html"

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()    { echo -e "${CYAN}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERR]${NC}  $*"; }

usage() {
    echo ""
    echo -e "${CYAN}MangaMover dev.sh${NC}"
    echo ""
    echo "Uso: ./dev.sh <comando>"
    echo ""
    echo "Comandos disponíveis:"
    echo "  build        Compila o projeto e gera o JAR"
    echo "  test         Executa os testes unitários"
    echo "  test-v       Executa os testes com output detalhado"
    echo "  coverage     Executa testes e gera relatório JaCoCo"
    echo "  coverage-open  Gera relatório JaCoCo e abre no browser"
    echo "  run          Executa a aplicação (requer JAR compilado)"
    echo "  build-run    Compila e executa em seguida"
    echo "  clean        Remove arquivos compilados (target/)"
    echo "  clean-build  Limpa e recompila"
    echo "  package      Empacota sem rodar testes"
    echo "  verify       Compila, testa e verifica"
    echo "  help         Exibe esta ajuda"
    echo ""
}

cmd_build() {
    info "Compilando projeto..."
    mvn package -DskipTests
    success "JAR gerado em: $JAR"
}

cmd_test() {
    info "Executando testes..."
    mvn test -q
    success "Todos os testes passaram."
}

cmd_test_v() {
    info "Executando testes (verbose)..."
    mvn test
}

cmd_coverage() {
    info "Executando testes com cobertura JaCoCo..."
    mvn test
    echo ""
    # Extrai resumo do relatório HTML
    if [ -f "$JACOCO_REPORT" ]; then
        python3 - <<'PYEOF'
import re, sys
html = open("target/site/jacoco/index.html").read()
rows = re.findall(r'<tr[^>]*>(.*?)</tr>', html, re.DOTALL)
for row in rows:
    cells = re.findall(r'<td[^>]*>(.*?)</td>', row, re.DOTALL)
    cells = [re.sub(r'<[^>]+>', '', c).strip() for c in cells]
    if cells and ('Total' in cells[0] or 'com.mangamover' in cells[0]):
        print(f"  {cells[0]:<35} instruções: {cells[2]:<6}  branches: {cells[4]}")
PYEOF
        echo ""
        success "Relatório completo: $JACOCO_REPORT"
    else
        warn "Relatório JaCoCo não encontrado."
    fi
}

cmd_coverage_open() {
    cmd_coverage
    if [ -f "$JACOCO_REPORT" ]; then
        info "Abrindo relatório no browser..."
        xdg-open "$JACOCO_REPORT" 2>/dev/null \
            || open "$JACOCO_REPORT" 2>/dev/null \
            || warn "Não foi possível abrir automaticamente. Acesse: $JACOCO_REPORT"
    fi
}

cmd_run() {
    if [ ! -f "$JAR" ]; then
        error "JAR não encontrado. Execute './dev.sh build' primeiro."
        exit 1
    fi
    info "Iniciando MangaMover..."
    java -jar "$JAR"
}

cmd_build_run() {
    cmd_build
    cmd_run
}

cmd_clean() {
    info "Limpando target/..."
    mvn clean -q
    success "Limpo."
}

cmd_clean_build() {
    cmd_clean
    cmd_build
}

cmd_package() {
    info "Empacotando (sem testes)..."
    mvn package -DskipTests -q
    success "JAR gerado em: $JAR"
}

cmd_verify() {
    info "Verificando projeto completo (compile + test + package)..."
    mvn verify
    success "Verificação concluída."
}

# --- Dispatcher ---
case "${1:-help}" in
    build)         cmd_build ;;
    test)          cmd_test ;;
    test-v)        cmd_test_v ;;
    coverage)      cmd_coverage ;;
    coverage-open) cmd_coverage_open ;;
    run)           cmd_run ;;
    build-run)     cmd_build_run ;;
    clean)         cmd_clean ;;
    clean-build)   cmd_clean_build ;;
    package)       cmd_package ;;
    verify)        cmd_verify ;;
    help|--help|-h) usage ;;
    *)
        error "Comando desconhecido: '${1}'"
        usage
        exit 1
        ;;
esac
