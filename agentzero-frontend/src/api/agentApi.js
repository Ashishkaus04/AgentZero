import axios from 'axios';

const BASE = 'http://localhost:8080/api';

export const api = {
  health:       () => axios.get(`${BASE}/health`),
  getTargets:   () => axios.get(`${BASE}/targets`),
  startSession: (targetIp, targetPort, targetName) =>
    axios.post(`${BASE}/sessions`, { targetIp, targetPort: parseInt(targetPort), targetName }),
  stopSession:  (id) => axios.post(`${BASE}/sessions/${id}/stop`),
  getSession:   (id) => axios.get(`${BASE}/sessions/${id}`),
  getSessions:  () => axios.get(`${BASE}/sessions`),
};
