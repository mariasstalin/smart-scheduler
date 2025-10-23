# Use official Rasa SDK image compatible with Rasa 3.6.x
FROM rasa/rasa-sdk:3.6.2

# Set working directory
WORKDIR /app

# Copy actions and dependency list
COPY . /app
COPY ./requirements.txt /app/requirements.txt

# Switch to root temporarily to install Python dependencies
USER root

# Upgrade pip and install dependencies inside the virtual environment
RUN /opt/venv/bin/pip install --upgrade pip && /opt/venv/bin/pip install --no-cache-dir -r /app/requirements.txt

# Switch back to the 'rasa' user (default for Rasa SDK)
USER 1001

# Expose Rasa action server port
EXPOSE 5055

# Start the Rasa action server
CMD ["run", "actions"]
