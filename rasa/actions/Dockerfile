# Use official Rasa SDK image compatible with Rasa 3.6.x
FROM rasa/rasa-sdk:3.6.2

# Set working directory
WORKDIR /app

# Copy actions and dependency list
COPY . /app

# Install additional dependencies
COPY ./requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

# Expose Rasa action server port
EXPOSE 5055

# Start the Rasa action server
CMD ["run", "actions"]
