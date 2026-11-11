import React, { useEffect, useRef } from 'react';
import { Sparkles, X } from 'lucide-react';
import { formatDate } from '../utils/format';
import './ChangelogModal.css';

const CHANGELOG = [
  {
    version: 'v1.1.0',
    date: '2026-08-09',
    changes: [
      'Added drag-and-drop reordering for Payment Methods',
      'Added inline calculator to Expense and Income forms',
      'Renamed app to Money Tracker',
      'Removed emojis from the sidebar for a cleaner look',
    ],
  },
  {
    version: 'v1.0.1',
    date: '2026-08-07',
    changes: [
      'Fixed layout wrapping on Recurring page',
      'Removed highlighted background from wallet badges',
      'Added multi-select filtering for Payment Methods',
    ],
  },
  {
    version: 'v1.0.0',
    date: '2026-08-06',
    changes: [
      'Initial release of WalletWatch (now Money Tracker)',
      'Added basic expense and income tracking',
      'Added Dashboard with privacy mode toggle',
      'Added recurring transactions support',
    ],
  },
];

const ChangelogModal = ({ isOpen, onClose }) => {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (isOpen) {
      if (!dialog.open) {
        dialog.showModal();
      }
    } else if (dialog.open) {
      dialog.close();
    }
  }, [isOpen]);

  const handleBackdropClick = (e) => {
    if (e.target === dialogRef.current) {
      onClose();
    }
  };

  const handleCancel = (e) => {
    e.preventDefault();
    onClose();
  };

  return (
    <dialog
      ref={dialogRef}
      className="changelog-dialog"
      aria-labelledby="changelog-modal-title"
      onClick={handleBackdropClick}
      onCancel={handleCancel}
    >
      <div className="changelog-modal">
        <div className="changelog-header">
          <div className="changelog-title-group">
            <span className="changelog-icon" aria-hidden="true">
              <Sparkles size={18} />
            </span>
            <div>
              <h2 id="changelog-modal-title">Money Tracker Release Notes</h2>
              <p className="changelog-subtitle">Version history & recent refinements</p>
            </div>
          </div>
          <button
            type="button"
            className="changelog-close-btn"
            onClick={onClose}
            aria-label="Close release notes"
          >
            <X size={18} />
          </button>
        </div>

        <div className="changelog-list">
          {CHANGELOG.map((release, index) => (
            <section key={release.version} className="changelog-item">
              <div className="release-meta">
                <span className={`release-version ${index === 0 ? 'is-latest' : ''}`}>
                  {release.version}
                </span>
                <span className="release-date tabular-nums">{formatDate(release.date)}</span>
                {index === 0 && <span className="release-tag">Latest</span>}
              </div>
              <ul className="release-changes">
                {release.changes.map((change, idx) => (
                  <li key={idx}>{change}</li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      </div>
    </dialog>
  );
};

export default ChangelogModal;
