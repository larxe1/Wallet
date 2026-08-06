import React, { useState, useRef, useEffect } from 'react';
import './MultiSelectDropdown.css';

const MultiSelectDropdown = ({ options, selectedIds, onChange, placeholder = 'Select items' }) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggle = (id) => {
    const newSelected = new Set(selectedIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    onChange(newSelected);
  };

  const selectedCount = selectedIds.size;
  const displayLabel = selectedCount === 0 
    ? placeholder 
    : selectedCount === 1 
      ? options.find(o => selectedIds.has(o.id))?.label || placeholder
      : `${selectedCount} selected`;

  return (
    <div className="multi-select-container" ref={dropdownRef}>
      <button 
        type="button" 
        className={`multi-select-btn ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="truncate">{displayLabel}</span>
        <span className="dropdown-arrow">▼</span>
      </button>
      
      {isOpen && (
        <div className="multi-select-menu">
          {options.map(option => (
            <label key={option.id} className="multi-select-item">
              <input 
                type="checkbox" 
                checked={selectedIds.has(option.id)}
                onChange={() => handleToggle(option.id)}
              />
              <span className="item-label">{option.label}</span>
            </label>
          ))}
          {options.length === 0 && (
            <div className="multi-select-empty">No options available</div>
          )}
        </div>
      )}
    </div>
  );
};

export default MultiSelectDropdown;
