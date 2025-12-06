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

# **6. Intégration Backend – Spring Boot + Keycloak (OAuth2 / OIDC)**

Cette section décrit la mise en place de la sécurité OAuth2 Resource Server dans l’API Spring Boot, en utilisant les tokens JWT émis par Keycloak. Le backend expose une API `/courses` sécurisée selon les rôles définis dans Keycloak.

---

## **6.1. Dépendances Maven**

Assure-toi d’avoir les dépendances suivantes :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## **6.2. Configuration application.yml**

Le backend vérifie automatiquement les tokens JWT via l’`issuer-uri` correspondant au realm Keycloak :

```yaml
spring:
  application:
    name: APISecurise
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/elearning-realm

server:
  port: 8081
```

---

## **6.3. Gestion des rôles Keycloak dans Spring Boot**

Keycloak envoie les rôles dans le claim `realm_access.roles`.
On convertit ces rôles en autorités Spring Security (`ROLE_ADMIN`, `ROLE_STUDENT`, etc.)

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        var realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();

        var roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
    });

    return converter;
}
```

---

## **6.4. Configuration de la sécurité (SecurityConfig)**

Spring Boot est configuré comme **Resource Server**, c’est-à-dire qu’il valide les JWT fournis par Keycloak.

### ✔ Authentification des endpoints

### ✔ Activation des annotations `@PreAuthorize`

### ✔ Gestion complète du CORS pour le frontend React

### ✔ Autorisation OPTIONS (indispensable pour Vite / React)

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            );

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
```

---

## **6.5. API sécurisée : CourseController**

L’accès à l’API est protégé par des rôles.
L’utilisateur doit être authentifié et avoir :

* ROLE_STUDENT ou ROLE_ADMIN → GET /courses
* ROLE_ADMIN → POST /courses

```java
@RestController
@RequestMapping("/courses")
public class CourseController {

    private final List<Map<String,String>> courses = new ArrayList<>(List.of(
        Map.of("id","1","title","Spring Boot Basics"),
        Map.of("id","2","title","React for Beginners")
    ));

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public List<Map<String,String>> getCourses() {
        return courses;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,String> addCourse(@RequestBody Map<String,String> course) {
        courses.add(course);
        return course;
    }
}
```

---

## **6.6. Résultat**

Grâce à cette configuration :

| Endpoint                    | Rôle requis                    | Fonction               |
| --------------------------- | ------------------------------ | ---------------------- |
| `GET /courses`              | `ROLE_STUDENT` ou `ROLE_ADMIN` | Liste les cours        |
| `POST /courses`             | `ROLE_ADMIN`                   | Ajoute un cours        |
| JWT vérifié automatiquement | —                              | Signature + expiration |

---


# **8. Intégration du Frontend React avec Keycloak**

Le frontend est développé avec **React + Vite**, et utilise **Keycloak JavaScript Adapter** pour gérer l’authentification et sécuriser les appels API.

---

## **8.1. Initialisation de Keycloak**

Un fichier `keycloak.js` initialise le SDK et déclenche la connexion automatique :

```js
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'elearning-realm',
    clientId: 'react-client'
});

export default keycloak;
```

---

## **8.2. Fonction apiFetch (Appels API sécurisés)**

Tous les appels backend passent par `apiFetch.js`, qui :

✔ rafraîchit automatiquement le token
✔ ajoute le header Authorization
✔ redirige vers login si token expiré
✔ gère 401 / 403

```js
import keycloak from './keycloak';

export async function apiFetch(url, options = {}) {
    if (!options.headers) options.headers = {};

    await keycloak.updateToken(30).catch(() => keycloak.login());
    options.headers['Authorization'] = 'Bearer ' + keycloak.token;

    const res = await fetch(url, options);

    if (res.status === 401) {
        keycloak.login();
        throw new Error('Token invalide (401)');
    }
    if (res.status === 403) {
        throw new Error('Accès interdit (403)');
    }

    return res;
}
```

---

## **8.3. Composant App.jsx**

Le composant principal :

✔ charge le profil utilisateur (nom, email)
✔ affiche un bouton logout
✔ inclut la liste des cours

```jsx
function App({ keycloak }) {
    const [profile, setProfile] = useState(null);

    useEffect(() => {
        keycloak.loadUserInfo().then(setProfile);
    }, []);

    const logout = () => keycloak.logout({ redirectUri: window.location.origin });

    return (
        <div className="container mt-4">
            <header className="d-flex justify-content-between align-items-center mb-4 p-3 bg-light rounded shadow-sm">
                {profile && (
                    <div>
                        👋 Bonjour {profile.given_name} {profile.family_name}
                        <span className="text-muted"> ({profile.email})</span>
                    </div>
                )}
                <button className="btn btn-outline-danger" onClick={logout}>Logout</button>
            </header>

            <main>
                <CoursesList keycloak={keycloak} />
            </main>
        </div>
    );
}
```

---

## **8.4. Composant CoursesList (Affichage des cours)**

✔ récupère les cours depuis Spring Boot
✔ n’affiche la partie admin que si l’utilisateur a `ROLE_ADMIN`

```jsx
export default function CoursesList() {
    const [courses, setCourses] = useState([]);

    const loadCourses = () => {
        apiFetch('http://localhost:8081/courses')
            .then(res => res.json())
            .then(setCourses)
            .catch(console.error);
    };

    useEffect(() => {
        loadCourses();
    }, []);

    return (
        <div className="container mt-4">
            <h2 className="mb-3">📚 Cours disponibles</h2>

            <ul className="list-group mb-4">
                {courses.map(c => (
                    <li key={c.id} className="list-group-item">
                        {c.title}
                    </li>
                ))}
            </ul>

            {keycloak?.hasRealmRole('ROLE_ADMIN') && (
                <div className="card shadow-sm p-3">
                    <AdminCourses onCourseAdded={loadCourses} />
                </div>
            )}
        </div>
    );
}
```

---

## **8.5. Composant AdminCourses (Gestion des cours – Admin seulement)**

✔ permet d’ajouter un cours via POST `/courses`
✔ notifie CoursesList pour recharger les données

```jsx
export default function AdminCourses({ onCourseAdded }) {
    const [title, setTitle] = useState('');

    const addCourse = async () => {
        await apiFetch('http://localhost:8081/courses', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: Date.now().toString(), title })
        });

        setTitle('');
        onCourseAdded();
    };

    return (
        <div className="container mt-3">
            <h2 className="mb-3">🛠️ Gestion des cours (Admin)</h2>

            <div className="input-group mb-3">
                <input
                    className="form-control"
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                    placeholder="Titre du cours"
                />
                <button className="btn btn-primary" onClick={addCourse}>
                    Ajouter
                </button>
            </div>
        </div>
    );
}
```

---

## **8.6. Gestion des rôles dans le frontend**

Keycloak expose les rôles via :

```js
keycloak.hasRealmRole('ROLE_ADMIN')
```

Ce mécanisme permet de :

✔ afficher / masquer la gestion des cours
✔ contrôler l’UI côté frontend
✔ renforcer la sécurité avec Spring Boot côté backend

---

## **8.7. Résultat final**

🎯 Le frontend est maintenant entièrement sécurisé :

* Authentification via Keycloak
* Token JWT ajouté automatiquement
* Renouvellement automatique du token
* Appels API protégés
* UI dynamique selon le rôle (`STUDENT` / `ADMIN`)
* Possibilité pour l’admin d’ajouter de nouveaux cours

