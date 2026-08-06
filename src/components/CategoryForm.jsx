import React, { useState, useEffect } from 'react';
import './CategoryForm.css';

const EMOJIS = ['🏠', '🛒', '🍔', '🚗', '⚡', '🎮', '🎬', '🏋️', '💰', '💳', '📦', '❤️', '🎁', '✈️', '📚', '👔', '🐾', '🏥'];

const CategoryForm = ({ isOpen, onClose, onSave, category }) => {
  const [name, setName] = useState('');
  const [icon, setIcon] = useState('🏠');

  useEffect(() => {
    if (category) {
      setName(category.name);
      setIcon(category.icon || '🏠');
    } else {
      setName('');
      setIcon('🏠');
    }
  }, [category, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    onSave({
      name: name.trim(),
      icon
    });
  };

  const handleOverlayClick = (e) => {
    if (e.target.className === 'modal-overlay') {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content slide-up category-modal">
        <h2>{category ? 'Edit Category' : 'Add Category'}</h2>
        <form onSubmit={handleSubmit} className="category-form">
          
          <div className="form-group">
            <label>Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Groceries"
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label>Icon</label>
            <div className="emoji-picker">
              {EMOJIS.map(e => (
                <button
                  key={e}
                  type="button"
                  className={`emoji-btn ${e === icon ? 'selected' : ''}`}
                  onClick={() => setIcon(e)}
                >
                  {e}
                </button>
              ))}
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" onClick={onClose} className="btn-cancel">Cancel</button>
            <button type="submit" className="btn-save">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CategoryForm;
