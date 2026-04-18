import React from 'react';

const SEVERITY_CONFIG = {
  CRITICAL: { color: '#ff0044', bg: 'rgba(255,0,68,0.08)',   rank: 0 },
  HIGH:     { color: '#ff3355', bg: 'rgba(255,51,85,0.08)',  rank: 1 },
  MEDIUM:   { color: '#ff8800', bg: 'rgba(255,136,0,0.08)', rank: 2 },
  LOW:      { color: '#ffcc00', bg: 'rgba(255,204,0,0.08)', rank: 3 },
  INFO:     { color: '#00d4ff', bg: 'rgba(0,212,255,0.06)', rank: 4 },
};

function VulnCard({ vuln }) {
  const sev = SEVERITY_CONFIG[vuln.severity] || SEVERITY_CONFIG.INFO;
  return (
    <div style={{...styles.card, borderColor: sev.color, background: sev.bg}}>
      <div style={styles.cardHeader}>
        <span style={{...styles.severity, color: sev.color, borderColor: sev.color}}>
          {vuln.severity}
        </span>
        <span style={styles.vulnName}>{vuln.name}</span>
      </div>
      {vuln.tool && (
        <div style={styles.cardMeta}>
          <span style={styles.metaLabel}>DETECTED BY</span>
          <span style={styles.metaVal}>{vuln.tool}</span>
        </div>
      )}
      {vuln.evidence && (
        <div style={styles.evidence}>
          {vuln.evidence.slice(0, 120)}{vuln.evidence.length > 120 ? '...' : ''}
        </div>
      )}
    </div>
  );
}

export default function VulnPanel({ vulnerabilities }) {
  const sorted = [...(vulnerabilities || [])].sort((a, b) => {
    const ra = SEVERITY_CONFIG[a.severity]?.rank ?? 5;
    const rb = SEVERITY_CONFIG[b.severity]?.rank ?? 5;
    return ra - rb;
  });

  const counts = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0, INFO: 0 };
  sorted.forEach(v => { if (counts[v.severity] !== undefined) counts[v.severity]++; });

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <span style={styles.title}>FINDINGS</span>
        <span style={styles.total}>{sorted.length} total</span>
      </div>

      <div style={styles.summary}>
        {Object.entries(counts).map(([sev, count]) => (
          count > 0 && (
            <div key={sev} style={styles.summaryItem}>
              <span style={{...styles.summaryCount, color: SEVERITY_CONFIG[sev].color}}>{count}</span>
              <span style={{...styles.summaryLabel, color: SEVERITY_CONFIG[sev].color}}>{sev}</span>
            </div>
          )
        ))}
      </div>

      <div style={styles.list}>
        {sorted.length === 0 && (
          <div style={styles.empty}>
            <div style={styles.emptyIcon}>◈</div>
            <div style={styles.emptyText}>NO FINDINGS YET</div>
          </div>
        )}
        {sorted.map((v, i) => <VulnCard key={i} vuln={v} />)}
      </div>
    </div>
  );
}

const styles = {
  container: { display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    paddingBottom: 10, borderBottom: '1px solid var(--border)', marginBottom: 10, flexShrink: 0 },
  title: { fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--accent)', letterSpacing: 3 },
  total: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-dim)',
    background: 'var(--bg-void)', padding: '2px 6px', borderRadius: 2, border: '1px solid var(--border)' },
  summary: { display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12, flexShrink: 0 },
  summaryItem: { display: 'flex', flexDirection: 'column', alignItems: 'center',
    background: 'var(--bg-void)', border: '1px solid var(--border)', borderRadius: 4,
    padding: '6px 10px', minWidth: 44 },
  summaryCount: { fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 700, lineHeight: 1 },
  summaryLabel: { fontFamily: 'var(--font-mono)', fontSize: 8, letterSpacing: 1, marginTop: 3 },
  list: { flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8, paddingRight: 4 },
  card: { border: '1px solid', borderRadius: 4, padding: '10px 12px',
    animation: 'fade-up 0.3s ease forwards' },
  cardHeader: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 },
  severity: { fontFamily: 'var(--font-mono)', fontSize: 9, border: '1px solid',
    padding: '2px 6px', borderRadius: 2, letterSpacing: 1, flexShrink: 0 },
  vulnName: { fontFamily: 'var(--font-ui)', fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' },
  cardMeta: { display: 'flex', gap: 6, alignItems: 'center', marginBottom: 4 },
  metaLabel: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 1 },
  metaVal: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-secondary)' },
  evidence: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-dim)',
    lineHeight: 1.5, marginTop: 4, wordBreak: 'break-all' },
  empty: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
    justifyContent: 'center', gap: 6, opacity: 0.3, padding: 20 },
  emptyIcon: { fontSize: 24, color: 'var(--accent)' },
  emptyText: { fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--text-dim)', letterSpacing: 3 },
};
