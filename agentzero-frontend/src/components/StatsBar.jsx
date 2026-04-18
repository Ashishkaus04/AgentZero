import React from 'react';

export default function StatsBar({ events, sessionStatus, step, maxSteps }) {
  const thinks   = events.filter(e => e.type === 'THINK').length;
  const acts     = events.filter(e => e.type === 'ACT').length;
  const observes = events.filter(e => e.type === 'OBSERVE').length;
  const errors   = events.filter(e => e.type === 'ERROR').length;

  const stats = [
    { label: 'STATUS',   value: sessionStatus || 'IDLE',  color: sessionStatus === 'RUNNING' ? 'var(--green)' : sessionStatus === 'COMPLETED' ? 'var(--accent)' : 'var(--text-dim)' },
    { label: 'STEP',     value: `${step}/${maxSteps}`,    color: 'var(--yellow)' },
    { label: 'THINKS',   value: thinks,                   color: 'var(--purple)' },
    { label: 'ACTIONS',  value: acts,                     color: 'var(--yellow)' },
    { label: 'OBSERVES', value: observes,                 color: 'var(--green)' },
    { label: 'ERRORS',   value: errors,                   color: errors > 0 ? 'var(--red)' : 'var(--text-dim)' },
  ];

  return (
    <div style={styles.bar}>
      {stats.map((s, i) => (
        <React.Fragment key={s.label}>
          <div style={styles.stat}>
            <span style={styles.label}>{s.label}</span>
            <span style={{...styles.value, color: s.color}}>{s.value}</span>
          </div>
          {i < stats.length - 1 && <div style={styles.divider} />}
        </React.Fragment>
      ))}
    </div>
  );
}

const styles = {
  bar: { display: 'flex', alignItems: 'center', padding: '8px 20px',
    background: 'var(--bg-panel)', borderBottom: '1px solid var(--border)',
    gap: 0, flexShrink: 0 },
  stat: { display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px' },
  label: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 2 },
  value: { fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 700, letterSpacing: 1 },
  divider: { width: 1, height: 20, background: 'var(--border)' },
};
