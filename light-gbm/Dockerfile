# Use a lightweight official Python image
FROM python:3.11-slim

# Set environment variables for non-interactive commands
ENV DEBIAN_FRONTEND=noninteractive

# Install necessary system dependencies including build tools for uWSGI
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        gcc \
        python3-dev \
        wget \
        libssl-dev \
        libffi-dev \
        && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy requirements file
COPY requirements.txt .

# Install Python dependencies
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy the rest of the application code
COPY . .

# Expose application port
EXPOSE 5000

# Run the app using uWSGI
CMD ["uwsgi", "--http", "0.0.0.0:5000", "--wsgi-file", "main.py", "--callable", "app"]
