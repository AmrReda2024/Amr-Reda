import React from 'react';

const Header: React.FC = () => {
  return (
    <header className="bg-header-bg text-white p-3 shadow-md flex justify-center items-center h-24 md:h-28"> {/* Centered logo, adjusted padding and height */}
      <div className="container mx-auto flex justify-center items-center"> {/* Ensure container respects centering */}
        <img 
          src="/lexmena-logo.png" 
          alt="LexMENA Logo" 
          className="h-16 md:h-20 object-contain" // Adjusted height and object-contain
        />
      </div>
    </header>
  );
};

export default Header;