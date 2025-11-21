# VuNet Light Agent

Lightweight Spring Boot agent that collects metrics and forwards them to a backend. Includes Docker, Compose, and CI setup.

## Build

From the nested Maven project:

```bash
cd vu-light-agent
./mvnw -B clean package
```

## Run

```bash
java -jar vu-light-agent/target/vu-light-agent-0.0.1-SNAPSHOT.jar
```

Environment overrides:

```bash
# PowerShell
$env:AGENT_PORT = "9010"
$env:BACKEND_URL = "http://localhost:8080/api/metrics"
java -jar vu-light-agent/target/vu-light-agent-0.0.1-SNAPSHOT.jar
```

## Health

```bash
curl http://localhost:9001/health
```

Expected JSON contains: status, agentName, backendUrl, timestamp.

## Logs

Logs write to `vu-light-agent/logs/agent.log`.

## Docker

```bash
cd vu-light-agent
docker build -t vunet-light-agent .
docker run -d -p 9001:9001 -e BACKEND_URL="http://host.docker.internal:8080/api/metrics" vunet-light-agent
```

Or with compose (includes mock backend):

```bash
cd vu-light-agent
docker-compose up --build
```

## Linux systemd

Copy JAR to `/opt/vunet`, use the service template `vu-light-agent/deploy/vunet-agent.service`, then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now vunet-agent
```

## Windows (NSSM)

Use the script `vu-light-agent/scripts/nssm-install.ps1` (run as Administrator).


