import React, { useEffect, useState } from 'react';
import CoursesList from './CoursesList';

function App({ keycloak }) {
    const [profile, setProfile] = useState(null);

    // Charger le profil de l'utilisateur
    useEffect(() => {
        keycloak.loadUserInfo().then(setProfile);
    }, []);

    const logout = () => keycloak.logout({ redirectUri: window.location.origin });

    return (
        <div className="container mt-4">

            <header className="d-flex justify-content-between align-items-center mb-4 p-3 bg-light rounded shadow-sm">
                {profile && (
                    <div className="fw-semibold">
                        👋 Bonjour {profile.given_name} {profile.family_name}
                        <span className="text-muted">({profile.email})</span>
                    </div>
                )}

                <button className="btn btn-outline-danger" onClick={logout}>
                    Logout
                </button>
            </header>

            <main>
                <CoursesList keycloak={keycloak} />
            </main>
        </div>
    );

}

export default App;
