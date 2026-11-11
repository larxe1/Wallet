import React, { useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import {
  CalendarDays,
  LayoutGrid,
  LogOut,
  Receipt,
  Repeat,
  SlidersHorizontal,
  Sparkles,
  TrendingUp,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import ChangelogModal from './ChangelogModal';
import './Sidebar.css';

const PRIMARY_NAV_ITEMS = [
  { path: '/dashboard', label: 'Dashboard', icon: LayoutGrid },
  { path: '/expenses', label: 'Expenses', icon: Receipt },
  { path: '/income', label: 'Income', icon: TrendingUp },
  { path: '/calendar', label: 'Calendar', icon: CalendarDays },
  { path: '/recurring', label: 'Recurring', icon: Repeat },
];

const Sidebar = ({ onOpenChangelog }) => {
  const { signOut } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [internalChangelogOpen, setInternalChangelogOpen] = useState(false);

  const handleOpenChangelog = () => {
    if (onOpenChangelog) {
      onOpenChangelog();
    } else {
      setInternalChangelogOpen(true);
    }
  };

  const navigateWithTransition = (e, path) => {
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) {
      return;
    }
    if (location.pathname === path) return;

    e.preventDefault();
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (typeof document !== 'undefined' && document.startViewTransition && !prefersReducedMotion) {
      document.startViewTransition(() => {
        navigate(path);
      });
    } else {
      navigate(path);
    }
  };

  return (
    <>
      <aside className="sidebar-rail" aria-label="Primary navigation">
        {/* Main Navigation Pill Cluster */}
        <nav className="rail-cluster rail-cluster--primary" aria-label="Workspace sections">
          {PRIMARY_NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={(e) => navigateWithTransition(e, item.path)}
                className={`rail-item ${isActive ? 'is-active' : ''}`}
                aria-label={item.label}
                aria-current={isActive ? 'page' : undefined}
              >
                {isActive && (
                  <motion.span
                    layoutId="sidebar-active-pill"
                    className="rail-active-pill"
                    transition={{ type: 'spring', stiffness: 420, damping: 32 }}
                  />
                )}
                <Icon className="rail-icon" size={20} strokeWidth={isActive ? 2.25 : 1.85} aria-hidden="true" />
                <span className="rail-tooltip" role="tooltip">
                  {item.label}
                </span>
              </NavLink>
            );
          })}

          {/* Settings lives in dock on mobile too */}
          <NavLink
            to="/settings"
            onClick={(e) => navigateWithTransition(e, '/settings')}
            className={`rail-item rail-item--mobile-settings ${
              location.pathname === '/settings' ? 'is-active' : ''
            }`}
            aria-label="Settings"
            aria-current={location.pathname === '/settings' ? 'page' : undefined}
          >
            {location.pathname === '/settings' && (
              <motion.span
                layoutId="sidebar-active-pill"
                className="rail-active-pill"
                transition={{ type: 'spring', stiffness: 420, damping: 32 }}
              />
            )}
            <SlidersHorizontal
              className="rail-icon"
              size={20}
              strokeWidth={location.pathname === '/settings' ? 2.25 : 1.85}
              aria-hidden="true"
            />
            <span className="rail-tooltip" role="tooltip">
              Settings
            </span>
          </NavLink>
        </nav>

        {/* Desktop Utility Cluster */}
        <div className="rail-cluster rail-cluster--utility">
          <NavLink
            to="/settings"
            onClick={(e) => navigateWithTransition(e, '/settings')}
            className={`rail-item ${location.pathname === '/settings' ? 'is-active' : ''}`}
            aria-label="Settings"
            aria-current={location.pathname === '/settings' ? 'page' : undefined}
          >
            {location.pathname === '/settings' && (
              <motion.span
                layoutId="sidebar-active-pill"
                className="rail-active-pill"
                transition={{ type: 'spring', stiffness: 420, damping: 32 }}
              />
            )}
            <SlidersHorizontal
              className="rail-icon"
              size={20}
              strokeWidth={location.pathname === '/settings' ? 2.25 : 1.85}
              aria-hidden="true"
            />
            <span className="rail-tooltip" role="tooltip">
              Settings
            </span>
          </NavLink>

          <button
            type="button"
            className="rail-item"
            onClick={handleOpenChangelog}
            aria-label="Release Notes v1.1.0"
          >
            <Sparkles className="rail-icon" size={19} strokeWidth={1.85} aria-hidden="true" />
            <span className="rail-tooltip" role="tooltip">
              Release Notes (v1.1.0)
            </span>
          </button>
        </div>

        {/* Desktop Bottom Sign Out */}
        <div className="rail-cluster rail-cluster--footer">
          <button
            type="button"
            className="rail-item rail-item--logout"
            onClick={signOut}
            aria-label="Sign Out"
          >
            <LogOut className="rail-icon" size={19} strokeWidth={1.85} aria-hidden="true" />
            <span className="rail-tooltip" role="tooltip">
              Sign Out
            </span>
          </button>
        </div>
      </aside>

      {!onOpenChangelog && (
        <ChangelogModal
          isOpen={internalChangelogOpen}
          onClose={() => setInternalChangelogOpen(false)}
        />
      )}
    </>
  );
};

export default Sidebar;
