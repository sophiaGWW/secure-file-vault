import { useEffect, useState } from 'react';
import { deleteFile, downloadFile } from '../api/fileApi.js';

function FileTable({ files, loading, onFilesChanged }) {
  // ダウンロード・削除中の行を識別し、二重クリックを防ぐ。
  const [busyFileId, setBusyFileId] = useState(null);
  const [selectedFileIds, setSelectedFileIds] = useState([]);
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    // 一覧が更新されたとき、存在しなくなった fileId は選択状態から外す。
    const currentFileIds = new Set(files.map((file) => file.id));
    setSelectedFileIds((currentIds) => currentIds.filter((fileId) => currentFileIds.has(fileId)));
  }, [files]);

  if (loading) {
    return <p className="muted-text">ファイルを読み込み中...</p>;
  }

  const selectableFiles = files.filter((file) => file.status !== 'DELETED');
  const allSelected =
    selectableFiles.length > 0 && selectedFileIds.length === selectableFiles.length;

  async function handleDownload(file) {
    // Blob URL を一時的に作成し、ブラウザのダウンロードを開始する。
    setBusyFileId(file.id);
    setError('');

    try {
      const blob = await downloadFile(file.id);
      const downloadUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = file.originalFilename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      // 一時 URL は使い終わったら解放する。
      URL.revokeObjectURL(downloadUrl);
    } catch (downloadError) {
      setError(downloadError.message);
    } finally {
      setBusyFileId(null);
    }
  }

  async function handleDelete(file) {
    // 誤削除を避けるため、削除前にブラウザ確認を挟む。
    if (!window.confirm(`${file.originalFilename} を削除しますか？`)) {
      return;
    }

    setBusyFileId(file.id);
    setError('');

    try {
      await deleteFile(file.id);
      await onFilesChanged();
    } catch (deleteError) {
      setError(deleteError.message);
    } finally {
      setBusyFileId(null);
    }
  }

  function handleSelectFile(fileId, checked) {
    setSelectedFileIds((currentIds) => {
      if (checked) {
        return currentIds.includes(fileId) ? currentIds : [...currentIds, fileId];
      }
      return currentIds.filter((currentId) => currentId !== fileId);
    });
  }

  function handleSelectAll(checked) {
    setSelectedFileIds(checked ? selectableFiles.map((file) => file.id) : []);
  }

  async function handleBulkDelete() {
    if (selectedFileIds.length === 0) {
      setError('削除するファイルを選択してください。');
      return;
    }

    if (!window.confirm(`選択した ${selectedFileIds.length} 件のファイルを一括削除しますか？`)) {
      return;
    }

    setBulkDeleting(true);
    setError('');

    try {
      await Promise.all(selectedFileIds.map((fileId) => deleteFile(fileId)));
      setSelectedFileIds([]);
      await onFilesChanged();
    } catch (deleteError) {
      setError(deleteError.message);
    } finally {
      setBulkDeleting(false);
    }
  }

  return (
    <>
      {error && <p className="error-message table-message">{error}</p>}
      <div className="table-toolbar">
        <p className="muted-text">
          {selectedFileIds.length > 0
            ? `${selectedFileIds.length} 件選択中`
            : '削除対象を選択できます。'}
        </p>
        <button
          type="button"
          className="danger-button"
          onClick={handleBulkDelete}
          disabled={selectedFileIds.length === 0 || bulkDeleting}
        >
          {bulkDeleting ? '一括削除中...' : '一括削除'}
        </button>
      </div>
      <table>
        <thead>
          <tr>
            <th>
              <input
                className="row-checkbox"
                type="checkbox"
                aria-label="すべて選択"
                checked={allSelected}
                onChange={(event) => handleSelectAll(event.target.checked)}
                disabled={selectableFiles.length === 0 || bulkDeleting}
              />
            </th>
            <th>ファイル名</th>
            <th>所有者ID</th>
            <th>種類</th>
            <th>サイズ</th>
            <th>状態</th>
            <th>作成日時</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {files.length === 0 ? (
            <tr>
              <td colSpan="8">ファイルはまだありません。</td>
            </tr>
          ) : (
            files.map((file) => (
              <tr key={file.id}>
                <td>
                  <input
                    className="row-checkbox"
                    type="checkbox"
                    aria-label={`${file.originalFilename} を選択`}
                    checked={selectedFileIds.includes(file.id)}
                    onChange={(event) => handleSelectFile(file.id, event.target.checked)}
                    disabled={busyFileId === file.id || bulkDeleting || file.status === 'DELETED'}
                  />
                </td>
                <td>{file.originalFilename}</td>
                <td>{file.ownerId}</td>
                <td>{file.contentType}</td>
                <td>{formatFileSize(file.fileSize)}</td>
                <td>
                  <span className={`status-badge status-${file.status.toLowerCase()}`}>
                    {formatStatus(file.status)}
                  </span>
                </td>
                <td>{formatDate(file.createdAt)}</td>
                <td>
                  <div className="table-actions">
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => handleDownload(file)}
                      disabled={busyFileId === file.id}
                    >
                      ダウンロード
                    </button>
                    <button
                      type="button"
                      className="danger-button"
                      onClick={() => handleDelete(file)}
                      disabled={busyFileId === file.id}
                    >
                      削除
                    </button>
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </>
  );
}

function formatFileSize(size) {
  // byte 数を画面表示用の B / KB / MB に変換する。
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(value) {
  // API から日時が返らない場合は "-" として表示する。
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function formatStatus(status) {
  switch (status) {
    case 'AVAILABLE':
      return '利用可能';
    case 'UPLOADING':
      return 'アップロード中';
    case 'FAILED':
      return '失敗';
    case 'DELETED':
      return '削除済み';
    default:
      return status;
  }
}

export default FileTable;
