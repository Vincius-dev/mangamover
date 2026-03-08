# MangaMover — Release Notes

## v1.0.0 (2026-03-08)

Primeira versão do MangaMover, aplicação Java para monitorar e mover arquivos de manga entre pastas (ex: Suwayomi → Kavita).

### Funcionalidades

- **Gerenciamento de Jobs**: criação, edição, remoção e execução manual de jobs de movimentação
- **Monitoramento automático**: WatchService NIO com threads virtuais (Java 21) — detecta arquivos novos e move automaticamente com delay de 1s para aguardar escrita completa
- **Movimentação segura**: `Files.move(ATOMIC_MOVE)` com fallback copy+delete para dispositivos cruzados; resolução de conflitos por sufixo (`arquivo_1.cbz`, `arquivo_2.cbz`...)
- **Histórico**: registro de cada arquivo movido com status OK/ERROR e mensagem de erro
- **Interface web**: SPA com Bootstrap 5, stats em tempo real (auto-refresh 30s), tabela de jobs com CRUD, modal create/edit, histórico paginado com filtro por job
- **Banco embarcado**: SQLite em modo WAL, thread-safety via `synchronized`
- **Docker**: imagem multi-stage (~200MB), mem_limit 200m, -Xmx128m

### Stack

| Componente | Versão |
|---|---|
| Java | 21 |
| Javalin | 6.3.0 |
| sqlite-jdbc | 3.46.1.3 |
| Jackson | 2.17.2 |
| SLF4J Simple | 2.0.13 |
| Base image | eclipse-temurin:21-jre-jammy |

### API REST

| Método | Endpoint | Descrição |
|---|---|---|
| GET | /api/stats | Contadores gerais + watchers ativos |
| GET | /api/jobs | Lista todos os jobs |
| POST | /api/jobs | Cria job (inicia watcher se watch=true) |
| PUT | /api/jobs/{id} | Atualiza job (reinicia watcher) |
| DELETE | /api/jobs/{id} | Remove job (para watcher) |
| POST | /api/jobs/{id}/run | Executa job manualmente (async, 202) |
| GET | /api/history | Histórico paginado (?page&per_page&job_id) |

### Como usar

```bash
# Build
mvn package -DskipTests

# Local
java -jar target/mangamover-1.0.0.jar

# Docker
docker compose up --build
```

Acesse `http://localhost:8765`

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| PORT | 8765 | Porta HTTP |
| DB_PATH | manga_mover.db | Caminho do banco SQLite |
| LOG_LEVEL | INFO | Nível de log |
