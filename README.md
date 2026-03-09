# Task Manager API — DeployFast



API REST de gestion des tâches avec pipeline CI/CD complet (CI, tests automatisés, SonarQube, DevSecOps, déploiement Docker).

## Stack Technique

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3.2 + Java 17 |
| Sécurité | Spring Security + JWT (JJWT 0.12) + BCrypt |
| Base de données | PostgreSQL 16 |
| Tests | JUnit 5 + Mockito + JaCoCo (≥ 60%) |
| Qualité | SonarQube 10 Community |
| CI/CD | GitHub Actions (4 jobs) |
| Conteneurisation | Docker multi-stage + Docker Compose |

---

## Démarrage rapide

**Prérequis :** Docker & Docker Compose, Java 17, Maven 3.9+

```bash
# 1. Créer le fichier de variables d'environnement
cp .env.example .env   # puis éditer les mots de passe

# 2. Démarrer tous les services (app + postgres + sonarqube)
docker-compose up -d

# 3. Vérifier l'état
docker-compose ps
```

| Service | URL |
|---------|-----|
| API REST | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| SonarQube | http://localhost:9000 (admin / admin) |

### Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `DB_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/taskmanager` |
| `DB_USERNAME` | Utilisateur DB | `postgres` |
| `DB_PASSWORD` | Mot de passe DB | `postgres` |
| `JWT_SECRET` | Clé secrète JWT (Base64, 64 bytes) | valeur de démo |
| `JWT_EXPIRATION` | Durée de vie du token (ms) | `86400000` (24h) |

---

## Endpoints REST

### Authentification — public

| Méthode | Route | Code | Description |
|---------|-------|------|-------------|
| `POST` | `/auth/register` | 201 | Créer un compte |
| `POST` | `/auth/login` | 200 | Connexion + token JWT |

### Tâches — `Authorization: Bearer <token>` requis

| Méthode | Route | Code | Description |
|---------|-------|------|-------------|
| `POST` | `/tasks` | 201 | Créer une tâche |
| `GET` | `/tasks` | 200 | Lister mes tâches (paginé + filtres) |
| `GET` | `/tasks/{id}` | 200 | Détail d'une tâche |
| `PUT` | `/tasks/{id}` | 200 | Modifier une tâche |
| `DELETE` | `/tasks/{id}` | 200 | Supprimer une tâche |

**Paramètres de filtrage sur `GET /tasks` :**
- `?status=TODO|IN_PROGRESS|DONE|CANCELLED`
- `?keyword=texte` — recherche dans titre et description
- `?page=0&size=10&sortBy=createdAt&sortDir=desc`

### Codes de réponse

| Code | Signification |
|------|---------------|
| 200 | Succès |
| 201 | Ressource créée |
| 400 | Données invalides (validation) |
| 401 | Token manquant ou expiré |
| 403 | Accès refusé (pas propriétaire) |
| 404 | Ressource introuvable |
| 500 | Erreur serveur interne |

---

## Architecture du projet

```
src/main/java/com/deployfast/taskmanager/
├── config/          # SecurityConfig, OpenApiConfig
├── controller/      # AuthController, TaskController (couche HTTP uniquement)
├── dto/
│   ├── request/     # LoginRequest, RegisterRequest, TaskRequest
│   └── response/    # AuthResponse, TaskResponse, ApiResponse<T>
├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
├── model/
│   ├── User.java    # Entité JPA + implémente UserDetails
│   ├── Task.java    # Entité JPA avec @PrePersist / @PreUpdate
│   └── enums/       # Role, TaskStatus
├── repository/      # UserRepository, TaskRepository (Spring Data JPA)
├── security/        # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/
    ├── AuthService.java / TaskService.java   (interfaces)
    └── impl/        # AuthServiceImpl, TaskServiceImpl (logique métier)
```

---

## Tests

```bash
# Lancer tous les tests
mvn test

# Tests avec rapport de couverture JaCoCo
mvn test jacoco:report
# → Rapport HTML : target/site/jacoco/index.html

# Vérifier que la couverture respecte les 60% minimaux
mvn jacoco:check
```

---

## Analyse SonarQube

```bash
# Démarrer SonarQube via Docker Compose
docker-compose up -d sonardb sonarqube
# Attendre ~2 min puis ouvrir http://localhost:9000

# Lancer l'analyse
mvn clean test jacoco:report sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=VOTRE_TOKEN
```

---

