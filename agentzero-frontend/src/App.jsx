import React, { useState, useCallback, useEffect } from 'react';
import Header from './components/Header';
import StatsBar from './components/StatsBar';
import TargetPanel from './components/TargetPanel';
import LiveLog from './components/LiveLog';
import VulnPanel from './components/VulnPanel';
import { useWebSocket } from './hooks/useWebSocket';
import { api } from './api/agentApi';

export default function App() {
  const [sessionId,      setSessionId]      = useState(null);
  const [sessionStatus,  setSessionStatus]  = useState(null);
  const [events,         setEvents]         = useState([]);
  const [vulnerabilities,setVulnerabilities]= useState([]);
  const [isRunning,      setIsRunning]      = useState(false);
  const [step,           setStep]           = useState(0);
  const [maxSteps]                          = useState(20);

  const handleEvent = useCallback((event) => {
    setEvents(prev => [...prev, event]);

    switch (event.type) {
      case 'SESSION_START':
        setSessionStatus('RUNNING');
        setIsRunning(true);
        break;
      case 'THINK':
      case 'ACT':
      case 'OBSERVE':
        if (event.step) setStep(event.step);
        break;
      case 'VULNERABILITY_FOUND':
        setVulnerabilities(prev => [...prev, {
          name: event.message?.replace('Vulnerability found: ', '').split(' [')[0] || 'Unknown',
          severity: event.data || 'MEDIUM',
          tool: event.toolName,
          evidence: event.data,
        }]);
        break;
      case 'DONE':
        setSessionStatus('COMPLETED');
        setIsRunning(false);
        break;
      case 'STOPPED':
        setSessionStatus('STOPPED');
        setIsRunning(false);
        break;
      case 'ERROR':
        setSessionStatus('FAILED');
        setIsRunning(false);
        break;
      default:
        break;
    }
  }, []);

  const { connected } = useWebSocket(sessionId, handleEvent);

  // Poll session for vulnerabilities
  useEffect(() => {
    if (!sessionId || !isRunning) return;
    const iv = setInterval(async () => {
      try {
        const res = await api.getSession(sessionId);
        if (res.data?.vulnerabilities?.length > 0) {
          setVulnerabilities(res.data.vulnerabilities.map(v => ({
            name: v.name || v['name'],
            severity: v.severity || v['severity'],
            tool: v.tool || v['tool'],
            evidence: v.evidence || v['evidence'],
          })));
        }
      } catch (e) {}
    }, 5000);
    return () => clearInterval(iv);
  }, [sessionId, isRunning]);

  const handleStart = async (ip, port, name) => {
    try {
      setEvents([]);
      setVulnerabilities([]);
      setStep(0);
      setSessionStatus('STARTING');
      const res = await api.startSession(ip, port, name);
      setSessionId(res.data.sessionId);
      setIsRunning(true);
    } catch (e) {
      console.error('Failed to start session', e);
      alert('Failed to start session. Is the backend running?');
    }
  };

  const handleStop = async () => {
    if (!sessionId) return;
    try {
      await api.stopSession(sessionId);
      setIsRunning(false);
      setSessionStatus('STOPPED');
    } catch (e) {
      console.error('Failed to stop session', e);
    }
  };

  return (
    <div style={styles.root}>
      {/* Background grid effect */}
      <div style={styles.bgGrid} />

      <Header />
      <StatsBar
        events={events}
        sessionStatus={sessionStatus}
        step={step}
        maxSteps={maxSteps}
      />

      <div style={styles.main}>
        {/* Left: Target control */}
        <div style={styles.leftCol}>
          <div style={styles.panel}>
            <TargetPanel
              onStart={handleStart}
              isRunning={isRunning}
              onStop={handleStop}
              sessionId={sessionId}
            />
          </div>

          {/* Connection status */}
          <div style={styles.connStatus}>
            <div style={{...styles.connDot, background: connected ? 'var(--green)' : 'var(--text-dim)'}} />
            <span style={styles.connLabel}>
              {connected ? 'WEBSOCKET CONNECTED' : sessionId ? 'CONNECTING...' : 'WEBSOCKET IDLE'}
            </span>
          </div>
        </div>

        {/* Center: Live log */}
        <div style={styles.centerCol}>
          <div style={styles.panel}>
            <LiveLog
              events={events}
              isRunning={isRunning}
              step={step}
              maxSteps={maxSteps}
            />
          </div>
        </div>

        {/* Right: Vulnerabilities */}
        <div style={styles.rightCol}>
          <div style={styles.panel}>
            <VulnPanel vulnerabilities={vulnerabilities} />
          </div>
        </div>
      </div>

      {/* Bottom footer */}
      <div style={styles.footer}>
        <span style={styles.footerText}>
          AGENTZERO v1.0 — FOR AUTHORIZED LAB USE ONLY — JIIT PROJECT
        </span>
      </div>
    </div>
  );
}

const styles = {
  root: { display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden', position: 'relative' },
  bgGrid: {
    position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 0,
    backgroundImage: `
      linear-gradient(rgba(0,212,255,0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0,212,255,0.03) 1px, transparent 1px)
    `,
    backgroundSize: '40px 40px',
  },
  main: { flex: 1, display: 'flex', gap: 0, overflow: 'hidden', position: 'relative', zIndex: 1 },
  leftCol: { width: 260, flexShrink: 0, display: 'flex', flexDirection: 'column',
    borderRight: '1px solid var(--border)', background: 'var(--bg-panel)' },
  centerCol: { flex: 1, overflow: 'hidden', background: 'var(--bg-deep)' },
  rightCol: { width: 280, flexShrink: 0, borderLeft: '1px solid var(--border)', background: 'var(--bg-panel)' },
  panel: { flex: 1, padding: 16, overflow: 'hidden', display: 'flex', flexDirection: 'column', height: '100%' },
  connStatus: { flexShrink: 0, display: 'flex', alignItems: 'center', gap: 6,
    padding: '8px 16px', borderTop: '1px solid var(--border)' },
  connDot: { width: 6, height: 6, borderRadius: '50%' },
  connLabel: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 1 },
  footer: { flexShrink: 0, padding: '6px 24px', borderTop: '1px solid var(--border)',
    background: 'var(--bg-void)', display: 'flex', justifyContent: 'center' },
  footerText: { fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-dim)', letterSpacing: 2 },
};
