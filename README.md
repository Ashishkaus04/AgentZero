# AgentZero 🕵️
### LLM-Powered Autonomous Penetration Testing Agent

> ⚠️ **ETHICAL USE ONLY** — AgentZero is designed exclusively for testing isolated, intentionally vulnerable lab environments. Never use against systems you do not own or have explicit written authorization to test.

---

## 📌 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [How It Works](#how-it-works)
4. [Tech Stack](#tech-stack)
5. [Project Structure](#project-structure)
6. [Prerequisites](#prerequisites)
7. [Setup & Installation](#setup--installation)
8. [Running the Project](#running-the-project)
9. [API Reference](#api-reference)
10. [Security Tools](#security-tools)
11. [Target Environments](#target-environments)
12. [Screenshots](#screenshots)
13. [Known Limitations](#known-limitations)
14. [Future Scope](#future-scope)
15. [Disclaimer](#disclaimer)

---

## Project Overview

**AgentZero** is an autonomous penetration testing agent powered by a Large Language Model (LLM). Unlike traditional security scanners that follow fixed scripts, AgentZero uses an AI brain to **reason**, **decide**, and **act** — mimicking the decision-making process of a real penetration tester.

The system takes a target IP and port as input, then autonomously:
- Discovers open ports and running services
- Identifies the technology stack
- Tests for common vulnerabilities (SQL injection, weak credentials, exposed files)
- Adapts its strategy based on what it discovers
- Generates a professional PDF penetration testing report

### What Makes It Unique

Most security tools (Nmap, Burp Suite, Metasploit) are passive — they give you data and leave the analysis to humans. AgentZero is **active** — it reasons about the data, decides what to do next, and executes the next attack step autonomously, all in a continuous loop.

This is achieved using the **ReAct (Reasoning + Acting)** framework where the LLM acts as the "brain" of the agent:

```
THINK → ACT → OBSERVE → THINK → ACT → OBSERVE → ... → DONE
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    React Frontend (Port 3000)                │
│                                                             │
│  ┌─────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │   Target    │  │   Live Attack    │  │  Vulnerability│  │
│  │  Selector   │  │      Log         │  │    Findings   │  │
│  └─────────────┘  └──────────────────┘  └───────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API + WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│                Spring Boot Backend (Port 8080)               │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                   Agent Engine                        │   │
│  │                                                      │   │
│  │   THINK ──► LLM (Gemini) ──► ACT ──► Tool ──► OBSERVE│   │
│  │     ▲                                        │       │   │
│  │     └────────────────────────────────────────┘       │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────┐  ┌─────────────┐  ┌────────────────────┐   │
│  │   Tool     │  │   Docker    │  │    WebSocket        │   │
│  │  Registry  │  │   Manager   │  │    Publisher        │   │
│  └────────────┘  └─────────────┘  └────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    PostgreSQL Database                  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              Docker Victim Environments                      │
│                                                             │
│   ┌──────────┐    ┌───────────┐    ┌──────────────────┐    │
│   │  DVWA    │    │  WebGoat  │    │   OWASP          │    │
│   │  :80     │    │   :8080   │    │   Juice Shop :3000│    │
│   └──────────┘    └───────────┘    └──────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## How It Works

### The ReAct Loop

AgentZero uses the **ReAct (Reasoning + Acting)** framework. Each cycle consists of three phases:

**1. THINK**
The LLM receives the full attack history and reasons about what to do next:
```json
{
  "action": "THINK",
  "reasoning": "Port 80 is open and running Apache. The login page redirects to login.php. Before brute forcing, I should check for SQL injection vulnerabilities.",
  "thought": "I will test the username field with common SQL injection payloads."
}
```

**2. ACT**
The LLM decides which security tool to call and with what parameters:
```json
{
  "action": "ACT",
  "tool": "sqli_test",
  "params": { "url": "http://127.0.0.1/login.php", "parameter": "username" }
}
```

**3. OBSERVE**
The tool executes and its output is fed back to the LLM as context for the next cycle:
```
SQL Injection Test Results
==========================
VULNERABLE with payload: ' OR '1'='1 (detected: sql syntax)
```

This loop repeats up to 20 times or until the LLM decides the assessment is complete and responds with `DONE`.

### Vulnerability Detection

After each tool execution, AgentZero automatically scans the output for indicators of vulnerabilities:

| Indicator | Vulnerability | Severity |
|---|---|---|
| SQL error signatures in response | SQL Injection | HIGH |
| Valid credentials found by brute forcer | Weak/Default Credentials | CRITICAL |
| Accessible sensitive paths found | Sensitive File Exposed | MEDIUM |
| `Index of /` in HTTP response | Directory Listing Enabled | MEDIUM |
| `/admin`, `/console` returning 200 | Admin Endpoint Exposed | HIGH |

### PDF Report Generation

At the end of every session, a full penetration testing report is auto-generated containing:
- Cover page with session metadata and severity summary
- Executive summary with overall risk rating
- Scope and methodology
- Detailed vulnerability findings with evidence and remediation
- Full attack timeline table
- Prioritized recommendations (Immediate / Short Term / Long Term)

---

## Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.2 | Core application server |
| AI Brain | Google Gemini 2.0 Flash | LLM reasoning engine |
| LLM Integration | LangChain4j 0.36.2 | Java LLM framework |
| Database | PostgreSQL 15 | Session and findings storage |
| Real-time | Spring WebSocket + STOMP | Live event streaming |
| Containers | Docker Java SDK 3.3.4 | Victim environment management |
| PDF Reports | iText7 7.2.5 | Professional report generation |
| Frontend | React 18 + Tailwind CSS | Dashboard UI |
| HTTP Client | Java HttpClient | HTTP probing tool |
| Build Tool | Maven | Dependency management |

---

## Project Structure

```
AgentZero/
├── pom.xml                                          ← Maven dependencies
├── src/main/
│   ├── resources/
│   │   └── application.properties                  ← Configuration
│   └── java/com/agentzero/
│       ├── AgentZeroApplication.java               ← Entry point
│       ├── agent/
│       │   ├── AgentEngine.java                    ← ⭐ ReAct loop core
│       │   └── AgentSessionManager.java            ← Session state management
│       ├── api/
│       │   ├── AgentController.java                ← REST endpoints
│       │   └── ReportController.java               ← PDF download endpoint
│       ├── config/
│       │   ├── AsyncConfig.java                    ← Thread pool configuration
│       │   └── WebSocketConfig.java                ← WebSocket setup
│       ├── docker/
│       │   └── DockerManager.java                  ← Container lifecycle
│       ├── llm/
│       │   ├── GeminiLLMService.java               ← Gemini API integration
│       │   └── SystemPromptBuilder.java            ← Prompt engineering
│       ├── model/
│       │   ├── AttackStep.java                     ← JPA entity
│       │   ├── PentestSession.java                 ← JPA entity
│       │   ├── Target.java                         ← JPA entity
│       │   ├── ToolCall.java                       ← Tool call model
│       │   ├── ToolResult.java                     ← Tool result model
│       │   └── Vulnerability.java                  ← JPA entity
│       ├── report/
│       │   └── PentestReportGenerator.java         ← PDF generation
│       ├── tools/
│       │   ├── PentestTool.java                    ← Tool interface
│       │   ├── NmapScanner.java                    ← Port scanning
│       │   ├── HttpProber.java                     ← HTTP probing
│       │   ├── SqlInjectionTester.java             ← SQLi testing
│       │   ├── DirectoryFuzzer.java                ← Directory enumeration
│       │   ├── BannerGrabber.java                  ← Service banner reading
│       │   ├── BruteForcer.java                    ← Credential testing
│       │   └── ToolRegistry.java                   ← Tool management
│       └── websocket/
│           └── AgentEventPublisher.java            ← Real-time streaming

agentzero-frontend/
├── package.json
└── src/
    ├── App.jsx                                     ← Main layout & state
    ├── index.css                                   ← Dark hacker theme
    ├── api/agentApi.js                             ← Backend API calls
    ├── hooks/useWebSocket.js                       ← WebSocket hook
    └── components/
        ├── Header.jsx                              ← Logo + server status
        ├── StatsBar.jsx                            ← Live stats bar
        ├── TargetPanel.jsx                         ← Target selector
        ├── LiveLog.jsx                             ← Attack log stream
        └── VulnPanel.jsx                           ← Findings display
```

---

## Prerequisites

- **Java 21+** — [Download](https://adoptium.net)
- **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** — [Download](https://nodejs.org)
- **PostgreSQL 15+** — [Download](https://www.postgresql.org/download)
- **Docker Desktop** — [Download](https://www.docker.com/products/docker-desktop)
- **Nmap** — [Download](https://nmap.org/download.html)
- **Google Gemini API Key** — [Get free key](https://aistudio.google.com)
- **IntelliJ IDEA** (recommended) — [Download](https://www.jetbrains.com/idea)

---

## Setup & Installation

### Step 1: Clone the Repository
```bash
git clone https://github.com/yourusername/AgentZero.git
cd AgentZero
```

### Step 2: Set Up PostgreSQL
```sql
CREATE DATABASE agentzero;
CREATE USER postgres WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE agentzero TO postgres;
```

### Step 3: Configure Application
Copy the example config and fill in your values:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agentzero
spring.datasource.username=postgres
spring.datasource.password=YOUR_DB_PASSWORD

agentzero.llm.gemini.api-key=YOUR_GEMINI_API_KEY
agentzero.llm.gemini.model=gemini-2.0-flash
```

### Step 4: Start Victim Environments
```bash
# DVWA — Damn Vulnerable Web Application
docker run -d -p 80:80 --name dvwa vulnerables/web-dvwa

# OWASP Juice Shop
docker run -d -p 3000:3000 --name juiceshop bkimminich/juice-shop

# OWASP WebGoat
docker run -d -p 8080:8080 --name webgoat webgoat/goat-and-wolf
```

### Step 5: Install Frontend Dependencies
```bash
cd agentzero-frontend
npm install
```

---

## Running the Project

### Start Backend
```bash
# From project root
mvn spring-boot:run
```
Backend starts at `http://localhost:8080`

### Start Frontend
```bash
# From agentzero-frontend directory
npm start
```
Frontend opens at `http://localhost:3000`

### Launch First Pentest
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d "{\"targetIp\":\"127.0.0.1\",\"targetPort\":80,\"targetName\":\"DVWA\"}"
```

Or use the dashboard — select DVWA and click **LAUNCH PENTEST**.

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/sessions` | Start a new pentest session |
| `GET` | `/api/sessions` | List all sessions |
| `GET` | `/api/sessions/{id}` | Get session details and findings |
| `POST` | `/api/sessions/{id}/stop` | Stop a running session |
| `GET` | `/api/targets` | List available Docker targets |
| `GET` | `/api/reports/{id}` | Download PDF report |
| `GET` | `/api/health` | Health check |

### WebSocket
Connect to `ws://localhost:8080/ws` and subscribe to `/topic/session/{sessionId}` for real-time events.

**Event Types:**

| Type | Description |
|---|---|
| `SESSION_START` | Pentest session initiated |
| `THINK` | LLM reasoning step |
| `ACT` | Tool being executed |
| `OBSERVE` | Tool result received |
| `VULNERABILITY_FOUND` | New finding detected |
| `DONE` | Assessment complete |
| `ERROR` | Error occurred |

---

## Security Tools

| Tool | Name | Description |
|---|---|---|
| `nmap_scan` | Port Scanner | Discovers open ports and service versions using Nmap. Falls back to Java socket scanning if Nmap is unavailable. |
| `http_probe` | HTTP Prober | Sends HTTP GET/POST requests and analyzes responses, headers, and body content. |
| `banner_grab` | Banner Grabber | Connects to a port and reads the raw service banner to identify software and version. |
| `dir_fuzz` | Directory Fuzzer | Probes 25+ common paths to discover hidden directories, config files, and admin interfaces. |
| `sqli_test` | SQL Injection Tester | Injects 5 common SQL payloads and analyzes responses for SQL error signatures. |
| `brute_force` | Brute Forcer | Tests 10 common username/password combinations against login endpoints. |

---

## Target Environments

All testing is conducted against intentionally vulnerable applications running in isolated Docker containers:

| Target | Docker Image | Port | Vulnerability Focus |
|---|---|---|---|
| DVWA | `vulnerables/web-dvwa` | 80 | SQL injection, XSS, file inclusion, weak auth |
| OWASP Juice Shop | `bkimminich/juice-shop` | 3000 | Modern web vulns, broken auth, sensitive data |
| OWASP WebGoat | `webgoat/goat-and-wolf` | 8080 | Java app vulns, injection, broken access control |

---

## Known Limitations

- **Rate Limits** — Gemini free tier allows 10 requests/minute. The agent has a configurable delay between steps (`agentzero.agent.step-delay-ms`) to stay within limits.
- **JSON Parsing** — Gemini occasionally responds with natural language instead of JSON. The parser handles this gracefully and retries.
- **Nmap on Windows** — Nmap requires administrator privileges for SYN scans (`-sS`). Use basic scan type or run IntelliJ as administrator.
- **In-Memory Sessions** — Sessions are stored in memory and lost on restart. Future versions will persist to PostgreSQL.
- **Tool Scope** — Current tools cover web application testing. Network-level and binary exploitation are out of scope.

---

## Future Scope

- **Persistent Sessions** — Full JPA persistence for sessions and findings
- **Multi-target Campaigns** — Scan multiple targets in sequence
- **CVE Integration** — Automatically look up CVEs for discovered service versions
- **Custom Payloads** — Upload custom wordlists for fuzzing and brute force
- **Scheduled Scans** — Cron-based automated scanning
- **Team Collaboration** — Multi-user support with role-based access
- **Export Formats** — JSON and HTML report export alongside PDF
- **Plugin System** — Add custom tools without modifying core code

---

## Disclaimer

AgentZero was developed as an academic project at **Jaypee Institute of Information Technology (JIIT)**. It is intended solely for educational purposes and authorized security testing within isolated lab environments.

**Legal Notice:** Unauthorized use of this tool against systems you do not own or have explicit written permission to test is illegal under the Computer Fraud and Abuse Act (CFAA), the IT Act 2000 (India), and equivalent laws in other jurisdictions. The developers assume no liability for misuse of this software.

**Ethical Use:** Always obtain written authorization before testing any system. Only use AgentZero against intentionally vulnerable applications (DVWA, WebGoat, Juice Shop) in isolated Docker environments.

---

*Built with Java, Spring Boot, LangChain4j, React, and Google Gemini — JIIT Academic Project 2026*
