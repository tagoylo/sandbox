#!/bin/bash

# Exit on error
set -e

# Function to display usage
usage() {
    echo "Usage: $0 <repository-url> [branch-name]"
    exit 1
}

# Check arguments
if [ -z "$1" ]; then
    usage
fi

REPO_URL="$1"
BRANCH_NAME="${2:-main}"  # Default to 'main' if no branch specified
REPO_NAME=$(basename "$REPO_URL" .git)

# Pull the repository
if [ -d "$REPO_NAME" ]; then
    echo "Repository '$REPO_NAME' already exists. Pulling latest changes..."
    cd "$REPO_NAME"
    git pull origin "$BRANCH_NAME"
else
    echo "Cloning repository '$REPO_NAME'..."
    git clone -b "$BRANCH_NAME" "$REPO_URL"
    cd "$REPO_NAME"
fi

# Install the Python package
if [ -f "setup.py" ]; then
    echo "Installing Python package using setup.py..."
    python3 setup.py install
elif [ -f "pyproject.toml" ]; then
    echo "Installing Python package using pip install ...."
    pip install .
else
    echo "No installation file (setup.py or pyproject.toml) found. Please check the repository structure."
    exit 1
fi

echo "Installation completed successfully!"
