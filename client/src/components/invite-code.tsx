'use client';

import { useState } from 'react';
import { FiCopy, FiCheck } from 'react-icons/fi';

import { Button } from './ui/button';

interface InviteCodeProps {
  port: number | null;
}

export default function InviteCode({ port }: InviteCodeProps) {
  const [copied, setCopied] = useState(false);

  if (!port) return null;

  const copyToClipboard = () => {
    navigator.clipboard.writeText(port.toString());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="p-6 bg-green-50 dark:bg-green-950/20 border border-green-200 dark:border-green-800 rounded-lg">
      <h3 className="text-lg font-semibold text-green-800 dark:text-green-400 mb-2">File Ready to Share!</h3>
      <p className="text-sm text-green-700 dark:text-green-300 mb-4">
        Share this invite code with anyone you want to share the file with:
      </p>

      <div className="flex items-center gap-0 rounded-lg overflow-hidden border">
        <div className="flex-1 bg-background p-4 font-mono text-lg font-semibold text-center">
          {port}
        </div>
        <Button
          onClick={copyToClipboard}
          variant="secondary"
          size="lg"
          className="rounded-none px-4"
          aria-label="Copy invite code"
        >
          {copied ? <FiCheck className="w-5 h-5" /> : <FiCopy className="w-5 h-5" />}
        </Button>
      </div>

      <p className="mt-4 text-xs text-muted-foreground">
        This code will be valid as long as your file sharing session is active.
      </p>
    </div>
  );
}
