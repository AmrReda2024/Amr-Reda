import React from 'react';
import { Message, Source } from '../types';

interface ChatMessageProps {
  message: Message;
}

const UserIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path fillRule="evenodd" d="M18.685 19.097A9.723 9.723 0 0 0 21.75 12c0-5.385-4.365-9.75-9.75-9.75S2.25 6.615 2.25 12a9.723 9.723 0 0 0 3.065 7.097A9.716 9.716 0 0 0 12 21.75a9.716 9.716 0 0 0 6.685-2.653Zm-12.54-1.285A7.486 7.486 0 0 1 12 15a7.486 7.486 0 0 1 5.855 2.812A8.224 8.224 0 0 1 12 20.25a8.224 8.224 0 0 1-5.855-2.438ZM15.75 9a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z" clipRule="evenodd" />
  </svg>
);

// AI (Quill) Icon
const AiQuillIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg 
    xmlns="http://www.w3.org/2000/svg" 
    viewBox="0 0 24 24" 
    fill="none" 
    stroke="currentColor" 
    strokeWidth="2" 
    strokeLinecap="round" 
    strokeLinejoin="round" 
    className={className}
    aria-hidden="true"
  >
    <path d="M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z"></path>
    <path d="M16 8L2 22"></path>
    <path d="M17.5 15H9"></path>
  </svg>
);

const ExternalLinkIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
    <path d="M13.5 6H5.25A2.25 2.25 0 0 0 3 8.25v10.5A2.25 2.25 0 0 0 5.25 21h10.5A2.25 2.25 0 0 0 18 18.75V10.5a.75.75 0 0 0-1.5 0v8.25a.75.75 0 0 1-.75.75H5.25a.75.75 0 0 1-.75-.75V8.25a.75.75 0 0 1 .75-.75h8.25a.75.75 0 0 0 0-1.5Z" />
    <path d="M16.5 3a.75.75 0 0 0 0 1.5h1.94l-6.72 6.72a.75.75 0 1 0 1.06 1.06L19.5 5.56v1.94a.75.75 0 0 0 1.5 0V3.75A.75.75 0 0 0 20.25 3h-3.75Z" />
  </svg>
);


const SourcesList: React.FC<{ sources: Source[] }> = ({ sources }) => {
  if (!sources || sources.length === 0) {
    return null;
  }
  return (
    <div className="mt-4 pt-4 border-t border-slate-200">
      <h4 className="text-base font-bold text-text-primary mb-2">Sources</h4>
      <div className="space-y-2">
        {sources.map((source, index) => (
          <a
            key={index}
            href={source.uri}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center p-2.5 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors group border border-slate-200"
          >
            <ExternalLinkIcon className="h-5 w-5 text-primary-accent mr-3 flex-shrink-0" />
            <span className="text-sm font-medium text-primary-accent group-hover:underline break-all">
              {source.title || source.uri}
            </span>
          </a>
        ))}
      </div>
    </div>
  );
};

const isArabic = (text: string): boolean => {
  if (!text) return false;
  const arabicRegex = /[\u0600-\u06FF]/;
  return arabicRegex.test(text);
};

const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
  const isUser = message.sender === 'user';
  const messageIsArabic = isArabic(message.text);

  return (
    <div className={`flex mb-4 ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`flex items-start max-w-xl lg:max-w-2xl ${isUser ? 'flex-row-reverse' : ''}`}>
        {isUser ? (
          <UserIcon className="h-8 w-8 rounded-full bg-primary-accent text-white p-1 ml-2 flex-shrink-0" />
        ) : (
          <AiQuillIcon className="h-8 w-8 text-primary-accent mr-2 flex-shrink-0" />
        )}
        <div
          className={`p-3 rounded-lg shadow-md ${
            isUser ? 'bg-primary-accent text-white' : 'bg-card-bg text-text-primary border border-border-color'
          }`}
        >
          <p 
            className={`whitespace-pre-wrap ${messageIsArabic ? 'text-right' : 'text-left'}`}
            dir={messageIsArabic ? 'rtl' : 'ltr'}
          >
            {message.text}
          </p>
          {!isUser && message.sources && <SourcesList sources={message.sources} />}
        </div>
      </div>
    </div>
  );
};

export default ChatMessage;