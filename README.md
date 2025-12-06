# E-Learning Platform - OAuth2 / OpenID Connect Setup

## 1. Pré-requis
- Docker et Docker Compose installés
- Keycloak 26.4.2
- React (frontend)
- Spring Boot (backend)

---

## 2. Lancer Keycloak avec Docker

```bash
docker-compose up -d
````

**Configuration Docker Compose :**

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.4.2
    command:
      - start-dev
    environment:
      KC_DB: dev-file
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
```

Keycloak sera accessible sur : [http://localhost:8080](http://localhost:8080)
Admin : `admin` / `admin`

---

## 3. Configuration du Realm et du Client

**Via Keycloak Admin Console :**

* **Realm** : `elearning-realm`
* **Client** : `react-client`

  * Type : `public`
  * Standard Flow (OIDC) activé
  * Valid Redirect URI : `http://localhost:3000/*`
  * Web origins : `http://localhost:3000`

---

## 4. Création des rôles

* `ROLE_ADMIN`
* `ROLE_STUDENT`

---

## 5. Création des utilisateurs

| Username | Password | Rôle         |
| -------- | -------- | ------------ |
| user1    | password | ROLE_STUDENT |
| admin1   | password | ROLE_ADMIN   |

---

