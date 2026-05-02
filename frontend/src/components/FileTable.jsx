import { useState } from 'react';
import { deleteFile, downloadFile } from '../api/fileApi.js';

function FileTable({ files, loading, onFilesChanged }) {
  const [busyFileId, setBusyFileId] = useState(null);
  const [error, setError] = useState('');

  if (loading) {
    return <p className="muted-text">Loading files...</p>;
  }

  async function handleDownload(file) {
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
      URL.revokeObjectURL(downloadUrl);
    } catch (downloadError) {
      setError(downloadError.message);
    } finally {
      setBusyFileId(null);
    }
  }

  async function handleDelete(file) {
    if (!window.confirm(`Delete ${file.originalFilename}?`)) {
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

  return (
    <>
      {error && <p className="error-message table-message">{error}</p>}
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
          {files.length === 0 ? (
            <tr>
              <td colSpan="6">No files yet.</td>
            </tr>
          ) : (
            files.map((file) => (
              <tr key={file.id}>
                <td>{file.originalFilename}</td>
                <td>{file.contentType}</td>
                <td>{formatFileSize(file.fileSize)}</td>
                <td>
                  <span className={`status-badge status-${file.status.toLowerCase()}`}>
                    {file.status}
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
                      Download
                    </button>
                    <button
                      type="button"
                      className="danger-button"
                      onClick={() => handleDelete(file)}
                      disabled={busyFileId === file.id}
                    >
                      Delete
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
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

export default FileTable;
