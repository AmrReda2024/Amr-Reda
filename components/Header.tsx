import React from 'react';

const Header: React.FC = () => {
  return (
    <header className="bg-header-bg text-text-primary p-3 shadow-sm flex justify-center items-center h-20 md:h-24 border-b border-border-color sticky top-0 z-10">
      <div className="container mx-auto flex justify-center items-center">
        <img 
          src="/lexmena-logo.png" 
          alt="LexMENA Logo" 
          className="h-14 md:h-16 object-contain"
        />
      </div>
    </header>
  );
};

export default Header;
