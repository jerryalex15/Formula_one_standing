# F1 Standings — Architecture microservices

Plateforme event-driven de calcul et diffusion des classements pilotes F1 (1950 → 2024), construite pour explorer Spring Batch, Kafka et Spring Cloud Gateway.

## Données

Les CSV F1 (source [Ergast/Kaggle](https://www.kaggle.com/datasets/rohanrao/formula-1-world-championship-1950-2020)) ne sont pas versionnés dans ce repo.

1. Télécharge le dataset depuis Kaggle
2. Place les fichiers suivants dans `batchF1/src/main/resources/data/` :
   - `races.csv`
   - `drivers.csv`
   - `results.csv`
   - `sprint_results.csv`

## Architecture

```
   CSV (Ergast)
        │
        ▼
  ┌──────────────┐
  │   batchF1    │──────▶ Postgres (données F1)
  │ Spring Batch │
  └──────┬───────┘
         │ publie un événement (fin de job)
         ▼
  ┌──────────────┐
  │    Kafka     │
  └──────┬───────┘
         │ consomme
         ▼
  ┌───────────────────────┐
  │ standings-api-service │──────▶ Postgres (lecture)
  │   (REST + cache)      │
  └──────┬────────────────┘
         │
         ▼
  ┌──────────────┐
  │ api-gateway  │◀────── Client
  └──────┬───────┘
         │
         ▼
      Redis (rate limiting)
```
## Composants

### `batchF1`
Job Spring Batch qui :
- ingère 4 CSV (races, drivers, results, sprint_results) en parallèle
- calcule le classement pilotes par saison (fenêtre SQL `DENSE_RANK`/`ROW_NUMBER` partitionnée)
- exporte un CSV final
- s'exécute via un `@Scheduled` (cron)
- publie un événement Kafka (`driver-standings-calculated`) à la fin de chaque exécution

**Stack** : Spring Batch 6, Spring Kafka, PostgreSQL, JDBC (readers/writers `FlatFileItemReader`, `JdbcPagingItemReader`), partitionnement dynamique.

### `standings-api-service`
Microservice qui :
- consomme les événements Kafka du topic `driver-standings-calculated`
- invalide un cache en mémoire (`@CacheEvict`) à chaque nouvel événement
- expose une API REST (`GET /api/standings/{season}`) avec cache (`@Cacheable`) pour éviter de retaper Postgres à chaque requête

**Stack** : Spring Kafka (consumer), Spring Cache, Spring Web, JDBC.

### `api-gateway`
Point d'entrée unique qui :
- route les requêtes vers `standings-api-service`
- applique un rate limiting (Token Bucket) via Redis, cohérent même si le Gateway est scalé horizontalement

**Stack** : Spring Cloud Gateway (WebFlux), Redis (rate limiter).

## Lancer le projet en local

### Prérequis
- Java 25
- Maven
- Docker

### 1. Infrastructure (Kafka + Redis + Postgres)

```bash
docker compose up -d
```

### 2. Base de données

Créer la base et exécuter le schéma :
```bash
psql -U app_user -d batch_f1_db -f batchF1/src/main/resources/schema.sql
```

### 3. Lancer les services (ordre recommandé)

```bash
cd batchF1 && mvn spring-boot:run
cd standings-api-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### 4. Tester

```bash
# Déclenche manuellement une exécution (sinon attend le scheduler)
# Consulter les classements via le Gateway
curl http://localhost:8080/api/standings/1961

# Tester le rate limiting
for i in {1..15}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/standings/1961; done
```

## Points techniques notables

- **Partitionnement dynamique** (`DynamicDecadePartitioner`) pour paralléliser le calcul des classements par décennie.
- **Idempotence du batch** : purge des tables (`clearTablesStep`) avant chaque réingestion, pour supporter une exécution planifiée récurrente sans conflits de clés.
- **Découplage producer/consumer** : DTO d'événement dupliqué volontairement entre `batchF1` et `standings-api-service` (packages différents), avec `USE_TYPE_INFO_HEADERS=false` côté consumer pour ne pas coupler les deux services aux noms de classe Java internes de l'autre.
- **Rate limiting distribué** : implémentation Redis plutôt qu'un compteur en mémoire, pour rester cohérent si le Gateway est répliqué.

## À venir / pistes d'amélioration
- Authentification (JWT) sur le Gateway
- WebSocket/SSE pour notifier les clients en temps réel
- Dockerisation complète des 3 microservices (Dockerfile + ajout au docker-compose)