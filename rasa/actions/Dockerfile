FROM rasa/rasa-sdk:3.6.0

USER root

# Upgrade pip, setuptools, wheel inside the venv
RUN /opt/venv/bin/pip install --upgrade pip setuptools wheel

# Install dependencies
RUN /opt/venv/bin/pip install --no-cache-dir requests

# Copy action files into container
COPY . /app/actions

# Switch back to non-root user (important for rasa-sdk images)
USER 1001

# Start Rasa Action server
CMD ["start", "--actions", "actions"]
