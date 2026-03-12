# AgentZero 🕵️
### LLM-Powered Autonomous Penetration Testing Agent

> ⚠️ **ETHICAL USE ONLY** — AgentZero is designed exclusively for testing isolated lab environments.
> Never use against systems you don't own or have explicit permission to test.

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 15+
- Docker Desktop
- IntelliJ IDEA
- Gemini API Key (free at https://aistudio.google.com)

---

### Step 1: Clone & Open
```bash
# Open this folder in IntelliJ IDEA
# It will auto-detect the Maven project
```

### Step 2: Set Up PostgreSQL
```sql
CREATE DATABASE agentzero;
CREATE USER postgres WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE agentzero TO postgres;
```

### Step 3: Configure API Key
Edit `src/main/resources/application.properties`:
```properties
agentzero.llm.gemini.api-key=YOUR_GEMINI_API_KEY_HERE
spring.datasource.password=yourpassword
```

### Step 4: Start a Vulnerable Target
```bash
# Pull and run DVWA (Damn Vulnerable Web App)
docker pull vulnerables/web-dvwa
docker run -d -p 80:80 --name dvwa vulnerables/web-dvwa

# Or OWASP Juice Shop
docker pull bkimminich/juice-shop
docker run -d -p 3000:3000 --name juiceshop bkimminich/juice-shop
```

### Step 5: Run AgentZero
```bash
mvn spring-boot:run
```

### Step 6: Start a Pentest via API
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"targetIp": "127.0.0.1", "targetPort": 80, "targetName": "DVWA"}'
```

### Step 7: Watch Live via WebSocket
Connect to: `ws://localhost:8080/ws`
Subscribe to: `/topic/session/{sessionId}`

---

## 📁 Project Structure

```
src/main/java/com/agentzero/
├── AgentZeroApplication.java      # Entry point
├── agent/
│   ├── AgentEngine.java           # ⭐ Core ReAct loop
│   └── AgentSessionManager.java   # Session state management
├── llm/
│   ├── GeminiLLMService.java      # Gemini API integration
│   └── SystemPromptBuilder.java   # Prompt engineering
├── tools/
│   ├── PentestTools.java          # All 6 security tools
│   └── ToolRegistry.java          # Tool management
├── docker/
│   └── DockerManager.java         # Container lifecycle
├── api/
│   └── AgentController.java       # REST endpoints
├── websocket/
│   └── AgentEventPublisher.java   # Real-time streaming
├── config/
│   └── WebSocketConfig.java       # WebSocket setup
└── model/
    └── Models.java                # All JPA entities
```

---

## 🔌 API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/sessions` | Start a new pentest |
| GET | `/api/sessions/{id}` | Get session + findings |
| POST | `/api/sessions/{id}/stop` | Stop running session |
| GET | `/api/sessions` | List all sessions |
| GET | `/api/targets` | Available Docker targets |
| GET | `/api/health` | Health check |

---

## 🛠️ Tools Available

| Tool | Description |
|------|-------------|
| `nmap_scan` | Port & service discovery |
| `http_probe` | HTTP request probing |
| `sqli_test` | SQL injection testing |
| `dir_fuzz` | Directory enumeration |
| `banner_grab` | Service banner reading |
| `brute_force` | Credential testing |

---

## 📅 Development Roadmap

- [x] Week 1-2: Project structure & Spring Boot foundation
- [ ] Week 3-4: LLM brain & ReAct loop
- [ ] Week 5-6: Tool executor & all security tools
- [ ] Week 7-8: Full agent end-to-end
- [ ] Week 9-10: React frontend & WebSocket streaming
- [ ] Week 11-12: PDF reports & demo polish

---

## 🧠 How It Works

```
User starts session
      ↓
AgentEngine.runPentest() [async]
      ↓
┌─────────────────────────────────┐
│  ReAct Loop (max 20 steps)      │
│                                 │
│  1. THINK: Send history to LLM  │
│  2. LLM returns JSON decision   │
│  3. If ACT: run security tool   │
│  4. OBSERVE: feed result to LLM │
│  5. Repeat until DONE           │
└─────────────────────────────────┘
      ↓
WebSocket streams every step live
      ↓
Session saved to PostgreSQL
      ↓
PDF report generated
```

---

Built with ❤️ using Java 21, Spring Boot 3, LangChain4j, Gemini API, Docker
