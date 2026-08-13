// src/components/Documents/DocumentUpload.jsx
import { useState } from 'react';
import { Upload, X, File, Loader2 } from 'lucide-react';
import { documentsApi } from '../../services/api';

const DOCUMENT_TYPES = [
  { value: 'LEASE_AGREEMENT', label: 'Lease Agreement' },
  { value: 'ID_CARD', label: 'ID Card / Passport' },
  { value: 'BUSINESS_LICENSE', label: 'Business License' },
  { value: 'TIN_CERTIFICATE', label: 'TIN Certificate' },
  { value: 'BANK_DETAILS', label: 'Bank Details' },
  { value: 'OTHER', label: 'Other' },
];

export default function DocumentUpload({ tenantId, onUpload, onClose }) {
  const [type, setType] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState(null);
  const [fileName, setFileName] = useState('');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    if (selected) {
      setFile(selected);
      setFileName(selected.name);
      setError('');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!type) {
      setError('Please select a document type');
      return;
    }
    if (!file) {
      setError('Please select a file');
      return;
    }

    setUploading(true);
    setError('');

    try {
      await documentsApi.uploadForTenant(tenantId, type, description, file);
      onUpload();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to upload document');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-bold text-slate-800">Upload Document</h3>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {/* Document Type */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Document Type *
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
              required
            >
              <option value="">Select type...</option>
              {DOCUMENT_TYPES.map((doc) => (
                <option key={doc.value} value={doc.value}>
                  {doc.label}
                </option>
              ))}
            </select>
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Description (Optional)
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g., Signed lease agreement 2026"
              className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
            />
          </div>

          {/* File Upload */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              File *
            </label>
            <div className="relative">
              <input
                type="file"
                onChange={handleFileChange}
                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
              />
              <div className="border-2 border-dashed border-slate-200 rounded-lg p-6 text-center hover:border-indigo-300 transition">
                {fileName ? (
                  <div className="flex items-center justify-center gap-2 text-slate-700">
                    <File className="w-5 h-5 text-indigo-500" />
                    <span className="text-sm font-medium">{fileName}</span>
                    <button
                      type="button"
                      onClick={() => setFile(null)}
                      className="text-rose-500 hover:text-rose-600"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div>
                    <Upload className="w-8 h-8 mx-auto text-slate-400 mb-2" />
                    <p className="text-sm text-slate-500">
                      Click to upload or drag and drop
                    </p>
                    <p className="text-xs text-slate-400 mt-1">
                      PDF, DOC, JPG, PNG (Max 10MB)
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Submit */}
          <div className="flex gap-3 mt-6">
            <button
              type="submit"
              disabled={uploading}
              className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {uploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin inline mr-2" />
                  Uploading...
                </>
              ) : (
                'Upload Document'
              )}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium rounded-lg transition"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}