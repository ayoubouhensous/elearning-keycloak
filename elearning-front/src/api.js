import keycloak from './keycloak';

export async function apiFetch(url, options = {}) {
    if (!options.headers) options.headers = {};
    // Rafraîchir le token si proche de l’expiration
    await keycloak.updateToken(30).catch(() => keycloak.login());
    options.headers['Authorization'] = 'Bearer ' + keycloak.token;

    console.log(keycloak.token)
    console.log(options)
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
