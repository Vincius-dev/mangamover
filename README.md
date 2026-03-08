# MangaMover

Aplicação Java para monitorar pastas e mover arquivos de manga automaticamente entre diretórios — por exemplo, da pasta de downloads do Suwayomi para a biblioteca do Kavita.

## Funcionalidades

- **Jobs configuráveis**: defina pares origem → destino com filtro de extensões (ex: `.cbz`, `.cbr`)
- **Monitoramento automático**: detecta arquivos novos via Java NIO WatchService com threads virtuais (Java 21); move com delay de 1s para aguardar escrita completa
- **Movimentação segura**: `Files.move(ATOMIC_MOVE)` com fallback copy+delete para volumes cruzados; conflitos resolvidos por sufixo (`arquivo_1.cbz`, `arquivo_2.cbz`...)
- **Execução manual**: dispara um job imediatamente via UI ou API
- **Histórico**: registro de cada arquivo movido com status OK/ERROR
- **Interface web**: SPA com Bootstrap 5 — stats em tempo real (auto-refresh 30s), CRUD de jobs, histórico paginado com filtro
- **Banco embarcado**: SQLite em modo WAL
- **Docker**: imagem multi-stage, mem_limit 200m

## Stack

| Componente | Versão |
|---|---|
| Java | 21 |
| Javalin | 6.3.0 |
| sqlite-jdbc | 3.46.1.3 |
| Jackson | 2.17.2 |
| SLF4J Simple | 2.0.13 |
| Base image | eclipse-temurin:21-jre-jammy |

## Início rápido

### Docker (recomendado)

1. Edite o `docker-compose.yml` para mapear suas pastas:

```yaml
volumes:
  - /caminho/para/suwayomi/downloads:/manga/source:ro
  - /caminho/para/kavita/library:/manga/dest
```

2. Suba o serviço:

```bash
docker compose up -d --build
```

3. Acesse `http://localhost:8765` e crie seu primeiro job.

### Local (sem Docker)

```bash
# Build
mvn package -DskipTests

# Executa
java -jar target/mangamover-1.0.0.jar
```

Acesse `http://localhost:8765`.

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `PORT` | `8765` | Porta HTTP |
| `DB_PATH` | `manga_mover.db` | Caminho do banco SQLite |
| `LOG_LEVEL` | `INFO` | Nível de log (DEBUG, INFO, WARN) |

## API REST

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/stats` | Contadores gerais + watchers ativos |
| GET | `/api/jobs` | Lista todos os jobs |
| POST | `/api/jobs` | Cria job |
| PUT | `/api/jobs/{id}` | Atualiza job |
| DELETE | `/api/jobs/{id}` | Remove job |
| POST | `/api/jobs/{id}/run` | Executa job manualmente (async, 202) |
| GET | `/api/history` | Histórico paginado (`?page&per_page&job_id`) |
| GET | `/api/logs` | Log de eventos em tempo real |

## Estrutura do projeto

```
src/main/java/com/mangamover/
├── Main.java
├── config/AppConfig.java
├── db/Database.java
├── model/          # Job, HistoryEntry, LogEntry
├── service/        # JobService, WatcherService, FileMoverService, HistoryService, LogStore
└── api/            # JobController, HistoryController, StatsController, LogController
src/main/resources/public/
└── index.html      # Frontend SPA
```
