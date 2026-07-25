#!/bin/bash
cd "$(dirname "$0")/.."
echo "Generating Javadoc..."
mvn javadoc:javadoc
echo "Javadoc generation complete."
