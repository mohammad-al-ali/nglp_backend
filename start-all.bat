@echo off
cd /d D:\ProjectFifthYear\NGLP

start "Spring Boot" cmd /k "cd backend.v1 && mvn spring-boot:run"
start "Frontend" cmd /k "cd frontend.v1 && npm run dev"  
start "Python Microservice" cmd /k "cd extract_transcription_microservice && python -m uvicorn server:app --port 8000"

echo All services launched!