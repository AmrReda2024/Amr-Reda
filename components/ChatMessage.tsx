import React from 'react';
import { Message, Source } from '../types';

// Simple Markdown to HTML renderer
const renderMarkdown = (text: string) => {
  const lines = text.split('\n');
  let inList = false;
  const htmlLines = lines.map(line => {
    // Headings
    if (line.startsWith('### ')) {
      return `<h3 class="text-lg font-bold mt-4 mb-2 text-text-primary">${line.substring(4)}</h3>`;
    }
    // Blockquotes
    if (line.startsWith('> ')) {
      return `<blockquote class="border-l-4 border-border-color pl-4 italic text-text-secondary">${line.substring(2)}</blockquote>`;
    }
    // List items
    if (line.startsWith('* ')) {
      const listItem = `<li>${line.substring(2)}</li>`;
      if (!inList) {
        inList = true;
        return `<ul class="list-disc pl-5 space-y-1">${listItem}`;
      }
      return listItem;
    }

    if (inList) {
      inList = false;
      return `</ul>${line}`;
    }

    return line;
  });

  if (inList) {
    htmlLines.push('</ul>');
  }
  
  let html = htmlLines.join('\n').replace(/\n/g, '<br />');

  // Bold
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  
  // Clean up extra breaks after block elements
  const blockElements = ['h3', 'blockquote', 'ul', 'li'];
  blockElements.forEach(tag => {
    const regex = new RegExp(`</${tag}><br />`, 'g');
    html = html.replace(regex, `</${tag}>`);
  });

  return { __html: html };
};


const UserIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path fillRule="evenodd" d="M18.685 19.097A9.723 9.723 0 0 0 21.75 12c0-5.385-4.365-9.75-9.75-9.75S2.25 6.615 2.25 12a9.723 9.723 0 0 0 3.065 7.097A9.716 9.716 0 0 0 12 21.75a9.716 9.716 0 0 0 6.685-2.653Zm-12.54-1.285A7.486 7.486 0 0 1 12 15a7.486 7.486 0 0 1 5.855 2.812A8.224 8.224 0 0 1 12 20.25a8.224 8.224 0 0 1-5.855-2.438ZM15.75 9a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z" clipRule="evenodd" />
  </svg>
);

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
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden="true">
    <path d="M12.232 4.232a2.5 2.5 0 0 1 3.536 3.536l-1.225 1.224a.75.75 0 0 0 1.061 1.06l1.224-1.224a4 4 0 0 0-5.656-5.656l-3 3a4 4 0 0 0 .225 5.865.75.75 0 0 0 .977-1.138 2.5 2.5 0 0 1-.142-3.665l3-3Z" />
    <path d="M8.603 14.53a2.5 2.5 0 0 1-3.536-3.536l1.225-1.224a.75.75 0 0 0-1.061-1.06l-1.224 1.224a4 4 0 0 0 5.656 5.656l3-3a4 4 0 0 0-.225-5.865.75.75 0 0 0-.977 1.138 2.5 2.5 0 0 1 .142 3.665l-3 3Z" />
  </svg>
);

const SourcesList: React.FC<{ sources: Source[] }> = ({ sources }) => {
  if (!sources || sources.length === 0) return null;
  return (
    <div className="mt-4 pt-4 border-t border-border-color">
      <h4 className="text-sm font-bold text-text-primary mb-2">Sources</h4>
      <div className="space-y-2">
        {sources.map((source, index) => (
          <a
            key={index}
            href={source.uri}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-start p-2.5 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors group border border-slate-200"
          >
            <ExternalLinkIcon className="h-4 w-4 text-primary-accent mr-2 mt-0.5 flex-shrink-0" />
            <span className="text-sm font-medium text-primary-accent group-hover:underline break-words">
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
  // Check if more than 10% of the text is Arabic, to avoid matching single characters
  const arabicChars = (text.match(arabicRegex) || []).length;
  return arabicChars / text.length > 0.1;
};

// Fix: Define ChatMessageProps interface for the component's props.
interface ChatMessageProps {
  message: Message;
}

const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
  const isUser = message.sender === 'user';
  const messageIsArabic = isArabic(message.text);

  // Loading Indicator UI
  if (message.text === '...') {
    return (
      <div className="flex mb-4 justify-start animate-fadeIn">
        <div className="flex items-start max-w-xl lg:max-w-2xl">
          <div className="h-8 w-8 bg-slate-200 rounded-full mr-3 flex-shrink-0 flex items-center justify-center">
            <AiQuillIcon className="h-5 w-5 text-slate-500" />
          </div>
          <div className="p-3 rounded-lg bg-card-bg border border-border-color">
            <div className="flex items-center justify-center space-x-1.5">
              <div className="w-2 h-2 bg-primary-accent rounded-full animate-pulse" style={{ animationDelay: '0s' }}></div>
              <div className="w-2 h-2 bg-primary-accent rounded-full animate-pulse" style={{ animationDelay: '0.2s' }}></div>
              <div className="w-2 h-2 bg-primary-accent rounded-full animate-pulse" style={{ animationDelay: '0.4s' }}></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex mb-4 ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`flex items-start max-w-2xl lg:max-w-4xl ${isUser ? 'flex-row-reverse' : ''}`}>
        <div className={`h-8 w-8 rounded-full flex-shrink-0 flex items-center justify-center ${isUser ? 'bg-primary-accent text-white ml-3' : 'bg-slate-200 mr-3'}`}>
          {isUser ? <UserIcon className="h-5 w-5" /> : <AiQuillIcon className="h-5 w-5 text-slate-500" />}
        </div>
        <div
          className={`px-4 py-3 rounded-lg shadow-sm ${
            isUser ? 'bg-primary-accent text-white' : 'bg-card-bg text-text-primary border border-border-color'
          }`}
        >
          <div
            className={`leading-relaxed ${messageIsArabic ? 'text-right' : 'text-left'}`}
            dir={messageIsArabic ? 'rtl' : 'ltr'}
            dangerouslySetInnerHTML={renderMarkdown(message.text)}
          />
          {!isUser && message.sources && <SourcesList sources={message.sources} />}
        </div>
      </div>
    </div>
  );
};

export default ChatMessage;
