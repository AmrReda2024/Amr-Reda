import React from 'react';

interface Starter {
  use_case: string;
  prompt: string;
}

const STARTERS: Starter[] = [
    { use_case: "Egyptian Law", prompt: "ما هي عقوبة التهرب الضريبي في القانون المصري؟" },
    { use_case: "Case Law", prompt: "Give me a precedent on shareholder disputes in Egypt." },
    { use_case: "Contract Drafting", prompt: "Draft a joint venture agreement under UAE law." },
    { use_case: "Web Legal Research", prompt: "Search the latest construction law updates in Saudi Arabia." },
    { use_case: "Clause Analysis", prompt: "What does Article 157 of the Egyptian Civil Code say about breach of contract?" },
    { use_case: "Academic Support", prompt: "Find recent papers on arbitration in the MENA region." },
];

interface ConversationStartersProps {
  onSendMessage: (message: string) => void;
}

const ConversationStarters: React.FC<ConversationStartersProps> = ({ onSendMessage }) => {
  return (
    <div className="text-center p-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {STARTERS.map((starter, index) => (
          <button
            key={index}
            onClick={() => onSendMessage(starter.prompt)}
            className="p-4 bg-card-bg border border-border-color rounded-lg shadow-sm hover:shadow-md hover:border-primary-accent transition-all duration-200 text-left focus:outline-none focus:ring-2 focus:ring-primary-accent"
          >
            <p className="font-bold text-primary-accent text-sm">{starter.use_case}</p>
            <p className="text-text-secondary text-sm mt-1">{starter.prompt}</p>
          </button>
        ))}
      </div>
    </div>
  );
};

export default ConversationStarters;
