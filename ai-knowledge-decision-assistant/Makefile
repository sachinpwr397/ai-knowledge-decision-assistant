.PHONY: up down backend-test frontend-build
up:
	docker compose up --build

down:
	docker compose down

backend-test:
	cd backend && mvn test

frontend-build:
	cd frontend && npm install && npm run build