## Build et déploiement Docker

```bash
# Build manuel de l'image
docker build -t deployfast/task-manager:latest .

# Déploiement complet
docker-compose up -d

# Mise à jour de l'application uniquement
docker-compose pull app && docker-compose up -d app

# Logs en temps réel
docker-compose logs -f app

# Vérification de santé
curl http://localhost:8080/api/v1/actuator/health
```

---
---

# Réponses aux Questions de l'Épreuve

---

## Question 1 — Conception Architecturale et Modélisation

### 1.1 Besoin fonctionnel

**Acteurs :**
- **Utilisateur authentifié** (`ROLE_USER`) : gère ses propres tâches (CRUD)
- **Administrateur** (`ROLE_ADMIN`) : accès complet (extension future)
- **Système CI/CD** : acteur technique qui exécute les pipelines automatisés

**Principales fonctionnalités :**
- Gestion des comptes : inscription et connexion sécurisée via JWT
- CRUD des tâches : créer, lire, modifier, supprimer ses propres tâches
- Filtrage et pagination : par statut, par mot-clé, avec tri configurable
- Sécurité : isolation des données par utilisateur, validation stricte, protection XSS

**Contraintes techniques :**
- API stateless (pas de session serveur)
- Authentification par token JWT dans le header `Authorization: Bearer <token>`
- Validation obligatoire de toutes les entrées
- Couverture de tests ≥ 60%
- Conteneurisation Docker obligatoire

### 1.2 Modélisation des données

**Table `users`**

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| id | BIGINT | PK, AUTO_INC | Identifiant unique |
| email | VARCHAR(100) | UNIQUE, NOT NULL | Email de connexion |
| password | VARCHAR(255) | NOT NULL | Hash BCrypt |
| first_name | VARCHAR(50) | NOT NULL | Prénom |
| last_name | VARCHAR(50) | NOT NULL | Nom |
| role | ENUM | NOT NULL | `ROLE_USER` / `ROLE_ADMIN` |
| created_at | TIMESTAMP | NOT NULL | Date de création |

**Table `tasks`**

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| id | BIGINT | PK, AUTO_INC | Identifiant unique |
| title | VARCHAR(200) | NOT NULL | Titre de la tâche |
| description | TEXT | NULLABLE | Description détaillée |
| status | ENUM | NOT NULL, DEFAULT `TODO` | `TODO / IN_PROGRESS / DONE / CANCELLED` |
| due_date | TIMESTAMP | NULLABLE | Date d'échéance |
| user_id | BIGINT | FK → users.id | Propriétaire (ManyToOne) |
| created_at | TIMESTAMP | NOT NULL | Date de création |
| updated_at | TIMESTAMP | NOT NULL | Dernière modification |

**Relations :** Un `User` possède plusieurs `Task` (OneToMany). Chaque tâche appartient à exactement un utilisateur via la clé étrangère `tasks.user_id → users.id`.

### 1.3 Structure REST de l'API

Voir la section **Endpoints REST** ci-dessus.

Choix de versioning via le préfixe `/api/v1` dans l'URL — simple, explicite, lisible dans les logs, compatible avec tous les clients sans gestion d'en-têtes spéciaux.

### 1.4 Architecture applicative Spring Boot — Principes SOLID

**Séparation des responsabilités (SRP) :**
- `Controller` : reçoit la requête HTTP, délègue au service, retourne la réponse. Aucune logique métier.
- `Service` (interface + impl) : toute la logique métier. Séparé en interface pour respecter l'OCP.
- `Repository` : accès données uniquement, via Spring Data JPA.
- `Security` : filtre JWT totalement découplé du reste de l'application.
- `DTO` : les entités JPA ne sont jamais exposées directement à l'API.

**Principe OCP (Open/Closed) :** Les services sont définis par des interfaces (`AuthService`, `TaskService`). L'implémentation peut être changée sans modifier les contrôleurs.

**Principe DIP (Dependency Inversion) :** Les contrôleurs dépendent des interfaces de service, pas des implémentations concrètes. Spring IoC injecte les dépendances.

**Justification des choix :**
- **JWT stateless** : idéal pour une API REST, scalable horizontalement, pas de session à partager entre instances
- **Spring Data JPA** : abstraction de la couche SQL, pagination native, JPQL pour les requêtes complexes
- **BCryptPasswordEncoder** : hachage sécurisé avec sel aléatoire automatique, résistant aux rainbow tables
- **`@RestControllerAdvice`** : gestion centralisée de toutes les erreurs en un seul endroit (SRP)
- **`ApiResponse<T>` générique** : structure de réponse cohérente sur tous les endpoints

