import React from 'react';
import './ChangelogModal.css';

const CHANGELOG = [
  {
    version: 'v1.1.0',
    date: '2026-08-09',
    changes: [
      'Added drag-and-drop reordering for Payment Methods',
      'Added inline calculator to Expense and Income forms',
      'Renamed app to Eli Tracker',
      'Removed emojis from the sidebar for a cleaner look'
    ]
  },
  {
    version: 'v1.0.1',
    date: '2026-08-07',
    changes: [
      'Fixed layout wrapping on Recurring page',
      'Removed highlighted background from wallet badges',
      'Added multi-select filtering for Payment Methods'
    ]
  },
  {
    version: 'v1.0.0',
    date: '2026-08-06',
    changes: [
      'Initial release of WalletWatch (now Eli Tracker)',
      'Added basic expense and income tracking',
      'Added Dashboard with privacy mode toggle',
      'Added recurring transactions support'
    ]
  }
];

const ChangelogModal = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  const handleOverlayClick = (e) => {
    if (e.target.className === 'modal-overlay') {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content slide-up changelog-modal">
        <div className="changelog-header">
          <h2>Release Notes</h2>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>
        
        <div className="changelog-list">
          {CHANGELOG.map((release) => (
            <div key={release.version} className="changelog-item">
              <div className="release-meta">
                <span className="release-version">{release.version}</span>
                <span className="release-date">{release.date}</span>
              </div>
              <ul className="release-changes">
                {release.changes.map((change, idx) => (
                  <li key={idx}>{change}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ChangelogModal;
