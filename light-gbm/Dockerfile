# Use a lightweight official Python image
FROM python:3.11-slim

# Prevents Python from writing pyc files and ensures output is flushed
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV DEBIAN_FRONTEND=noninteractive

# Install necessary system dependencies and uWSGI build tools
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        gcc \
        python3-dev \
        libssl-dev \
        libffi-dev \
        wget \
        curl && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy requirements file first (for better caching)
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application code
COPY . .

# Expose Flask default port
EXPOSE 5000

# Command to run the app with uWSGI
CMD ["uwsgi", "--http", "0.0.0.0:5000", "--wsgi-file", "main.py", "--callable", "app"]
