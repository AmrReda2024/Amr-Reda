import React, { useState, useEffect, useCallback, useRef } from 'react';
import Header from './components/Header';
import ChatMessage from './components/ChatMessage';
import ChatInput from './components/ChatInput';
import Footer from './components/Footer';
import Disclaimer from './components/Disclaimer';
import ConversationStarters from './components/ConversationStarters';
import { Message } from './types';
import { fetchGeminiResponse } from './services/geminiService';

const App: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
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

  return (
    <div className="flex flex-col h-screen bg-body-bg text-text-primary">
      <Header />
      <main className="flex-grow overflow-y-auto p-4" aria-live="polite">
        <div className="container mx-auto max-w-4xl space-y-6">
          <Disclaimer />
          {messages.length === 0 && <ConversationStarters onSendMessage={handleSendMessage} />}
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