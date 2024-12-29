#!/bin/bash

# Start ScyllaDB in the background
/usr/bin/scylla --smp 2 --developer-mode 1 &

# Wait for ScyllaDB to initialize
echo "Waiting for ScyllaDB to initialize..."
sleep 15

# Execute the CQL script if it exists
if [ -f /docker-entrypoint-initdb.d/init.cql ]; then
  echo "Executing init.cql..."
  cqlsh -f /docker-entrypoint-initdb.d/init.cql
else
  echo "No init.cql found. Skipping initialization."
fi

# Bring ScyllaDB to the foreground
wait
