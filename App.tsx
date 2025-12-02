import React, { useState, useEffect, useCallback, useRef } from 'react';
import Header from './components/Header';
import ChatMessage from './components/ChatMessage';
import ChatInput from './components/ChatInput';
import Footer from './components/Footer';
import ConversationStarters from './components/ConversationStarters';
import ShareButton from './components/ShareButton';
import { Message } from './types';
import { fetchGeminiResponse } from './services/geminiService';

const App: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isCopied, setIsCopied] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);
  
  const handleFileChange = useCallback((files: FileList | null) => {
    if (files) {
      setUploadedFiles(Array.from(files));
      // Future implementation would involve processing these files.
    }
  }, []);

  const handleSendMessage = useCallback(async (text: string) => {
    const newUserMessage: Message = {
      id: Date.now().toString(),
      text,
      sender: 'user',
    };
    setMessages(prevMessages => [...prevMessages, newUserMessage]);
    setIsLoading(true);
    setError(null);

    const loadingMessageId = (Date.now() + 1).toString();
    const loadingAiMessage: Message = {
      id: loadingMessageId,
      text: '...', // Special key for loading indicator
      sender: 'ai',
      sources: []
    };
    setMessages(prevMessages => [...prevMessages, loadingAiMessage]);

    try {
      const { text: aiText, sources } = await fetchGeminiResponse(text);
      const newAiMessage: Message = {
        id: loadingMessageId, // Use same ID to replace
        text: aiText,
        sender: 'ai',
        sources: sources,
      };
      setMessages(prevMessages => prevMessages.map(msg => msg.id === loadingMessageId ? newAiMessage : msg));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "An unexpected error occurred.";
      setError(errorMessage); 
      const errorAiMessage: Message = {
        id: loadingMessageId, // Use same ID to replace
        text: `Sorry, I encountered an error. ${errorMessage}. Please check your configuration or try again.`,
        sender: 'ai',
        sources: []
      };
      setMessages(prevMessages => prevMessages.map(msg => msg.id === loadingMessageId ? errorAiMessage : msg));
    } finally {
      setIsLoading(false);
    }
  }, []); 

  const handleShareConversation = useCallback(() => {
    // Filter out the loading message before copying
    const validMessages = messages.filter(msg => msg.text !== '...');

    if (validMessages.length === 0) return;

    const formattedConversation = validMessages
      .map(msg => {
        let content = `${msg.sender === 'user' ? 'User' : 'LexMENA'}:\n${msg.text}`;
        if (msg.sender === 'ai' && msg.sources && msg.sources.length > 0) {
          const sourcesText = msg.sources
            .map(source => `- ${source.title} (${source.uri})`)
            .join('\n');
          content += `\n\nSources:\n${sourcesText}`;
        }
        return content;
      })
      .join('\n\n---\n\n');

    const fullText = `LexMENA Conversation\n\n${formattedConversation}`;
    
    navigator.clipboard.writeText(fullText).then(() => {
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2500);
    }).catch(err => {
      console.error('Failed to copy conversation: ', err);
      setError("Failed to copy conversation to clipboard.");
    });
  }, [messages]);

  return (
    <div className="flex flex-col h-screen bg-body-bg text-text-primary">
      <Header />
      <main className="flex-grow overflow-y-auto p-4 pb-32" aria-live="polite">
        <div className="container mx-auto max-w-4xl space-y-6 glass-panel p-6 rounded-2xl border border-border-color shadow-lg fade-in">
          <h1 className="text-center text-2xl font-bold text-primary-accent opacity-90">LexMENA</h1>
          {messages.length === 0 && <ConversationStarters onSendMessage={handleSendMessage} />}
          {messages.length > 0 && <ShareButton onShare={handleShareConversation} isCopied={isCopied} />}
          {messages.map(msg => (
            <ChatMessage key={msg.id} message={msg} />
          ))}
          <div ref={messagesEndRef} />
        </div>
      </main>
      <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} onFileChange={handleFileChange} />
      <Footer />
    </div>
  );
};

export default App;