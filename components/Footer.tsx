import React from 'react';

const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-header-bg text-text-secondary p-4 text-center text-sm">
      <div className="container mx-auto max-w-7xl">
        <p>&copy; {currentYear} LexMENA. All rights reserved.</p>
        <p>Developed by Amr Reda Abdullatif.</p>
      </div>
    </footer>
  );
};

export default Footer;