import React, { useState } from 'react';
import { apiFetch } from './api';

export default function AdminCourses({ onCourseAdded }) {
    const [title, setTitle] = useState('');

    const addCourse = async () => {
        await apiFetch('http://localhost:8081/courses', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: Date.now().toString(), title })
        });

        setTitle('');

        // ⬅️ Notifier CoursesList qu'un nouveau cours a été ajouté
        onCourseAdded();
    };

    return (
        <div className="container mt-3">
            <h2 className="mb-3">🛠️ Gestion des cours (Admin)</h2>

            <div className="input-group mb-3">
                <input
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                    placeholder="Titre du cours"
                    className="form-control"
                />
                <button onClick={addCourse} className="btn btn-primary">
                    Ajouter
                </button>
            </div>
        </div>
    );
}
