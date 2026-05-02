import { useEffect, useState } from 'react';
import { listFiles } from '../api/fileApi.js';
import FileTable from './FileTable.jsx';
import FileUpload from './FileUpload.jsx';

function FileDashboard({ currentUser, onLogout }) {
  // Dashboard 全体で表示するファイル一覧と読み込み状態を管理する。
  const [files, setFiles] = useState([]);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [fileListError, setFileListError] = useState('');

  useEffect(() => {
    // 初回表示時に現在ユーザーのファイル一覧を取得する。
    refreshFiles();
  }, []);

  async function refreshFiles() {
    // アップロード・削除後にも再利用する一覧再取得処理。
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
          <p className="eyebrow">セキュアファイル保管庫</p>
          <h1>ファイル管理</h1>
        </div>
        <div className="user-actions">
          <span>{currentUser.username}</span>
          <button type="button" onClick={onLogout}>
            ログアウト
          </button>
        </div>
      </header>

      <section className="panel">
        <h2>アップロード</h2>
        <FileUpload onUploadComplete={refreshFiles} />
      </section>

      <section className="panel">
        <h2>ファイル一覧</h2>
        {fileListError && <p className="error-message">{fileListError}</p>}
        <FileTable files={files} loading={loadingFiles} onFilesChanged={refreshFiles} />
      </section>
    </main>
  );
}

export default FileDashboard;
