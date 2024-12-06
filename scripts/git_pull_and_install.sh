#!/bin/bash

# Exit on error
set -e

# Function to determine the available pip
set_pip_path() {
    if command -v /usr/local/bin/pip >/dev/null 2>&1; then
        echo "Using pip from /usr/local/bin"
        PIP_CMD="/usr/local/bin/pip"
    elif command -v /usr/bin/pip >/dev/null 2>&1; then
        echo "Using pip from /usr/bin"
        PIP_CMD="/usr/bin/pip"
    elif command -v pip >/dev/null 2>&1; then
        echo "Using pip from PATH"
        PIP_CMD="pip"
    else
        echo "Error: No pip installation found!"
        exit 1
    fi
}

# Set the pip path
set_pip_path

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

# Change to the desired directory before cloning
cd /root/do-not-delete-scripts || { echo "Failed to change directory to /root/do-not-delete-scripts"; exit 1; }

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
    echo "Installing Python package using pip..."
    $PIP_CMD install .
else
    echo "No installation file found. Please check the repository."
    exit 1
fi

echo "Installation completed successfully!"
