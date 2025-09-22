import React, { useState, useRef } from 'react';

interface ChatInputProps {
  onSendMessage: (message: string) => void;
  isLoading: boolean;
  onFileChange: (files: FileList | null) => void;
}

const SendIcon: React.FC<{className?: string}> = ({className}) => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
        <path d="M3.478 2.404a.75.75 0 0 0-.926.941l2.432 7.905H13.5a.75.75 0 0 1 0 1.5H4.984l-2.432 7.905a.75.75 0 0 0 .926.94 60.519 60.519 0 0 0 18.445-8.986.75.75 0 0 0 0-1.218A60.517 60.517 0 0 0 3.478 2.404Z" />
    </svg>
);

const PaperclipIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path fillRule="evenodd" d="M18.97 3.659a2.25 2.25 0 0 0-3.182 0l-10.5 10.5a.75.75 0 0 0 1.06 1.06l10.5-10.5a.75.75 0 0 1 1.06 0Zm-4.595 4.595a2.25 2.25 0 0 0-3.182 0L6 12.384a3.75 3.75 0 0 0 5.304 5.303l7.06-7.06a.75.75 0 0 0-1.06-1.06l-7.06 7.06a2.25 2.25 0 0 1-3.182-3.182l4.829-4.829a.75.75 0 0 0-1.06-1.06l-4.83 4.83a3.75 3.75 0 0 0 5.303 5.302l10.75-10.75a2.25 2.25 0 0 0-3.182-3.182Z" clipRule="evenodd" />
  </svg>
);


const ChatInput: React.FC<ChatInputProps> = ({ onSendMessage, isLoading, onFileChange }) => {
  const [inputValue, setInputValue] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputValue.trim() && !isLoading) {
      onSendMessage(inputValue.trim());
      setInputValue('');
    }
  };

  const handleFileButtonClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFileChange(e.target.files);
    if (e.target) {
      e.target.value = '';
    }
  };


  return (
    <div className="bg-card-bg border-t border-border-color sticky bottom-0">
      <form onSubmit={handleSubmit} className="p-4 container mx-auto max-w-4xl">
        <div className="flex items-end bg-card-bg border border-border-color rounded-lg shadow-sm focus-within:ring-2 focus-within:ring-primary-accent">
          <button
            type="button"
            onClick={handleFileButtonClick}
            className="p-3 text-text-secondary hover:text-primary-accent focus:outline-none disabled:text-slate-300"
            disabled={isLoading}
            aria-label="Attach file"
          >
            <PaperclipIcon className="h-6 w-6" />
          </button>
          <input 
            type="file"
            ref={fileInputRef}
            onChange={handleFileInputChange}
            className="hidden"
            multiple
          />
          <textarea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Ask a legal question or describe your task... / اطرح سؤالاً قانونياً أو صف مهمتك..."
            rows={1}
            className="flex-grow p-3 border-none text-text-primary bg-transparent outline-none resize-none disabled:bg-slate-50 max-h-40"
            style={{paddingTop: '12px', paddingBottom: '12px'}}
            disabled={isLoading}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSubmit(e);
              }
            }}
            aria-label="Chat message input"
          />
          <button
            type="submit"
            className="p-3 text-white bg-primary-accent rounded-lg m-1 hover:bg-primary-accent-hover focus:outline-none focus:ring-2 focus:ring-primary-accent focus:ring-offset-2 disabled:bg-blue-300"
            disabled={isLoading || !inputValue.trim()}
            aria-label="Send message"
          >
            {isLoading ? (
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white" role="status" aria-label="Loading"></div>
            ) : (
              <SendIcon className="h-6 w-6" />
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ChatInput;
