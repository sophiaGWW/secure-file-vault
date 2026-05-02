import { useState } from 'react';
import { uploadFile } from '../api/fileApi.js';

const ALLOWED_CONTENT_TYPES = ['application/pdf'];
const MAX_FILE_SIZE = 50 * 1024 * 1024;

function FileUpload({ onUploadComplete }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);

  function handleFileChange(event) {
    const file = event.target.files[0];
    setSelectedFile(file || null);
    setMessage('');
    setError('');
  }

  async function handleUpload() {
    if (!selectedFile) {
      setError('Please choose a file first.');
      return;
    }

    const validationError = validateFile(selectedFile);
    if (validationError) {
      setError(validationError);
      return;
    }

    setUploading(true);
    setMessage('');
    setError('');

    try {
      await uploadFile(selectedFile);
      setSelectedFile(null);
      setMessage('Upload completed.');
      await onUploadComplete();
    } catch (uploadError) {
      setError(uploadError.message);
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="upload-box">
      <input
        className="file-input"
        type="file"
        accept="application/pdf"
        onChange={handleFileChange}
      />

      {selectedFile && (
        <div className="file-preview">
          <div>
            <span className="preview-label">Name</span>
            <strong>{selectedFile.name}</strong>
          </div>
          <div>
            <span className="preview-label">Type</span>
            <strong>{selectedFile.type || 'unknown'}</strong>
          </div>
          <div>
            <span className="preview-label">Size</span>
            <strong>{formatFileSize(selectedFile.size)}</strong>
          </div>
        </div>
      )}

      {message && <p className="success-message">{message}</p>}
      {error && <p className="error-message">{error}</p>}

      <button type="button" onClick={handleUpload} disabled={uploading}>
        {uploading ? 'Uploading...' : 'Upload'}
      </button>
    </div>
  );
}

function validateFile(file) {
  if (!file.name) {
    return 'Filename is required.';
  }

  if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
    return 'Only PDF files are allowed.';
  }

  if (file.size > MAX_FILE_SIZE) {
    return 'File size must not exceed 50MB.';
  }

  return '';
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

export default FileUpload;
