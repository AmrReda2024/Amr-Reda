import React from 'react';

const ShareIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden="true">
    <path d="M15.5 12.086a3.5 3.5 0 0 0-5.323-2.013l-3.34-1.78a3.5 3.5 0 1 0-.613 1.155l3.34 1.78a3.5 3.5 0 1 0 5.936-.942ZM4.5 7a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5Zm0 11a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5Zm11-5.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0Z" />
  </svg>
);

const CheckIcon: React.FC<{ className?: string }> = ({ className }) => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden="true">
        <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.052-.143Z" clipRule="evenodd" />
    </svg>
);


interface ShareButtonProps {
  onShare: () => void;
  isCopied: boolean;
}

const ShareButton: React.FC<ShareButtonProps> = ({ onShare, isCopied }) => {
  return (
    <div className="flex justify-end -mb-2">
      <button
        onClick={onShare}
        disabled={isCopied}
        className="flex items-center px-4 py-2 bg-card-bg border border-border-color rounded-lg shadow-sm text-sm font-medium text-text-primary hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-accent transition-all duration-200 disabled:bg-slate-50 disabled:cursor-wait"
        aria-live="polite"
      >
        {isCopied ? (
          <>
            <CheckIcon className="h-5 w-5 mr-2 text-green-600" />
            Copied to Clipboard!
          </>
        ) : (
          <>
            <ShareIcon className="h-5 w-5 mr-2 text-primary-accent" />
            Share Conversation
          </>
        )}
      </button>
    </div>
  );
};

export default ShareButton;
