import { useState } from 'react';

function RegisterPage({ error, onRegister, onError, onGoToLogin }) {
  const [form, setForm] = useState({ username: '', password: '' });
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    try {
      await onRegister(form);
    } catch (requestError) {
      onError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="center-page">
      <section className="auth-card">
        <p className="eyebrow">Secure File Vault</p>
        <h1>Register</h1>
        <form onSubmit={handleSubmit} className="form-stack">
          <label>
            Username
            <input
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              autoComplete="username"
              required
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
          {error && <p className="error-message">{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? 'Creating account...' : 'Register'}
          </button>
        </form>
        <button className="link-button" type="button" onClick={onGoToLogin}>
          Back to login
        </button>
      </section>
    </main>
  );
}

export default RegisterPage;
