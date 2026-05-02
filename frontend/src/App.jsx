import { useEffect, useState } from 'react';
import { getCurrentUser, login, register } from './api/authApi.js';
import FileDashboard from './components/FileDashboard.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';

const TOKEN_KEY = 'secure_file_vault_token';

function App() {
  // ログイン・登録・ダッシュボードの簡易画面遷移を state で管理する。
  const [page, setPage] = useState('login');
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(token));
  const [error, setError] = useState('');

  useEffect(() => {
    // localStorage に token がある場合、起動時にユーザー情報を復元する。
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
        // token が無効な場合はログイン状態を破棄する。
        localStorage.removeItem(TOKEN_KEY);
        setToken(null);
        setCurrentUser(null);
        setPage('login');
      })
      .finally(() => setLoading(false));
  }, [token]);

  async function handleLogin(form) {
    // LoginPage から受け取った入力を API に渡す。
    setError('');
    const response = await login(form);
    saveLogin(response);
  }

  async function handleRegister(form) {
    // RegisterPage から受け取った入力を API に渡す。
    setError('');
    const response = await register(form);
    saveLogin(response);
  }

  function saveLogin(response) {
    // JWT を保存し、以後の API 呼び出しで Authorization ヘッダーに使う。
    localStorage.setItem(TOKEN_KEY, response.token);
    setToken(response.token);
    setCurrentUser(response.user);
    setPage('dashboard');
  }

  function handleLogout() {
    // ログアウト時は token と現在ユーザーを破棄する。
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setCurrentUser(null);
    setPage('login');
  }

  if (loading) {
    // token 検証中は簡易 loading を表示する。
    return (
      <main className="center-page">
        <div className="auth-card">読み込み中...</div>
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
