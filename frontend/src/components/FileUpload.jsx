import { useRef, useState } from 'react';
import { uploadFile } from '../api/fileApi.js';

const ALLOWED_CONTENT_TYPES = ['application/pdf'];
const MAX_FILE_SIZE = 500 * 1024 * 1024;

function FileUpload({ onUploadComplete }) {
  // 選択中ファイル、成功メッセージ、エラー、送信中状態を管理する。
  const fileInputRef = useRef(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);

  function handleFileChange(event) {
    // ファイル選択が変わったら、前回のメッセージをクリアする。
    const file = event.target.files[0];
    setSelectedFile(file || null);
    setMessage('');
    setError('');
  }

  async function handleUpload() {
    // 送信前にクライアント側でも最低限の validation を行う。
    if (!selectedFile) {
      setError('先にファイルを選択してください。');
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
      // 実際の S3 保存はバックエンド側で行う。
      await uploadFile(selectedFile);
      setSelectedFile(null);
      setMessage('アップロードが完了しました。');
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
        ref={fileInputRef}
        className="hidden-file-input"
        type="file"
        accept="application/pdf"
        onChange={handleFileChange}
      />
      <div className="file-picker">
        <button type="button" className="secondary-button" onClick={() => fileInputRef.current?.click()}>
          ファイルを選択
        </button>
        <span>{selectedFile ? selectedFile.name : '未選択'}</span>
      </div>

      {selectedFile && (
        <div className="file-preview">
          <div>
            <span className="preview-label">ファイル名</span>
            <strong>{selectedFile.name}</strong>
          </div>
          <div>
            <span className="preview-label">種類</span>
            <strong>{selectedFile.type || '不明'}</strong>
          </div>
          <div>
            <span className="preview-label">サイズ</span>
            <strong>{formatFileSize(selectedFile.size)}</strong>
          </div>
        </div>
      )}

      {message && <p className="success-message">{message}</p>}
      {error && <p className="error-message">{error}</p>}

      <button type="button" onClick={handleUpload} disabled={uploading}>
        {uploading ? 'アップロード中...' : 'アップロード'}
      </button>
    </div>
  );
}

function validateFile(file) {
  // バックエンドの PDF 制限と同じ条件をフロントエンドでも先に確認する。
  if (!file.name) {
    return 'ファイル名は必須です。';
  }

  if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
    return 'PDF ファイルのみアップロードできます。';
  }

  if (file.size > MAX_FILE_SIZE) {
    return 'ファイルサイズは 500MB 以下にしてください。';
  }

  return '';
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

export default FileUpload;
