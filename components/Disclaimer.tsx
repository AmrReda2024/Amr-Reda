import React from 'react';

const Disclaimer: React.FC = () => {
  return (
    <div className="bg-disclaimer-bg border-l-4 border-disclaimer-border text-disclaimer-text p-4 mb-6 mx-auto max-w-7xl" role="alert">
      <p className="font-bold">Important Disclaimer</p>
      <p>LexMENA provides AI-generated legal information and research assistance, with a focus on the MENA region and other specified jurisdictions. This information is for general guidance and educational purposes only. It is NOT legal advice and should not be substituted for consultation with a qualified legal professional.</p>
    </div>
  );
};

export default Disclaimer;