---

## Question 2 — Réalisation, Qualité et Sécurité

### 2.1 Authentification sécurisée (JWT)

Le flux d'authentification fonctionne ainsi :

1. Le client envoie `POST /auth/login` avec email + mot de passe
2. `AuthServiceImpl` délègue à `AuthenticationManager` qui vérifie les credentials via BCrypt
3. En cas de succès, `JwtService.generateToken()` crée un token signé avec HMAC-SHA256
4. Le token est retourné au client
5. Pour chaque requête suivante, `JwtAuthenticationFilter` extrait et valide le token avant d'autoriser l'accès

```bash
# Inscription
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jean","lastName":"Dupont","email":"jean@test.com","password":"motdepasse8"}'

# Connexion
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jean@test.com","password":"motdepasse8"}'

# Utiliser le token retourné
curl http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 2.2 Tests automatisés

Les tests couvrent trois niveaux :

**Tests unitaires (`TaskServiceTest`)** — isolent la logique métier avec Mockito :
- Création normale → retourne `TaskResponse`
- Tâche inexistante → lève `ResourceNotFoundException`
- Accès à une tâche d'un autre utilisateur → lève `AccessDeniedException`
- Suppression → appel vérifié sur le repository
- Mise à jour → retourne la tâche modifiée

**Tests Feature (`TaskControllerTest`)** — testent la couche HTTP avec `@WebMvcTest` + `MockMvc` :
- Création avec données valides → HTTP 201
- Création sans titre → HTTP 400 (validation)
- Requête sans token → HTTP 401
- Listing avec pagination → HTTP 200

**Tests d'authentification (`AuthTest`)** :
- Inscription valide → 201 avec token
- Email invalide → 400
- Connexion valide → 200 avec token
- Champs manquants → 400

```bash
mvn test jacoco:report
# La couverture minimale de 60% est enforced dans pom.xml
# Le build échoue automatiquement si la couverture est inférieure
```

### 2.3 Bonnes pratiques de sécurité

**Validation stricte des entrées :** Toutes les propriétés des DTOs sont annotées (`@NotBlank`, `@Email`, `@Size`, `@FutureOrPresent`). En cas de violation, `GlobalExceptionHandler` retourne un HTTP 400 avec le détail champ par champ.

**Protection XSS :** La méthode `sanitize()` dans `TaskServiceImpl` supprime toutes les balises HTML des champs titre et description avant persistance en base.

**Protection CSRF :** Désactivée intentionnellement — une API REST stateless avec JWT ne nécessite pas de protection CSRF (le token n'est pas stocké dans un cookie).

**Headers de sécurité HTTP :**
- `X-XSS-Protection: 1; mode=block`
- `Content-Security-Policy: default-src 'self'`
- `X-Frame-Options: DENY`

**Gestion des rôles et isolation des données :** `@EnableMethodSecurity` permet l'usage de `@PreAuthorize`. L'isolation est enforced dans `findTaskOwnedBy()` : si l'utilisateur n'est pas propriétaire de la tâche demandée, une `AccessDeniedException` est levée (→ HTTP 403).

### 2.4 Clean Code

- **Méthodes courtes** : chaque méthode a une responsabilité unique (< 20 lignes en moyenne)
- **Nommage explicite** : `buildUser()`, `buildAuthResponse()`, `findTaskOwnedBy()` — le nom décrit l'intention
- **Pas de duplication** : `findUserByEmail()` et `sanitize()` sont des méthodes privées partagées dans le service
- **Commentaires pertinents** : Javadoc sur chaque classe, commentaires sur les choix non évidents

**Améliorations clés :**
- `TaskResponse.from(Task)` — factory method statique qui centralise le mapping entité → DTO
- `ApiResponse<T>` générique — évite de répéter la structure de réponse dans chaque contrôleur
- Interfaces de service — permettent les tests unitaires avec Mockito sans démarrer Spring

---

## Question 3 — Intégration Continue (CI)

### 3.1 Pipeline GitHub Actions

Le fichier `.github/workflows/ci-cd.yml` définit 4 jobs interdépendants :

```
push main/develop
       │
       ▼
  1. test-and-analyze   → Tests + JaCoCo + SonarQube
       │
       ▼
  2. security-scan      → OWASP + TruffleHog
       │
       ▼
  3. deploy             → Build image + Push Docker Hub + Trivy
