function FileDashboard({ currentUser, onLogout }) {
  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Secure File Vault</p>
          <h1>File Dashboard</h1>
        </div>
        <div className="user-actions">
          <span>{currentUser.username}</span>
          <button type="button" onClick={onLogout}>
            Logout
          </button>
        </div>
      </header>

      <section className="panel">
        <h2>Upload</h2>
        <p>
          File upload and S3 Presigned URL flow will be implemented in the next phase.
        </p>
      </section>

      <section className="panel">
        <h2>Files</h2>
        <table>
          <thead>
            <tr>
              <th>Filename</th>
              <th>Type</th>
              <th>Size</th>
              <th>Status</th>
              <th>Created At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan="6">No files yet.</td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>
  );
}

export default FileDashboard;
