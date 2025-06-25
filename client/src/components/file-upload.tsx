'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { FiUpload } from 'react-icons/fi';

import { Input } from './ui/input';

interface FileUploadProps {
  onFileUpload: (file: File) => void;
  isUploading: boolean;
}

export default function FileUpload({ onFileUpload, isUploading }: FileUploadProps) {
  const [dragActive, setDragActive] = useState(false);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onFileUpload(acceptedFiles[0]);
    }
  }, [onFileUpload]);

  const { getRootProps, getInputProps } = useDropzone({
    onDrop,
    multiple: false,
    onDragEnter: () => setDragActive(true),
    onDragLeave: () => setDragActive(false),
    onDropAccepted: () => setDragActive(false),
    onDropRejected: () => setDragActive(false),
  });

  return (
    <div
      {...getRootProps()}
      className={`
        w-full p-8 border-2 border-dashed rounded-lg text-center cursor-pointer transition-all
        ${dragActive
          ? 'border-primary bg-primary/5'
          : 'border-border hover:border-primary/50 hover:bg-muted/50'
        }
        ${isUploading ? 'opacity-50 pointer-events-none' : ''}
      `}
    >
      <Input {...getInputProps()} />
      <div className="flex flex-col items-center justify-center space-y-4">
        <div className="p-4 bg-primary/10 rounded-full">
          <FiUpload className="w-8 h-8 text-primary" />
        </div>
        <div className="space-y-2">
          <p className="text-lg font-semibold text-foreground">Drag & drop a file here, or click to select</p>
          <p className="text-sm text-muted-foreground">
            Share any file with your peers securely
          </p>
        </div>
      </div>
    </div>
  );
}
