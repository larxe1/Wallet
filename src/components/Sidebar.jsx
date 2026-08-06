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
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/expenses', label: 'Expenses' },
    { path: '/income', label: 'Income' },
    { path: '/calendar', label: 'Calendar' },
    { path: '/recurring', label: 'Recurring' },
    { path: '/settings', label: 'Settings' },
  ];

  return (
    <>
      <button className="mobile-toggle" onClick={toggleSidebar}>
        {isOpen ? '✕' : '☰'}
      </button>
      
      <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <div className="logo-container">
            <h1 className="logo-text">Eli Tracker</h1>
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
            <span className="signout-label">Sign Out</span>
          </button>
        </div>
      </aside>
      
      {isOpen && <div className="sidebar-overlay" onClick={toggleSidebar}></div>}
    </>
  );
};

export default Sidebar;
