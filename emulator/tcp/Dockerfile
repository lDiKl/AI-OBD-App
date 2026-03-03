FROM python:3.11-slim

WORKDIR /app

# Copy emulator script and scenarios
COPY elm327_tcp_emulator.py .
COPY scenarios/ ./scenarios/

# No pip install needed — stdlib only

EXPOSE 35000

CMD ["python", "elm327_tcp_emulator.py"]
