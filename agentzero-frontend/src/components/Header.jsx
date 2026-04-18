import React, { useState, useEffect } from 'react';
import { api } from '../api/agentApi';

export default function Header() {
  const [serverUp, setServerUp] = useState(false);
  const [time, setTime] = useState(new Date());

  useEffect(() => {
    const check = async () => {
      try { await api.health(); setServerUp(true); }
      catch { setServerUp(false); }
    };
    check();
    const iv = setInterval(check, 5000);
    return () => clearInterval(iv);
  }, []);

  useEffect(() => {
    const iv = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(iv);
  }, []);

  return (
    <header style={styles.header}>
      {/* Scanline effect */}
      <div style={styles.scanline} />

      <div style={styles.left}>
        <div style={styles.logo}>
          <span style={styles.logoIcon}>⬡</span>
          <div>
            <div style={styles.logoText}>AGENT<span style={styles.logoAccent}>ZERO</span></div>
            <div style={styles.logoSub}>AUTONOMOUS PENTEST AGENT v1.0</div>
          </div>
        </div>
      </div>

      <div style={styles.center}>
        <div style={styles.statusBar}>
          {['RECON', 'EXPLOIT', 'REPORT'].map((s, i) => (
            <div key={s} style={styles.statusItem}>
              <div style={{...styles.statusDot, background: i === 0 ? 'var(--green)' : 'var(--text-dim)'}} />
              <span style={{...styles.statusLabel, color: i === 0 ? 'var(--green)' : 'var(--text-dim)'}}>{s}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={styles.right}>
        <div style={styles.sysInfo}>
          <div style={styles.sysRow}>
            <span style={styles.sysLabel}>SERVER</span>
            <span style={{...styles.sysVal, color: serverUp ? 'var(--green)' : 'var(--red)'}}>
              {serverUp ? '● ONLINE' : '● OFFLINE'}
            </span>
          </div>
          <div style={styles.sysRow}>
            <span style={styles.sysLabel}>TIME</span>
            <span style={styles.sysVal}>{time.toTimeString().slice(0, 8)}</span>
          </div>
        </div>
      </div>
    </header>
  );
}

const styles = {
  header: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '0 24px', height: '64px', flexShrink: 0,
    background: 'linear-gradient(180deg, #071828 0%, var(--bg-panel) 100%)',
    borderBottom: '1px solid var(--border-bright)',
    position: 'relative', overflow: 'hidden',
    boxShadow: '0 4px 24px rgba(0,0,0,0.5)',
  },
  scanline: {
    position: 'absolute', top: 0, left: 0, right: 0, height: '2px',
    background: 'linear-gradient(90deg, transparent, var(--accent), transparent)',
    opacity: 0.4,
  },
  left: { display: 'flex', alignItems: 'center', gap: 12, minWidth: 240 },
  logo: { display: 'flex', alignItems: 'center', gap: 12 },
  logoIcon: { fontSize: 28, color: 'var(--accent)', lineHeight: 1, filter: 'drop-shadow(0 0 8px var(--accent))' },
  logoText: { fontFamily: 'var(--font-display)', fontSize: 20, fontWeight: 900, letterSpacing: 4, color: 'var(--text-primary)' },
  logoAccent: { color: 'var(--accent)' },
  logoSub: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 2, marginTop: 2 },
  center: { flex: 1, display: 'flex', justifyContent: 'center' },
  statusBar: { display: 'flex', gap: 32, alignItems: 'center' },
  statusItem: { display: 'flex', alignItems: 'center', gap: 6 },
  statusDot: { width: 6, height: 6, borderRadius: '50%' },
  statusLabel: { fontFamily: 'var(--font-mono)', fontSize: 11, letterSpacing: 2 },
  right: { minWidth: 240, display: 'flex', justifyContent: 'flex-end' },
  sysInfo: { display: 'flex', flexDirection: 'column', gap: 4, alignItems: 'flex-end' },
  sysRow: { display: 'flex', gap: 8, alignItems: 'center' },
  sysLabel: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 2 },
  sysVal: { fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--accent)' },
};
