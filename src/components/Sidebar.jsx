import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Sidebar.css';

const Sidebar = () => {
  const { user, signOut } = useAuth();
  const [isOpen, setIsOpen] = useState(false);

  const toggleSidebar = () => {
    setIsOpen(!isOpen);
  };

  const navLinks = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/expenses', label: 'Expenses', icon: '💸' },
    { path: '/income', label: 'Income', icon: '💰' },
    { path: '/calendar', label: 'Calendar', icon: '📅' },
    { path: '/recurring', label: 'Recurring', icon: '🔄' },
    { path: '/settings', label: 'Settings', icon: '⚙️' },
  ];

  return (
    <>
      <button className="mobile-toggle" onClick={toggleSidebar}>
        {isOpen ? '✕' : '☰'}
      </button>
      
      <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <div className="logo-container">
            <span className="logo-icon">💰</span>
            <h1 className="logo-text">WalletWatch</h1>
          </div>
        </div>
        
        <nav className="sidebar-nav">
          <ul className="nav-list">
            {navLinks.map((link) => (
              <li key={link.path} className="nav-item">
                <NavLink 
                  to={link.path} 
                  className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setIsOpen(false)}
                >
                  <span className="nav-icon">{link.icon}</span>
                  <span className="nav-label">{link.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        
        <div className="sidebar-footer">
          <div className="user-info">
            <span className="user-email">{user?.email || 'user@example.com'}</span>
          </div>
          <button className="signout-button" onClick={signOut}>
            <span className="signout-icon">🚪</span>
            <span className="signout-label">Sign Out</span>
          </button>
        </div>
      </aside>
      
      {isOpen && <div className="sidebar-overlay" onClick={toggleSidebar}></div>}
    </>
  );
};

export default Sidebar;