```

### 3.2 Configuration des secrets GitHub

Dans `Settings > Secrets and variables > Actions` :

| Secret | Valeur |
|--------|--------|
| `SONAR_HOST_URL` | URL de votre SonarQube |
| `SONAR_TOKEN` | Token généré dans SonarQube > Account > Security |
| `DOCKERHUB_USERNAME` | Identifiant Docker Hub |
| `DOCKERHUB_TOKEN` | Token d'accès Docker Hub |
| `DEPLOY_HOST` | IP ou hostname du serveur de production |
| `DEPLOY_USER` | Utilisateur SSH |
| `DEPLOY_SSH_KEY` | Contenu complet de la clé privée SSH |
**NB :** les secrets DEPLOY_HOST, DEPLOY_USER, DEPLOY_SSH_KEY ne sont pas inséré dans le pipeline ils servirons plus tard pour héberger l'API
### 3.3 Optimisations du pipeline

**Cache Maven** — La clé de cache est basée sur le hash du `pom.xml`. Si les dépendances n'ont pas changé, elles ne sont pas re-téléchargées. Gain typique : 2 à 4 min par build.

**Cache Docker Buildx** — Les layers intermédiaires sont mis en cache dans GitHub Actions. Le `Dockerfile` est structuré pour en profiter : `COPY pom.xml` + `mvn dependency:go-offline` AVANT `COPY src/`. Si seul le code source change, les dépendances ne sont pas re-téléchargées dans l'image.

**`fetch-depth: 0`** — Requis par SonarQube pour analyser l'historique Git complet et calculer les métriques sur le nouveau code uniquement.

---

## Question 4 — Analyse Qualité avec SonarQube

### 4.1 Mise en place

```bash
# 1. Démarrer SonarQube
docker-compose up -d sonardb sonarqube
# Attendre ~2 minutes, puis ouvrir http://localhost:9000

# 2. Se connecter avec admin / admin et changer le mot de passe

# 3. Créer le projet
#    Create Project > Manually
#    Project key : deployfast_task-manager
#    Display name : Task Manager DeployFast

# 4. Générer un token
#    Account (en haut à droite) > Security > Generate Token
#    Copier le token

# 5. Lancer l'analyse
mvn clean test jacoco:report sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<votre_token>
```

### 4.2 Interpréter les rapports SonarQube

**Quality Gate** — Verdict global (Passed / Failed). Conditions par défaut :
- Couverture ≥ 80% sur le nouveau code
- Zéro nouveau bug BLOCKER ou CRITICAL
- Zéro nouvelle vulnérabilité
- Duplication ≤ 3%

**Catégories de problèmes :**
- **BUGS** : erreurs pouvant causer un comportement incorrect → priorité absolue
- **VULNERABILITIES** : failles exploitables → corriger avant toute mise en production
- **CODE SMELLS** : problèmes de maintenabilité → n'empêchent pas le fonctionnement mais dégradent la lisibilité
- **SECURITY HOTSPOTS** : code à relire manuellement pour évaluer le risque réel

**Navigation dans l'interface :**
- Dashboard : vue globale Quality Gate + métriques clés
- Issues > Bugs/Security : problèmes à corriger en priorité
- Measures > Coverage : identifier les fichiers sous 60%
- Activity : historique des analyses et tendance de la qualité

### 4.3 Corriger une vulnérabilité détectée

1. Cliquer sur la vulnérabilité dans SonarQube pour voir le contexte
2. Lire l'onglet **"Why is this an issue?"** pour comprendre le risque
3. Consulter **"How to fix it"** pour voir l'exemple de code sécurisé
4. Corriger dans le code, commiter, pousser
5. Le pipeline relance l'analyse automatiquement

> **Note :** Si c'est un faux positif, utiliser **"Won't Fix"** avec une justification obligatoire. Ne jamais marquer Won't Fix sans explication documentée.

### 4.4 Scans de sécurité DevSecOps intégrés

- **OWASP Dependency Check** : analyse les dépendances Maven pour détecter les CVE connues. Le build échoue si un CVE de score CVSS ≥ 7 est trouvé.
- **TruffleHog** : détecte les secrets (tokens, clés API, mots de passe) accidentellement commités dans le code.
- **Trivy** : scanne l'image Docker finale à la recherche de vulnérabilités dans les packages système de l'image de base Alpine.

---

## Question 5 — Déploiement Automatique

### 5.1 Build et tag automatique de l'image

Le job `build-docker` utilise `docker/metadata-action` pour générer les tags automatiquement :

| Branche / Événement | Tags générés |
|---------------------|--------------|
| `main` | `latest` + `sha-<commit>` |
| `develop` | `develop` + `sha-<commit>` |
| Tag git `v1.2.0` | `1.2.0` (semver) |
| Pull Request | `sha-<commit>` uniquement (pas de push) |

### 5.2 Déploiement via docker-compose

Le job `deploy` se connecte en SSH au serveur de production et exécute :

```bash
docker pull deployfast/task-manager:latest   # 1. Nouvelle image
docker-compose down app                       # 2. Arrêt (sans toucher la DB)
docker-compose up -d app                      # 3. Démarrage avec la nouvelle image
curl -f http://localhost:8080/.../health      # 4. Vérification de santé
docker image prune -f                         # 5. Nettoyage des vieilles images
```

La directive `restart: unless-stopped` dans `docker-compose.yml` assure le redémarrage automatique en cas de crash ou reboot du serveur.

### 5.3 Préparer le serveur de production

```bash
# Sur le serveur cible
mkdir -p /opt/task-manager && cd /opt/task-manager

