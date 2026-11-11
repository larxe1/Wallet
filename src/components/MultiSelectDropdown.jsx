import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ChevronDown, Check } from 'lucide-react';
import './MultiSelectDropdown.css';

const MultiSelectDropdown = ({
  options = [],
  selectedIds = new Set(),
  onChange,
  placeholder = 'Select items',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const triggerRef = useRef(null);

  const safeSelected = selectedIds instanceof Set ? selectedIds : new Set(selectedIds || []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && isOpen) {
        setIsOpen(false);
        triggerRef.current?.focus();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleToggle = (id) => {
    const newSelected = new Set(safeSelected);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    onChange(newSelected);
  };

  const selectedCount = safeSelected.size;
  const displayLabel =
    selectedCount === 0
      ? placeholder
      : selectedCount === 1
        ? options.find((o) => safeSelected.has(o.id))?.label || placeholder
        : `${selectedCount} selected`;

  return (
    <div className="multi-select-container" ref={dropdownRef}>
      <button
        ref={triggerRef}
        type="button"
        className={`multi-select-btn ${isOpen ? 'active' : ''} ${selectedCount > 0 ? 'has-selection' : ''}`}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <span className="truncate">{displayLabel}</span>
        <span className="multi-select-btn-meta">
          {selectedCount > 1 && (
            <span className="multi-select-badge">{selectedCount}</span>
          )}
          <ChevronDown
            size={15}
            strokeWidth={2.2}
            className={`dropdown-arrow ${isOpen ? 'rotated' : ''}`}
            aria-hidden="true"
          />
        </span>
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="multi-select-menu"
            role="listbox"
            aria-multiselectable="true"
            aria-label={placeholder}
            initial={{ opacity: 0, y: -6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.97 }}
            transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
          >
            {selectedCount > 0 && (
              <div className="multi-select-menu-header">
                <span>{selectedCount} active</span>
                <button
                  type="button"
                  className="multi-select-clear"
                  onClick={() => onChange(new Set())}
                >
                  Clear
                </button>
              </div>
            )}

            <div className="multi-select-options">
              {options.map((option) => {
                const isSelected = safeSelected.has(option.id);
                return (
                  <button
                    key={option.id}
                    type="button"
                    role="option"
                    aria-selected={isSelected}
                    className={`multi-select-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => handleToggle(option.id)}
                  >
                    <span className={`custom-checkbox ${isSelected ? 'checked' : ''}`} aria-hidden="true">
                      {isSelected && <Check size={12} strokeWidth={3} />}
                    </span>
                    <span className="item-label">{option.label}</span>
                  </button>
                );
              })}

              {options.length === 0 && (
                <div className="multi-select-empty">No options available</div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default MultiSelectDropdown;
