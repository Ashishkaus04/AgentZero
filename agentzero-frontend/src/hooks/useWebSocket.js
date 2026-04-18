import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket(sessionId, onEvent) {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);

  const onEventRef = useRef(onEvent);
  useEffect(() => { onEventRef.current = onEvent; }, [onEvent]);

  useEffect(() => {
    if (!sessionId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/session/${sessionId}`, (msg) => {
          try {
            const event = JSON.parse(msg.body);
            onEventRef.current(event);
          } catch (e) {
            console.error('Failed to parse WS message', e);
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error('STOMP error', frame),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [sessionId]);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
  }, []);

  return { connected, disconnect };
}
