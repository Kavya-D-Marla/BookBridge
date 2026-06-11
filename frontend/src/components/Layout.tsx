import React from 'react';
import Navbar from './Navbar';
import { useAuth } from '../context/AuthContext';
import { BookOpen } from 'lucide-react';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex h-screen w-screen flex-col items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center space-y-4">
          {/* Pulsing Book Logo */}
          <div className="relative flex h-16 w-16 items-center justify-center rounded-2xl bg-indigo-650 bg-gradient-to-br from-indigo-600 to-indigo-500 text-white shadow-premium animate-pulse">
            <BookOpen className="h-8 w-8" />
          </div>
          {/* Elegant Loading Text */}
          <h2 className="font-display text-lg font-bold text-slate-800 tracking-wide">
            Connecting to BookBridge...
          </h2>
          <div className="h-1.5 w-36 overflow-hidden rounded-full bg-slate-200">
            <div className="h-full w-1/2 rounded-full bg-indigo-600 animate-[loading_1s_ease-in-out_infinite]"></div>
          </div>
        </div>
        <style>{`
          @keyframes loading {
            0% { transform: translateX(-100%); }
            100% { transform: translateX(200%); }
          }
        `}</style>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fade-in">
        {children}
      </main>
      <footer className="w-full border-t border-slate-200 bg-white py-6">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500">
          <p>&copy; {new Date().getFullYear()} BookBridge. Built for students, by students.</p>
          <div className="flex space-x-4 mt-2 sm:mt-0">
            <a href="/disputes" className="hover:text-indigo-600 transition">Report an Issue</a>
            <span>&bull;</span>
            <span className="cursor-default">Version 1.0.0</span>
          </div>
        </div>
      </footer>
    </div>
  );
};
export default Layout;
