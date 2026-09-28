import { formatDualDate } from "../../utils/ethiopianCalendar";
// src/components/Documents/DocumentList.jsx
import { FileText, Trash2 } from 'lucide-react';
import { documentsApi } from '../../services/api';

export default function DocumentList({ documents, onDelete, isAdmin }) {
  const handleDownload = async (doc) => {
    try {
      await documentsApi.download(doc.id, doc.fileName);
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
            <p className="text-xs text-slate-400">Uploaded: {formatDualDate(doc.uploadedAt)}</p>
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