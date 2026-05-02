import { useEffect, useState } from 'react';
import { listFiles } from '../api/fileApi.js';
import FileTable from './FileTable.jsx';
import FileUpload from './FileUpload.jsx';

function FileDashboard({ currentUser, onLogout }) {
  const [files, setFiles] = useState([]);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [fileListError, setFileListError] = useState('');

  useEffect(() => {
    refreshFiles();
  }, []);

  async function refreshFiles() {
    setFileListError('');
    setLoadingFiles(true);
    try {
      const fileList = await listFiles();
      setFiles(fileList);
    } catch (error) {
      setFileListError(error.message);
    } finally {
      setLoadingFiles(false);
    }
  }

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
        <FileUpload onUploadComplete={refreshFiles} />
      </section>

      <section className="panel">
        <h2>Files</h2>
        {fileListError && <p className="error-message">{fileListError}</p>}
        <FileTable files={files} loading={loadingFiles} onFilesChanged={refreshFiles} />
      </section>
    </main>
  );
}

export default FileDashboard;
