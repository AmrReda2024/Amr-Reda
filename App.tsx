import React, { useState, useEffect, useCallback, useRef } from 'react';
import Header from './components/Header';
import ChatMessage from './components/ChatMessage';
import ChatInput from './components/ChatInput';
import Footer from './components/Footer'; // Import Footer
import { Message } from './types';
import { fetchGeminiResponse } from './services/geminiService';

const App: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]); // For future use

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);
  
  useEffect(() => {
    setMessages([
      {
        id: Date.now().toString(),
        text: "📍 Which jurisdiction (country or legal system) would you like me to search or answer under? / 📍 في أي ولاية قضائية (دولة أو نظام قانوني) تود أن أبحث أو أجيب؟",
        sender: 'ai',
        sources: []
      }
    ]);
  }, []);

  const handleFileChange = useCallback((files: FileList | null) => {
    if (files) {
      setUploadedFiles(Array.from(files));
      // For now, we'll just update the state. 
      // Future implementation would involve processing these files.
      // console.log("Files selected:", Array.from(files).map(f => f.name)); 
      // Optionally, send a message to the chat indicating files were "received"
      // This part is UI only for this iteration. The system prompt primes the AI.
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

    try {
      const { text: aiText, sources } = await fetchGeminiResponse(text);
      const newAiMessage: Message = {
        id: (Date.now() + 1).toString(),
        text: aiText,
        sender: 'ai',
        sources: sources,
      };
      setMessages(prevMessages => [...prevMessages, newAiMessage]);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "An unexpected error occurred.";
      setError(errorMessage); 
      const errorAiMessage: Message = {
        id: (Date.now() + 1).toString(),
        text: `Sorry, I encountered an error processing your request. ${errorMessage}. Please ensure your API_KEY is correctly configured if this issue persists, or try rephrasing your query.`,
        sender: 'ai',
        sources: []
      };
      setMessages(prevMessages => [...prevMessages, errorAiMessage]);
    } finally {
      setIsLoading(false);
    }
  }, []); 

  return (
    <div className="flex flex-col h-screen bg-body-bg text-text-primary">
      <Header />
      <main className="flex-grow overflow-y-auto p-4 space-y-4 container mx-auto max-w-7xl pb-24" aria-live="polite"> {/* Added pb-24 to avoid overlap with ChatInput+Footer */}
        {messages.map(msg => (
          <ChatMessage key={msg.id} message={msg} />
        ))}
        <div ref={messagesEndRef} />
         {error && !isLoading && (
           <div className="text-red-700 bg-red-100 border border-red-300 p-3 rounded-md text-sm text-center my-2 shadow max-w-7xl mx-auto" role="alert"> {/* Ensured error message also respects new max-width */}
             <strong>Error:</strong> {error}
           </div>
         )}
      </main>
      <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} onFileChange={handleFileChange} />
      <Footer /> {/* Render Footer */}
    </div>
  );
};

export default App;