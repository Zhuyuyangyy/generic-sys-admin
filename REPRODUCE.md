# REPRODUCE.md - generic-sys-admin

## Prerequisites

- **Python**: 3.10+
- **OS**: Linux / Windows
- **GPU**: Not required
- **Java**: Spring Boot backend (separate setup)
- **Node.js**: Vue 3 frontend (separate setup)

## Install (Python monitoring component)

```bash
cd generic-sys-admin
pip install -r requirements.txt
```

Dependencies: flask, psutil

## Run

```bash
python main.py
```

## Expected Outputs

- Enterprise Resource Management System (ERMS)
- RBAC permission model
- NL business flow orchestration
- Real-time status monitoring (WebSocket)
- AI service integration (MiniMax TTS/image/video)
- Full-chain audit (AOP)

## Known Issues

- Python component is monitoring only; full system requires Spring Boot + Vue 3
- Flask + psutil is lightweight
- No test suite for Python component
- Full stack setup requires Java and Node.js
