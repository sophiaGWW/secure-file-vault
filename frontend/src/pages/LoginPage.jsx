import { useState } from 'react';

function LoginPage({ error, onLogin, onError, onGoToRegister }) {
  // ログインフォームの入力状態と送信中状態を管理する。
  const [form, setForm] = useState({ username: '', password: '' });
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    // form submit の標準遷移を止め、React 側で API 呼び出しを行う。
    event.preventDefault();
    setSubmitting(true);
    try {
      await onLogin(form);
    } catch (requestError) {
      onError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="center-page">
      <section className="auth-card">
        <p className="eyebrow">セキュアファイル保管庫</p>
        <h1>ログイン</h1>
        <form onSubmit={handleSubmit} className="form-stack">
          <label>
            ユーザー名
            <input
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              autoComplete="username"
              required
            />
          </label>
          <label>
            パスワード
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              autoComplete="current-password"
              required
            />
          </label>
          {error && <p className="error-message">{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>
        <button className="link-button" type="button" onClick={onGoToRegister}>
          新規アカウントを作成
        </button>
      </section>
    </main>
  );
}

export default LoginPage;
