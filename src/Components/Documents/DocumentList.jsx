// src/components/Documents/DocumentList.jsx
import { FileText, Trash2 } from 'lucide-react';
import { API_BASE } from '../../services/api';

export default function DocumentList({ documents, onDelete, isAdmin }) {
  const handleDownload = async (doc) => {
    try {
      const token = localStorage.getItem('access_token') || localStorage.getItem('token');
      const response = await fetch(`${API_BASE}/documents/download/${doc.id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error(`Download failed: ${response.status}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.fileName || 'document';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);

    } catch (error) {
      console.error('Download failed:', error);
      alert('Failed to download document. Please try again.');
    }
  };

  if (!documents || documents.length === 0) {
    return (
      <div className="text-center py-8">
        <FileText className="w-12 h-12 mx-auto text-slate-300 mb-3" />
        <p className="text-slate-500">No documents uploaded yet.</p>
      </div>
    );
  }

  return (
    <div className="divide-y divide-slate-100">
      {documents.map((doc) => (
        <div key={doc.id} className="flex items-center justify-between px-4 py-3 hover:bg-slate-50/60 transition">
          <div>
            <p className="font-medium text-slate-800">{doc.fileName}</p>
            <p className="text-sm text-slate-500">{doc.type}</p>
            {doc.description && <p className="text-sm text-slate-500">{doc.description}</p>}
            <p className="text-xs text-slate-400">Uploaded: {new Date(doc.uploadedAt).toLocaleDateString()}</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => handleDownload(doc)}
              className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition"
            >
              Download
            </button>
            {isAdmin && (
              <button
                onClick={() => onDelete && onDelete(doc.id)}
                className="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg transition"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
