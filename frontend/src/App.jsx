import { useEffect, useState } from 'react';
import { getCurrentUser, login, register } from './api/authApi.js';
import FileDashboard from './components/FileDashboard.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';

const TOKEN_KEY = 'secure_file_vault_token';

function App() {
  const [page, setPage] = useState('login');
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(token));
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) {
      setCurrentUser(null);
      setLoading(false);
      return;
    }

    getCurrentUser(token)
      .then((user) => {
        setCurrentUser(user);
        setPage('dashboard');
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        setToken(null);
        setCurrentUser(null);
        setPage('login');
      })
      .finally(() => setLoading(false));
  }, [token]);

  async function handleLogin(form) {
    setError('');
    const response = await login(form);
    saveLogin(response);
  }

  async function handleRegister(form) {
    setError('');
    const response = await register(form);
    saveLogin(response);
  }

  function saveLogin(response) {
    localStorage.setItem(TOKEN_KEY, response.token);
    setToken(response.token);
    setCurrentUser(response.user);
    setPage('dashboard');
  }

  function handleLogout() {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setCurrentUser(null);
    setPage('login');
  }

  if (loading) {
    return (
      <main className="center-page">
        <div className="auth-card">Loading...</div>
      </main>
    );
  }

  if (token && currentUser) {
    return <FileDashboard currentUser={currentUser} onLogout={handleLogout} />;
  }

  if (page === 'register') {
    return (
      <RegisterPage
        error={error}
        onRegister={handleRegister}
        onError={setError}
        onGoToLogin={() => {
          setError('');
          setPage('login');
        }}
      />
    );
  }

  return (
    <LoginPage
      error={error}
      onLogin={handleLogin}
      onError={setError}
      onGoToRegister={() => {
        setError('');
        setPage('register');
      }}
    />
  );
}

export default App;
