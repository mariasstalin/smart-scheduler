FROM rasa/rasa-sdk:3.6.2 AS builder

WORKDIR /app

COPY ./requirements.txt .

USER root

RUN apt-get update && apt-get install -y wget curl \
    && /opt/venv/bin/pip install --upgrade pip \
    && /opt/venv/bin/pip install --no-cache-dir -r requirements.txt \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER 1001

COPY . /app

EXPOSE 5055

CMD ["start", "--actions", "actions"]
