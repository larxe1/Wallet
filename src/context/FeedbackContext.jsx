import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from 'lucide-react';
import '../components/Feedback.css';

const FeedbackContext = createContext({
  showToast: () => {},
  confirm: async () => false,
});

const TOAST_ICONS = {
  success: CheckCircle2,
  error: AlertCircle,
  warning: AlertTriangle,
  info: Info,
};

export const FeedbackProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);
  const [confirmState, setConfirmState] = useState(null);
  const dialogRef = useRef(null);
  const resolverRef = useRef(null);
  const timersRef = useRef(new Map());

  const dismissToast = useCallback((id) => {
    const timer = timersRef.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timersRef.current.delete(id);
    }
    setToasts((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const showToast = useCallback(
    (message, type = 'info') => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      const normalizedType = TOAST_ICONS[type] ? type : 'info';

      setToasts((prev) => [...prev.slice(-3), { id, message: String(message), type: normalizedType }]);

      const timer = setTimeout(() => {
        dismissToast(id);
      }, 3800);
      timersRef.current.set(id, timer);
    },
    [dismissToast]
  );

  useEffect(() => {
    const timers = timersRef.current;
    return () => {
      timers.forEach((timer) => clearTimeout(timer));
      timers.clear();
      if (resolverRef.current) {
        resolverRef.current(false);
        resolverRef.current = null;
      }
    };
  }, []);

  const resolveConfirm = useCallback((result) => {
    const dialog = dialogRef.current;
    if (dialog?.open) {
      dialog.close();
    }
    const resolve = resolverRef.current;
    resolverRef.current = null;
    setConfirmState(null);
    if (resolve) {
      resolve(Boolean(result));
    }
  }, []);

  const confirm = useCallback(
    ({
      title = 'Confirm action',
      message = 'Are you sure you want to continue?',
      confirmText = 'Confirm',
      cancelText = 'Cancel',
      variant = 'danger',
    } = {}) => {
      if (resolverRef.current) {
        resolverRef.current(false);
        resolverRef.current = null;
      }

      return new Promise((resolve) => {
        resolverRef.current = resolve;
        setConfirmState({ title, message, confirmText, cancelText, variant });
      });
    },
    []
  );

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (confirmState) {
      if (!dialog.open) {
        dialog.showModal();
      }
    } else if (dialog.open) {
      dialog.close();
    }
  }, [confirmState]);

  const handleBackdropClick = (e) => {
    if (e.target === dialogRef.current) {
      resolveConfirm(false);
    }
  };

  const handleDialogCancel = (e) => {
    e.preventDefault();
    resolveConfirm(false);
  };

  const isDanger = confirmState?.variant === 'danger';
  const isWarning = confirmState?.variant === 'warning';

  return (
    <FeedbackContext.Provider value={{ showToast, confirm }}>
      {children}

      {/* Top-right Toast Stack */}
      <div className="feedback-toast-region" aria-live="polite" aria-atomic="false">
        <AnimatePresence initial={false}>
          {toasts.map((toast) => {
            const Icon = TOAST_ICONS[toast.type] || Info;
            return (
              <motion.div
                key={toast.id}
                layout
                initial={{ opacity: 0, x: 24, scale: 0.96 }}
                animate={{ opacity: 1, x: 0, scale: 1 }}
                exit={{ opacity: 0, x: 20, scale: 0.96 }}
                transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
                className={`feedback-toast feedback-toast--${toast.type}`}
                role={toast.type === 'error' ? 'alert' : 'status'}
              >
                <span className={`feedback-toast-icon feedback-toast-icon--${toast.type}`} aria-hidden="true">
                  <Icon size={17} strokeWidth={2.1} />
                </span>
                <p className="feedback-toast-message">{toast.message}</p>
                <button
                  type="button"
                  className="feedback-toast-close"
                  onClick={() => dismissToast(toast.id)}
                  aria-label="Dismiss notification"
                >
                  <X size={15} />
                </button>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>

      {/* Native Accessible Confirm Dialog */}
      <dialog
        ref={dialogRef}
        className="feedback-confirm-dialog"
        aria-labelledby="feedback-confirm-title"
        aria-describedby="feedback-confirm-message"
        onCancel={handleDialogCancel}
        onClick={handleBackdropClick}
      >
        {confirmState && (
          <div className="feedback-confirm-card">
            <div className="feedback-confirm-header">
              <div
                className={`feedback-confirm-badge ${
                  isDanger ? 'is-danger' : isWarning ? 'is-warning' : 'is-info'
                }`}
                aria-hidden="true"
              >
                {isDanger || isWarning ? <AlertTriangle size={20} /> : <Info size={20} />}
              </div>
              <div className="feedback-confirm-copy">
                <h2 id="feedback-confirm-title">{confirmState.title}</h2>
                <p id="feedback-confirm-message">{confirmState.message}</p>
              </div>
            </div>

            <div className="feedback-confirm-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => resolveConfirm(false)}
              >
                {confirmState.cancelText}
              </button>
              <button
                type="button"
                className={`btn ${isDanger ? 'btn-danger' : 'btn-primary'}`}
                onClick={() => resolveConfirm(true)}
                autoFocus
              >
                {confirmState.confirmText}
              </button>
            </div>
          </div>
        )}
      </dialog>
    </FeedbackContext.Provider>
  );
};

export const useFeedback = () => useContext(FeedbackContext);
