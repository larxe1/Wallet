import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import BalanceCard from './BalanceCard';
import './WalletCardStack.css';

export default function WalletCardStack({
  walletsData = [],
  selectedWalletId = 'all',
  onSelectWallet,
  isPrivacyMode = false,
  onQuickAdd,
}) {
  const shouldReduceMotion = useReducedMotion();

  const totalCombinedIncome = walletsData.reduce(
    (sum, item) => sum + (Number(item.totalIncome) || 0),
    0
  );
  const totalCombinedExpenses = walletsData.reduce(
    (sum, item) => sum + (Number(item.totalExpenses) || 0),
    0
  );

  const cards = [
    {
      id: 'all',
      name: 'All Wallets',
      color: '#28C991',
      totalIncome: totalCombinedIncome,
      totalExpenses: totalCombinedExpenses,
      isCombined: true,
      walletCount: walletsData.length,
    },
    ...walletsData.map((w) => ({
      id: w.wallet.id,
      name: w.wallet.name,
      color: w.wallet.color || '#28C991',
      totalIncome: w.totalIncome,
      totalExpenses: w.totalExpenses,
      isCombined: false,
      walletCount: 0,
    })),
  ];

  const foundIndex = cards.findIndex((c) => c.id === selectedWalletId);
  const activeIndex = foundIndex >= 0 ? foundIndex : 0;

  const visibleBehind = Math.min(2, Math.max(0, cards.length - 1));
  const frontOffsetY = visibleBehind * 20;
  const deckHeight = frontOffsetY + 224;

  const handleCycle = (delta) => {
    if (cards.length <= 1 || !onSelectWallet) return;
    const nextIdx = (activeIndex + delta + cards.length) % cards.length;
    onSelectWallet(cards[nextIdx].id);
  };

  return (
    <section className="wallet-stack-section" aria-label="My Wallets">
      <div className="wallet-stack-header">
        <div className="wallet-stack-title-group">
          <h2 className="wallet-stack-heading">My Wallets</h2>
          {cards.length > 1 && (
            <div className="wallet-stack-nav">
              <button
                type="button"
                className="wallet-cycle-btn"
                onClick={() => handleCycle(-1)}
                aria-label="Previous wallet card"
                title="Previous wallet"
              >
                <ChevronLeft size={15} />
              </button>
              <span className="wallet-cycle-index" aria-live="polite">
                {activeIndex + 1}/{cards.length}
              </span>
              <button
                type="button"
                className="wallet-cycle-btn"
                onClick={() => handleCycle(1)}
                aria-label="Next wallet card"
                title="Next wallet"
              >
                <ChevronRight size={15} />
              </button>
            </div>
          )}
        </div>

        <Link to="/settings" className="dash-section-link">
          <span>Manage</span>
          <ChevronRight size={14} />
        </Link>
      </div>

      <div className="wallet-stack-deck" style={{ height: `${deckHeight}px` }}>
        {cards.map((card, index) => {
          const depth = (index - activeIndex + cards.length) % cards.length;
          const isFront = depth === 0;
          const isVisible = depth <= 2;

          const targetY =
            depth === 0
              ? frontOffsetY
              : depth === 1
                ? frontOffsetY - 20
                : 0;
          const targetScale =
            depth === 0 ? 1 : depth === 1 ? 0.94 : depth === 2 ? 0.88 : 0.84;
          const targetOpacity =
            depth === 0 ? 1 : depth === 1 ? 0.94 : depth === 2 ? 0.76 : 0;
          const zIndex = depth === 0 ? 30 : depth === 1 ? 20 : depth === 2 ? 10 : 5;

          return (
            <motion.div
              key={card.id}
              className={`wallet-deck-slot ${isFront ? 'is-active' : ''}`}
              style={{
                zIndex,
                transformOrigin: 'top center',
                pointerEvents: isVisible ? 'auto' : 'none',
              }}
              initial={false}
              animate={{
                y: targetY,
                scale: targetScale,
                opacity: targetOpacity,
              }}
              transition={
                shouldReduceMotion
                  ? { duration: 0.01 }
                  : { type: 'spring', stiffness: 320, damping: 28, mass: 0.85 }
              }
            >
              <BalanceCard
                walletName={card.name}
                walletColor={card.color}
                totalIncome={card.totalIncome}
                totalExpenses={card.totalExpenses}
                isPrivacyMode={isPrivacyMode}
                isFront={isFront}
                isCombined={card.isCombined}
                walletCount={card.walletCount}
                depth={depth}
                onQuickAdd={
                  isFront && onQuickAdd
                    ? (type) => onQuickAdd(type, card.id)
                    : undefined
                }
                onClick={
                  !isFront && onSelectWallet
                    ? () => onSelectWallet(card.id)
                    : undefined
                }
              />
            </motion.div>
          );
        })}
      </div>
    </section>
  );
}
