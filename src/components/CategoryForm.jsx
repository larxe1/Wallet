import React, { useState, useEffect, useId, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Tag } from 'lucide-react';
import './CategoryForm.css';

const EMOJIS = ['🏠', '🛒', '🍔', '🚗', '⚡', '🎮', '🎬', '🏋️', '💰', '💳', '📦', '❤️', '🎁', '✈️', '📚', '👔', '🐾', '🏥'];

const CategoryForm = ({ isOpen, onClose, onSave, category }) => {
  const uid = useId();
  const titleId = `cat-modal-title-${uid}`;
  const nameFieldId = `cat-name-${uid}`;

  const [name, setName] = useState('');
  const [icon, setIcon] = useState('🏠');

  const iconChoices = useMemo(() => {
    if (category?.icon && !EMOJIS.includes(category.icon)) {
      return [category.icon, ...EMOJIS];
    }
    return EMOJIS;
  }, [category]);

  useEffect(() => {
    if (!isOpen) return;

    if (category) {
      setName(category.name || '');
      setIcon(category.icon || '🏠');
    } else {
      setName('');
      setIcon('🏠');
    }
  }, [category, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    onSave({
      name: name.trim(),
      icon,
    });
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="ww-cat-backdrop"
          onClick={handleOverlayClick}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            className="ww-cat-panel"
            initial={{ opacity: 0, y: 14, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.97 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="ww-cat-header">
              <h2 id={titleId}>{category ? 'Edit Category' : 'Add Category'}</h2>
              <button
                type="button"
                className="ww-cat-close"
                onClick={onClose}
                aria-label="Close modal"
              >
                <X size={18} strokeWidth={2.2} aria-hidden="true" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="ww-cat-form">
              <div className="ww-cat-preview-row">
                <div className="ww-cat-preview-pill">
                  <span className="ww-cat-preview-emoji" aria-hidden="true">{icon}</span>
                  <span className="ww-cat-preview-name">{name.trim() || 'Category Preview'}</span>
                </div>
                <span className="ww-cat-preview-tag">
                  <Tag size={12} strokeWidth={2.2} aria-hidden="true" />
                  <span>Badge</span>
                </span>
              </div>

              <div className="ww-cat-field">
                <label htmlFor={nameFieldId}>Name</label>
                <input
                  id={nameFieldId}
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Groceries"
                  required
                  autoFocus
                />
              </div>

              <div className="ww-cat-field">
                <label>Choose Icon</label>
                <div className="emoji-picker" role="group" aria-label="Category icon picker">
                  {iconChoices.map((e) => {
                    const selected = e === icon;
                    return (
                      <motion.button
                        key={e}
                        type="button"
                        whileTap={{ scale: 0.92 }}
                        className={`emoji-btn ${selected ? 'selected' : ''}`}
                        aria-pressed={selected}
                        aria-label={`Select icon ${e}`}
                        onClick={() => setIcon(e)}
                      >
                        {e}
                      </motion.button>
                    );
                  })}
                </div>
              </div>

              <div className="ww-cat-actions">
                <button type="button" onClick={onClose} className="ww-cat-cancel">
                  Cancel
                </button>
                <button type="submit" className="ww-cat-save">
                  {category ? 'Save Changes' : 'Save Category'}
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default CategoryForm;
