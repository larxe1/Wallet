import React from 'react';
import { motion } from 'motion/react';
import { SlidersHorizontal, RotateCcw, EyeOff, Check } from 'lucide-react';
import './CategoryFilter.css';

const CategoryFilter = ({ categories = [], excludedIds = new Set(), onChange }) => {
  const safeExcluded = excludedIds instanceof Set ? excludedIds : new Set(excludedIds || []);

  const handleToggle = (id) => {
    const newExcluded = new Set(safeExcluded);
    if (newExcluded.has(id)) {
      newExcluded.delete(id);
    } else {
      newExcluded.add(id);
    }
    onChange(newExcluded);
  };

  const handleToggleAll = (exclude) => {
    if (exclude) {
      onChange(new Set(categories.map((c) => c.id)));
    } else {
      onChange(new Set());
    }
  };

  const totalCount = categories.length;
  const excludedCount = categories.filter((c) => safeExcluded.has(c.id)).length;
  const includedCount = totalCount - excludedCount;
  const allExcluded = totalCount > 0 && includedCount === 0;
  const noneExcluded = excludedCount === 0;

  return (
    <div className="category-filter-container" role="region" aria-label="Category filter">
      <div className="filter-header">
        <div className="filter-title-row">
          <SlidersHorizontal size={15} strokeWidth={2.2} className="filter-title-icon" aria-hidden="true" />
          <h4>Categories</h4>
          <span className="filter-count-pill">
            {includedCount}/{totalCount}
          </span>
        </div>

        <div className="quick-actions">
          <button
            type="button"
            className="quick-btn"
            onClick={() => handleToggleAll(false)}
            disabled={noneExcluded}
            title="Show all categories"
          >
            <RotateCcw size={12} strokeWidth={2.2} aria-hidden="true" />
            <span>Show All</span>
          </button>
          <button
            type="button"
            className="quick-btn"
            onClick={() => handleToggleAll(true)}
            disabled={allExcluded}
            title="Hide all categories"
          >
            <EyeOff size={12} strokeWidth={2.2} aria-hidden="true" />
            <span>Hide All</span>
          </button>
        </div>
      </div>

      {categories.length === 0 ? (
        <p className="category-filter-empty">No categories configured yet.</p>
      ) : (
        <div className="category-chips">
          {categories.map((cat) => {
            const isExcluded = safeExcluded.has(cat.id);
            const isIncluded = !isExcluded;

            return (
              <motion.button
                key={cat.id}
                type="button"
                whileTap={{ scale: 0.96 }}
                className={`cat-chip ${isExcluded ? 'excluded' : 'included'}`}
                aria-pressed={isIncluded}
                aria-label={`${cat.name}: ${isIncluded ? 'included' : 'excluded'}`}
                onClick={() => handleToggle(cat.id)}
              >
                <span className="cat-icon" aria-hidden="true">{cat.icon}</span>
                <span className="cat-name">{cat.name}</span>
                <span className="cat-state-indicator" aria-hidden="true">
                  {isIncluded ? <Check size={12} strokeWidth={2.6} /> : <EyeOff size={11} strokeWidth={2} />}
                </span>
              </motion.button>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CategoryFilter;
