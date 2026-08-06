import React from 'react';
import './CategoryFilter.css';

const CategoryFilter = ({ categories, excludedIds, onChange }) => {
  const handleToggle = (id) => {
    const newExcluded = new Set(excludedIds);
    if (newExcluded.has(id)) {
      newExcluded.delete(id);
    } else {
      newExcluded.add(id);
    }
    onChange(newExcluded);
  };

  const handleToggleAll = (exclude) => {
    if (exclude) {
      onChange(new Set(categories.map(c => c.id)));
    } else {
      onChange(new Set());
    }
  };

  const allExcluded = categories.length > 0 && excludedIds.size === categories.length;
  const noneExcluded = excludedIds.size === 0;

  return (
    <div className="category-filter-container">
      <div className="filter-header">
        <h4>Filter Categories</h4>
        <div className="quick-actions">
          <button 
            className="quick-btn" 
            onClick={() => handleToggleAll(false)}
            disabled={noneExcluded}
          >
            Show All
          </button>
          <button 
            className="quick-btn" 
            onClick={() => handleToggleAll(true)}
            disabled={allExcluded}
          >
            Hide All
          </button>
        </div>
      </div>
      
      <div className="category-chips">
        {categories.map(cat => {
          const isExcluded = excludedIds.has(cat.id);
          return (
            <button
              key={cat.id}
              className={`cat-chip ${isExcluded ? 'excluded' : 'included'}`}
              onClick={() => handleToggle(cat.id)}
            >
              <span className="cat-icon">{cat.icon}</span>
              <span className="cat-name">{cat.name}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default CategoryFilter;