# Déposer docker-compose.yml et créer le fichier .env
cat > .env << EOF
DB_PASSWORD=MotDePasseSecurise
JWT_SECRET=VotreCle64BytesEnBase64
EOF

# Premier démarrage
docker-compose up -d postgres app
```

### 5.4 Environnement staging (bonus)

Pour ajouter un staging sur `develop`, dupliquer le job `deploy` avec :
```yaml
staging:
  needs: build-docker
  if: github.ref == 'refs/heads/develop'
  environment: staging
  # → serveur staging distinct, port 8081
```

---

## Question 6 — Optimisation & Clean Code

### 6.1 Modularité

Chaque couche dépend uniquement de l'interface de la couche inférieure :

```
Controller → interface Service → interface Repository → JPA / DB
```

Les modules sont découplés : l'implémentation de `TaskService` peut être remplacée sans toucher au `TaskController`. Spring IoC gère toutes les instanciations via `@RequiredArgsConstructor`.

### 6.2 Optimisation du pipeline

**Cache des dépendances Maven :**
```yaml
key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```
Si le `pom.xml` ne change pas, les ~150 MB de dépendances ne sont pas re-téléchargés.

**Ordre des instructions dans le Dockerfile :**
```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline    # mis en cache si pom.xml inchangé
COPY src ./src
RUN mvn clean package            # seule cette étape re-tourne si le code change
```

**Parallélisation possible :** Les jobs `test-and-analyze` et `security-scan` peuvent être rendus indépendants pour tourner en parallèle. Gain estimé : 3 à 5 minutes.

### 6.3 Guide opérationnel

**Exécuter le pipeline :**
- Automatiquement à chaque `git push` sur `main` ou `develop`
- Manuellement : GitHub > Actions > CI/CD Pipeline > **Run workflow**
- Localement (tests) : `mvn test`
- Localement (Docker) : `docker build -t deployfast/task-manager:test .`

**Corriger un échec :**

| Symptôme | Action corrective |
|----------|-------------------|
| Tests échouent | Logs du job > corriger le test ou le code |
| Couverture < 60% | Ajouter des tests sur les classes non couvertes |
| Quality Gate SonarQube | Corriger les bugs/vulnérabilités reportés avant de re-pusher |
| Build Docker échoue | Tester localement avec `docker build -t test .` |
| Déploiement SSH échoue | Vérifier le secret `DEPLOY_SSH_KEY`, tester la connexion SSH manuellement |
| Trivy CVE critique | Mettre à jour l'image de base (`eclipse-temurin:17-jre-alpine`) ou la dépendance concernée |

**Commandes de débogage :**
```bash
# Logs de l'application
docker-compose logs -f app

# Health check manuel
curl http://localhost:8080/api/v1/actuator/health

# Entrer dans le conteneur
docker exec -it taskmanager-app sh

# Consommation de ressources
docker stats

# Redémarrer sans rebuild
docker-compose restart app

# Rebuild complet
docker-compose up -d --build app
```

---

> *« Il n'y a que dans le dictionnaire que réussite vient avant travail »* — Pierre Fonerod
