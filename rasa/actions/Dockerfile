
FROM rasa/rasa-sdk:3.6.2 AS builder

WORKDIR /app

COPY requirements.txt .

USER root

RUN /opt/venv/bin/pip install --upgrade pip && /opt/venv/bin/pip install --no-cache-dir -r requirements.txt

USER 1001

COPY . /app

EXPOSE 5055

CMD ["run", "actions"]