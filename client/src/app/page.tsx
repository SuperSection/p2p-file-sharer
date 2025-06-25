'use client';

import { useState } from 'react';
import axios from 'axios';

import FileUpload from '@/components/file-upload';
import FileDownload from '@/components/file-download';
import InviteCode from '@/components/invite-code';
import { ModeToggle } from '@/components/mode-toggle';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';


export default function Home() {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [port, setPort] = useState<number | null>(null);

  const handleFileUpload = async (file: File) => {
    setUploadedFile(file);
    setIsUploading(true);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axios.post('/api/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      setPort(response.data.port);
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Failed to upload file. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDownload = async (port: number) => {
    setIsDownloading(true);

    try {
      // Request download from Java backend
      const response = await axios.get(`/api/download/${port}`, {
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;

      // Try to get filename from response headers
      // Axios normalizes headers to lowercase, but we need to handle different cases
      const headers = response.headers;
      let contentDisposition = '';

      // Look for content-disposition header regardless of case
      for (const key in headers) {
        if (key.toLowerCase() === 'content-disposition') {
          contentDisposition = headers[key];
          break;
        }
      }

      let filename = 'downloaded-file';

      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch && filenameMatch.length === 2) {
          filename = filenameMatch[1];
        }
      }

      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error downloading file:', error);
      alert('Failed to download file. Please check the invite code and try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <header className="flex justify-between items-center mb-12">
          <div className="text-center flex-1">
            <h1 className="text-4xl font-bold text-primary mb-2">PeerLink</h1>
            <p className="text-xl text-muted-foreground">Secure P2P File Sharing</p>
          </div>
          <ModeToggle />
        </header>

        <Tabs defaultValue="upload">
          <div className="bg-card rounded-lg shadow-lg p-6 border">
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger value='upload'>Share a File</TabsTrigger>
              <TabsTrigger value='download'>Receive a File</TabsTrigger>
            </TabsList>

            <TabsContent value='upload'>
              <div className="space-y-6">
                <FileUpload onFileUpload={handleFileUpload} isUploading={isUploading} />

                {uploadedFile && !isUploading && (
                  <div className="p-4 bg-muted rounded-lg border">
                    <p className="text-sm text-muted-foreground">
                      Selected file: <span className="font-medium text-foreground">{uploadedFile.name}</span> ({Math.round(uploadedFile.size / 1024)} KB)
                    </p>
                  </div>
                )}

                {isUploading && (
                  <div className="text-center py-8">
                    <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent"></div>
                    <p className="mt-2 text-muted-foreground">Uploading file...</p>
                  </div>
                )}

                <InviteCode port={port} />
              </div>
            </TabsContent>
            <TabsContent value='download'>
              <div className="space-y-6">
                <FileDownload onDownload={handleDownload} isDownloading={isDownloading} />

                {isDownloading && (
                  <div className="text-center py-8">
                    <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent"></div>
                    <p className="mt-2 text-muted-foreground">Downloading file...</p>
                  </div>
                )}
              </div>
            </TabsContent>
          </div>
        </Tabs>

        <footer className="mt-12 text-center text-muted-foreground text-sm">
          <p>PeerLink &copy; {new Date().getFullYear()} - Secure P2P File Sharing</p>
        </footer>
      </div>
    </div>
  );
}
