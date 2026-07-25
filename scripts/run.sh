#!/bin/bash
cd "$(dirname "$0")/.."
echo "Starting Ether Society Simulation..."
mvn exec:java
