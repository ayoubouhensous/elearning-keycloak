import React, { useEffect, useState } from 'react';
import { apiFetch } from './api';
import AdminCourses from './AdminCourses';
import keycloak from "./keycloak.js";

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
                    <li key={c.id} className="list-group-item d-flex justify-content-between align-items-center">
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
