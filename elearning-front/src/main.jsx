import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import keycloak from './keycloak';
import 'bootstrap/dist/css/bootstrap.min.css'; // ← Import CSS de Bootstrap
import 'bootstrap/dist/js/bootstrap.bundle.min.js'; // ← Optionnel : pour les composants JS de Bootstrap

// Initialisation Keycloak
keycloak.init({
    onLoad: 'login-required',  // force le login dès le démarrage
    checkLoginIframe: false,   // désactive le check iframe (plus simple pour dev)
    pkceMethod: 'S256'         // PKCE sécurisé pour client public
}).then(authenticated => {
    if (!authenticated) {
        keycloak.login();  // redirection Keycloak si pas connecté
    } else {
        ReactDOM.createRoot(document.getElementById('root')).render(
            <React.StrictMode>
                <App keycloak={keycloak} />
            </React.StrictMode>
        );
    }
}).catch(err => {
    console.error('Keycloak init failed', err);
});
