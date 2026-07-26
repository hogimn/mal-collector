SERVICES := \
  discovery-server:8888:1:applications/discovery-server/build/libs/discovery-server.jar \
  gateway-server:8880:1:applications/gateway-server/build/libs/gateway-server.jar \
  anime-server:8881:2:applications/anime-server/build/libs/anime-server.jar \
  poll-server:8883:1:applications/poll-server/build/libs/poll-server.jar \
  mal-server:8884:1:applications/mal-server/build/libs/mal-server.jar

MAL_CLIENT_ID ?= YOUR_MAL_CLIENT_ID

.PHONY: all build start stop status restart

all: build start

build:
	@echo "==> Building runnable JARs..."
	./gradlew build

start: stop
	@echo ""
	@echo "==> Starting all servers in background..."
	@mkdir -p logs
	@for item in $(SERVICES); do \
		name=$$(echo $$item | cut -d: -f1); \
		base_port=$$(echo $$item | cut -d: -f2); \
		count=$$(echo $$item | cut -d: -f3); \
		jar=$$(echo $$item | cut -d: -f4); \
		for i in $$(seq 1 $$count); do \
			port=$$(expr $$base_port + $$i - 1); \
			inst_name="$$name-$$i"; \
			echo "Starting $$inst_name on port $$port..."; \
			case "$$name" in \
				discovery-server) \
					PORT=$$port REDIS_HOST=localhost REDIS_PASSWORD=foobared \
					nohup java -jar $$jar > logs/$$inst_name.log 2>&1 & echo $$! >> .pids ;; \
				gateway-server) \
					PORT=$$port DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
					nohup java -jar $$jar > logs/$$inst_name.log 2>&1 & echo $$! >> .pids ;; \
				anime-server) \
					PORT=$$port DATABASE_URL="jdbc:mysql://localhost:3306/dev_anime?user=uservices&password=uservices" DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
					nohup java -jar $$jar > logs/$$inst_name.log 2>&1 & echo $$! >> .pids ;; \
				poll-server) \
					PORT=$$port DATABASE_URL="jdbc:mysql://localhost:3306/dev_poll?user=uservices&password=uservices" DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
					nohup java -jar $$jar > logs/$$inst_name.log 2>&1 & echo $$! >> .pids ;; \
				mal-server) \
					PORT=$$port MAL_CLIENT_ID="$(MAL_CLIENT_ID)" DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
					nohup java -jar $$jar > logs/$$inst_name.log 2>&1 & echo $$! >> .pids ;; \
			esac; \
		done; \
		if [ "$$name" = "discovery-server" ]; then sleep 3; fi; \
	done
	@echo "==> All servers started! Logs are saved in ./logs/"
	@echo ""

stop:
	@echo ""
	@echo "==> Stopping all servers..."
	@if [ -f .pids ]; then \
		while read pid; do \
			kill -9 $$pid 2>/dev/null || true; \
		done < .pids; \
		rm -f .pids; \
	fi
	@PORTS=""; \
	for item in $(SERVICES); do \
		base_port=$$(echo $$item | cut -d: -f2); \
		count=$$(echo $$item | cut -d: -f3); \
		for i in $$(seq 1 $$count); do \
			port=$$(expr $$base_port + $$i - 1); \
			PORTS="$$PORTS $$port/tcp"; \
		done; \
	done; \
	fuser -k $$PORTS 2>/dev/null || true
	@echo "==> All servers stopped."
	@echo ""

status:
	@echo ""
	@echo "=========================================="
	@echo "          SERVER STATUS CHECK             "
	@echo "=========================================="
	@echo ""
	@printf "%-22s | %-6s | %s\n" "INSTANCE" "PORT" "PID (fuser)"
	@echo "------------------------------------------"
	@for item in $(SERVICES); do \
		name=$$(echo $$item | cut -d: -f1); \
		base_port=$$(echo $$item | cut -d: -f2); \
		count=$$(echo $$item | cut -d: -f3); \
		for i in $$(seq 1 $$count); do \
			port=$$(expr $$base_port + $$i - 1); \
			inst_name="$$name-$$i"; \
			pid=$$(fuser $$port/tcp 2>/dev/null || echo 'OFF'); \
			printf "%-22s | %-6s | %s\n" "$$inst_name" "$$port" "$$pid"; \
		done; \
	done
	@echo "------------------------------------------"
	@echo ""

restart: stop start
