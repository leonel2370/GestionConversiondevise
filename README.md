# API de Conversion de Devises

API REST développée avec **Java 25** et **Spring Boot 3.5** qui convertit une somme d'argent
d'une devise à une autre en utilisant des taux de change **récupérés dynamiquement** depuis
l'API externe [Exchangerate-API](https://www.exchangerate-api.com) (v6).

> Projet 3 — Java Spring Boot (énoncé : `3- API de Conversion de Devises.pdf`)

## Fonctionnalités

- Endpoint de conversion prenant en entrée la **devise source**, la **devise cible** et le **montant**.
- Appel à une **API externe** pour obtenir les taux de change (WebClient), avec **cache local** d'une heure
  pour limiter les appels (configurable).
- **Gestion des erreurs** : devise invalide, montant invalide, API externe injoignable — réponses
  homogènes au format [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) (Problem Detail).
- Documentation interactive **Swagger UI** avec exemples de requêtes.

## Prérequis

| Outil | Version |
|---|---|
| JDK | 25 (Temurin recommandé) |
| Maven | 3.9+ (ou wrapper) |
| Docker (optionnel) | 24+ |

## Configuration des données sensibles

Les données sensibles sont **jamais commitées** : elles vivent dans un fichier `.env` à la racine,
ignoré par git. Un modèle versionné `.env.example` documente les variables attendues.

```bash
cp .env.example .env
# puis renseigner votre clé API obtenue sur https://app.exchangerate-api.com
```

| Variable | Obligatoire | Description |
|---|---|---|
| `EXCHANGE_RATE_API_KEY` | oui | Clé API Exchangerate-API |
| `EXCHANGE_RATE_API_BASE_URL` | non | `https://v6.exchangerate-api.com/v6` par défaut |
| `EXCHANGE_RATE_CACHE_MINUTES` | non | Durée du cache des taux en minutes (60 par défaut) |
| `SERVER_PORT` | non | Port d'écoute (8080 par défaut) |

## Lancement

### Avec Maven

```bash
mvn spring-boot:run
```

### Avec Docker

```bash
docker compose up --build
```

## Tester l'API avec Swagger

Une fois l'application démarrée, ouvrir :

- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs

### Exemples de requêtes (curl)

Conversion de 100.50 USD vers EUR :

```bash
curl "http://localhost:8080/api/v1/conversion?source=USD&cible=EUR&montant=100.50"
```

Réponse :

```json
{
  "devise_source": "USD",
  "devise_cible": "EUR",
  "montant": 100.50,
  "taux": 0.9123,
  "montant_converti": 91.69,
  "date": "2026-09-23T10:00:00Z"
}
```

Taux courant entre deux devises :

```bash
curl "http://localhost:8080/api/v1/taux?source=USD&cible=XAF"
```

Liste des devises supportées :

```bash
curl "http://localhost:8080/api/v1/devises"
```

### Exemples de réponses en erreur (RFC 7807)

Devise inconnue :

```bash
curl "http://localhost:8080/api/v1/conversion?source=USD&cible=ZZZ&montant=10"
```

```json
{
  "type": "about:blank",
  "title": "Devise invalide",
  "status": 400,
  "detail": "Devise invalide : ZZZ (non trouvee parmi les devises de conversion de USD)",
  "timestamp": "2026-09-23T10:01:00Z"
}
```

Montant invalide :

```bash
curl "http://localhost:8080/api/v1/conversion?source=USD&cible=EUR&montant=-5"
```

```json
{
  "type": "about:blank",
  "title": "Requete invalide",
  "status": 400,
  "detail": "Le montant doit etre strictement positif",
  "timestamp": "2026-09-23T10:02:00Z"
}
```

## Endpoints

| Méthode | Chemin | Description | Erreurs |
|---|---|---|---|
| `GET` | `/api/v1/conversion?source=&cible=&montant=` | Convertit un montant | 400, 502 |
| `GET` | `/api/v1/taux?source=&cible=` | Taux courant entre deux devises | 400, 502 |
| `GET` | `/api/v1/devises` | Liste des devises supportées | 502 |

## Tests

```bash
mvn test
```

- `ConversionServiceTest` : service testé avec **MockWebServer** (simulation de l'API externe :
  succès, cache, 404, 500, erreur métier).
- `ConversionControllerTest`, `TauxChangeControllerTest` : contrôleurs testés avec **MockMvc**
  (validation des paramètres, sérialisation, gestion d'erreurs).
- `ConversionDevisesApplicationTests` : démarrage du contexte Spring.

## Structure du projet

```
src/main/java/com/k48/leonel/conversiondevises/
├── config/       # Configuration WebClient de l'API externe
├── controller/   # Endpoints REST (conversion, taux, devises)
├── dto/          # Requêtes et réponses (JSON snake_case)
├── exception/    # Exceptions métier + gestionnaire global RFC 7807
└── service/      # Logique de conversion, appel externe, cache
```

## Conventions Git

- Branches : `feature/CONV-1`, `bugfix/...`, `hotfix-X.Y/...` (voir `.agents/skills/git-conventions/SKILL.md`).
- Messages de commit en français, à l'impératif, référencant le ticket : `feature: CONV-1 ...`.
- Aucune donnée sensible dans le dépôt : vérifier `git status` avant chaque commit.
