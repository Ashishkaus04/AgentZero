import React, { useEffect, useRef } from 'react';

const EVENT_STYLES = {
  SESSION_START: { color: 'var(--accent)',  icon: '⬡', label: 'INIT'    },
  THINK:         { color: 'var(--purple)',  icon: '◈', label: 'THINK'   },
  ACT:           { color: 'var(--yellow)',  icon: '▶', label: 'ACT'     },
  OBSERVE:       { color: 'var(--green)',   icon: '◉', label: 'OBSERVE' },
  VULNERABILITY_FOUND: { color: 'var(--red)', icon: '⚡', label: 'VULN' },
  DONE:          { color: 'var(--green)',   icon: '✓', label: 'DONE'    },
  STOPPED:       { color: 'var(--orange)',  icon: '■', label: 'STOP'    },
  ERROR:         { color: 'var(--red)',     icon: '✕', label: 'ERROR'   },
};

function LogEntry({ event, index }) {
  const s = EVENT_STYLES[event.type] || { color: 'var(--text-secondary)', icon: '·', label: event.type };
  const time = event.timestamp
    ? new Date(event.timestamp).toTimeString().slice(0, 8)
    : '--:--:--';

  return (
    <div style={{...styles.entry, animationDelay: `${index * 20}ms`}}>
      <span style={styles.time}>{time}</span>
      <span style={{...styles.icon, color: s.color}}>{s.icon}</span>
      <span style={{...styles.badge, color: s.color, borderColor: s.color}}>{s.label}</span>
      {event.step && <span style={styles.step}>#{event.step}</span>}
      {event.toolName && <span style={styles.tool}>[{event.toolName}]</span>}
      <span style={{...styles.message, color: event.type === 'ERROR' ? 'var(--red)' : 'var(--text-primary)'}}>
        {event.message}
      </span>
    </div>
  );
}

export default function LiveLog({ events, isRunning, step, maxSteps }) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  const progress = maxSteps > 0 ? (step / maxSteps) * 100 : 0;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.headerLeft}>
          <span style={styles.title}>LIVE ATTACK LOG</span>
          <span style={styles.count}>{events.length} events</span>
        </div>
        <div style={styles.headerRight}>
          {isRunning && (
            <>
              <div style={styles.spinner} />
              <span style={styles.runningLabel}>RUNNING — STEP {step}/{maxSteps}</span>
            </>
          )}
          {!isRunning && events.length > 0 && (
            <span style={styles.doneLabel}>● COMPLETE</span>
          )}
        </div>
      </div>

      {isRunning && (
        <div style={styles.progressBar}>
          <div style={{...styles.progressFill, width: `${progress}%`}} />
        </div>
      )}

      <div style={styles.log}>
        {events.length === 0 && (
          <div style={styles.empty}>
            <div style={styles.emptyIcon}>⬡</div>
            <div style={styles.emptyText}>AWAITING TARGET ACQUISITION</div>
            <div style={styles.emptySub}>Select a target and launch pentest to begin</div>
          </div>
        )}
        {events.map((event, i) => (
          <LogEntry key={i} event={event} index={i} />
        ))}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}

const styles = {
  container: { display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '0 0 10px 0', borderBottom: '1px solid var(--border)', marginBottom: 8, flexShrink: 0 },
  headerLeft: { display: 'flex', alignItems: 'center', gap: 12 },
  headerRight: { display: 'flex', alignItems: 'center', gap: 8 },
  title: { fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--accent)', letterSpacing: 3 },
  count: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-dim)',
    background: 'var(--bg-void)', padding: '2px 6px', borderRadius: 2, border: '1px solid var(--border)' },
  spinner: { width: 8, height: 8, border: '1px solid var(--green)', borderTopColor: 'transparent',
    borderRadius: '50%', animation: 'spin 0.8s linear infinite' },
  runningLabel: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--green)', letterSpacing: 1 },
  doneLabel: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--green)' },
  progressBar: { height: 2, background: 'var(--border)', borderRadius: 1, marginBottom: 8, flexShrink: 0 },
  progressFill: { height: '100%', background: 'linear-gradient(90deg, var(--accent), var(--green))',
    borderRadius: 1, transition: 'width 1s ease', boxShadow: '0 0 6px var(--accent)' },
  log: { flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 3, paddingRight: 4 },
  entry: { display: 'flex', alignItems: 'flex-start', gap: 8, padding: '4px 6px',
    borderRadius: 3, animation: 'slide-in 0.2s ease forwards',
    borderLeft: '2px solid transparent', transition: 'background 0.1s',
    fontFamily: 'var(--font-mono)', fontSize: 11, lineHeight: 1.5 },
  time: { color: 'var(--text-dim)', flexShrink: 0, minWidth: 56 },
  icon: { flexShrink: 0, fontSize: 10, marginTop: 2 },
  badge: { flexShrink: 0, fontSize: 9, border: '1px solid', padding: '1px 4px',
    borderRadius: 2, letterSpacing: 1, minWidth: 50, textAlign: 'center', marginTop: 1 },
  step: { color: 'var(--text-dim)', flexShrink: 0, fontSize: 10 },
  tool: { color: 'var(--yellow)', flexShrink: 0, fontSize: 10 },
  message: { flex: 1, wordBreak: 'break-word', fontSize: 11 },
  empty: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
    justifyContent: 'center', gap: 8, opacity: 0.4, padding: 40 },
  emptyIcon: { fontSize: 40, color: 'var(--accent)' },
  emptyText: { fontFamily: 'var(--font-display)', fontSize: 13, letterSpacing: 4, color: 'var(--text-secondary)' },
  emptySub: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-dim)' },
};
