'use client';

import { useState } from 'react';
import { FiDownload } from 'react-icons/fi';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Button } from './ui/button';

interface FileDownloadProps {
  onDownload: (port: number) => Promise<void>;
  isDownloading: boolean;
}

export default function FileDownload({ onDownload, isDownloading }: FileDownloadProps) {
  const [inviteCode, setInviteCode] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const port = parseInt(inviteCode.trim(), 10);
    if (isNaN(port) || port <= 0 || port > 65535) {
      setError('Please enter a valid port number (1-65535)');
      return;
    }

    try {
      await onDownload(port);
    } catch (err) {
      console.error('Download error:', err);
      setError('Failed to download the file. Please check the invite code and try again.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-primary/5 p-4 rounded-lg border border-primary/20">
        <h3 className="text-lg font-semibold text-foreground mb-2">Receive a File</h3>
        <p className="text-sm text-muted-foreground">
          Enter the invite code shared with you to download the file.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="inviteCode">
            Invite Code
          </Label>
          <Input
            type="text"
            id="inviteCode"
            value={inviteCode}
            onChange={(e) => setInviteCode(e.target.value)}
            placeholder="Enter the invite code (port number)"
            disabled={isDownloading}
            required
            className="text-base"
          />
          {error && <p className="mt-1 text-sm text-destructive">{error}</p>}
        </div>

        <Button
          type="submit"
          disabled={isDownloading}
          size="lg"
          className="w-full"
        >
          {isDownloading ? (
            <span>Downloading...</span>
          ) : (
            <>
              <FiDownload className="mr-2" />
              <span>Download File</span>
            </>
          )}
        </Button>
      </form>
    </div>
  );
}
