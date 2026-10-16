#!/bin/sh
# Start the Ollama server, pull gemma2:2b once it's up, then keep serving.
set -e

# Start the server in the background
ollama serve &
server_pid=$!

# Wait for the server to accept connections, then pull the model.
# The app degrades gracefully (AI fallback) while this is in progress.
until ollama list >/dev/null 2>&1; do
  echo "ollama-entrypoint: waiting for server..."
  sleep 1
done

echo "ollama-entrypoint: pulling gemma2:2b (one-time, cached in volume)..."
ollama pull gemma2:2b

# Hand control back to the server process
wait "$server_pid"
