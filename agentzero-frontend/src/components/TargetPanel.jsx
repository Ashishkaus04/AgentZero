import React, { useState, useEffect } from 'react';
import { api } from '../api/agentApi';

const PRESET_TARGETS = [
  { id: 'dvwa',      name: 'DVWA',            desc: 'Damn Vulnerable Web App', ip: '127.0.0.1', port: '80',   color: 'var(--red)' },
  { id: 'webgoat',   name: 'WebGoat',          desc: 'OWASP WebGoat',           ip: '127.0.0.1', port: '8080', color: 'var(--orange)' },
  { id: 'juiceshop', name: 'Juice Shop',       desc: 'OWASP Juice Shop',        ip: '127.0.0.1', port: '3000', color: 'var(--yellow)' },
  { id: 'custom',    name: 'Custom Target',    desc: 'Manual IP/Port entry',    ip: '',          port: '',     color: 'var(--purple)' },
];

export default function TargetPanel({ onStart, isRunning, onStop, sessionId }) {
  const [selected, setSelected] = useState(PRESET_TARGETS[0]);
  const [customIp, setCustomIp] = useState('');
  const [customPort, setCustomPort] = useState('');

  const handleStart = () => {
    const ip   = selected.id === 'custom' ? customIp   : selected.ip;
    const port = selected.id === 'custom' ? customPort : selected.port;
    if (!ip || !port) return;
    onStart(ip, port, selected.name);
  };

  return (
    <div style={styles.panel}>
      <div style={styles.panelHeader}>
        <span style={styles.panelTitle}>TARGET SELECTION</span>
        <span style={styles.panelBadge}>ISOLATED LAB</span>
      </div>

      <div style={styles.targets}>
        {PRESET_TARGETS.map(t => (
          <button key={t.id} onClick={() => setSelected(t)}
            style={{...styles.targetBtn, borderColor: selected.id === t.id ? t.color : 'var(--border)',
              background: selected.id === t.id ? `rgba(${t.color === 'var(--red)' ? '255,51,85' : t.color === 'var(--orange)' ? '255,136,0' : t.color === 'var(--yellow)' ? '255,204,0' : '136,68,255'},0.08)` : 'transparent'}}>
            <div style={{...styles.targetDot, background: t.color}} />
            <div style={styles.targetInfo}>
              <div style={{...styles.targetName, color: selected.id === t.id ? t.color : 'var(--text-primary)'}}>{t.name}</div>
              <div style={styles.targetDesc}>{t.desc}</div>
            </div>
            {selected.id === t.id && t.id !== 'custom' && (
              <div style={styles.targetMeta}>
                <span style={styles.metaChip}>{t.ip}</span>
                <span style={styles.metaChip}>:{t.port}</span>
              </div>
            )}
          </button>
        ))}
      </div>

      {selected.id === 'custom' && (
        <div style={styles.customFields}>
          <div style={styles.fieldGroup}>
            <label style={styles.fieldLabel}>TARGET IP</label>
            <input style={styles.input} value={customIp}
              onChange={e => setCustomIp(e.target.value)} placeholder="192.168.1.1" />
          </div>
          <div style={styles.fieldGroup}>
            <label style={styles.fieldLabel}>PORT</label>
            <input style={styles.input} value={customPort}
              onChange={e => setCustomPort(e.target.value)} placeholder="80" />
          </div>
        </div>
      )}

      <div style={styles.disclaimer}>
        ⚠ AUTHORIZED LAB ENVIRONMENTS ONLY
      </div>

      {!isRunning ? (
        <button onClick={handleStart} style={styles.launchBtn}>
          <span style={styles.launchIcon}>▶</span>
          LAUNCH PENTEST
        </button>
      ) : (
        <button onClick={onStop} style={styles.stopBtn}>
          <span style={styles.stopIcon}>■</span>
          ABORT SESSION
        </button>
      )}

      {sessionId && (
        <div style={styles.sessionId}>
          <span style={styles.sessionLabel}>SESSION</span>
          <span style={styles.sessionVal}>{sessionId.slice(0, 18)}...</span>
        </div>
      )}

      {sessionId && !isRunning && (
          <a href={`http://localhost:8080/api/reports/${sessionId}`}
             target="_blank" rel="noopener noreferrer" style={styles.reportBtn}>
            ⬇ DOWNLOAD PDF REPORT
          </a>
      )}
    </div>
  );
}

const styles = {
  panel: { display: 'flex', flexDirection: 'column', gap: 12, height: '100%' },
  panelHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 },
  panelTitle: { fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--accent)', letterSpacing: 3 },
  panelBadge: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--green)', letterSpacing: 2,
    border: '1px solid var(--green)', padding: '2px 6px', borderRadius: 2 },
  targets: { display: 'flex', flexDirection: 'column', gap: 6 },
  targetBtn: { display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px',
    border: '1px solid', borderRadius: 4, cursor: 'pointer', transition: 'all 0.2s', textAlign: 'left',
    width: '100%' },
  targetDot: { width: 8, height: 8, borderRadius: '50%', flexShrink: 0 },
  targetInfo: { flex: 1 },
  targetName: { fontFamily: 'var(--font-ui)', fontSize: 13, fontWeight: 600, letterSpacing: 1 },
  targetDesc: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-dim)', marginTop: 2 },
  targetMeta: { display: 'flex', gap: 4 },
  metaChip: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-secondary)',
    background: 'var(--bg-void)', padding: '1px 5px', borderRadius: 2, border: '1px solid var(--border)' },
  customFields: { display: 'flex', gap: 8 },
  fieldGroup: { flex: 1, display: 'flex', flexDirection: 'column', gap: 4 },
  fieldLabel: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 2 },
  input: { background: 'var(--bg-void)', border: '1px solid var(--border-bright)', borderRadius: 3,
    padding: '6px 10px', color: 'var(--text-primary)', fontFamily: 'var(--font-mono)', fontSize: 12,
    outline: 'none', width: '100%' },
  disclaimer: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--orange)',
    letterSpacing: 1, textAlign: 'center', padding: '6px', border: '1px solid rgba(255,136,0,0.2)',
    borderRadius: 3, background: 'rgba(255,136,0,0.05)' },
  launchBtn: { padding: '12px', background: 'transparent',
    border: '1px solid var(--green)', borderRadius: 4, color: 'var(--green)',
    fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 700, letterSpacing: 3,
    cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    transition: 'all 0.2s', boxShadow: '0 0 12px rgba(0,255,136,0.1)' },
  stopBtn: { padding: '12px', background: 'rgba(255,51,85,0.1)',
    border: '1px solid var(--red)', borderRadius: 4, color: 'var(--red)',
    fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 700, letterSpacing: 3,
    cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 },
  launchIcon: { fontSize: 10 },
  stopIcon: { fontSize: 10 },
  sessionId: { display: 'flex', gap: 8, alignItems: 'center', padding: '6px 8px',
    background: 'var(--bg-void)', borderRadius: 3, border: '1px solid var(--border)' },
  sessionLabel: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 2 },
  sessionVal: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--accent)' },
  reportBtn: {
    display: 'block', padding: '10px', textAlign: 'center',
    background: 'transparent', border: '1px solid var(--accent)',
    borderRadius: 4, color: 'var(--accent)', fontFamily: 'var(--font-mono)',
    fontSize: 11, letterSpacing: 2, textDecoration: 'none',
    transition: 'all 0.2s',
  },
};
