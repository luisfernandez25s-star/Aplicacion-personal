#!/bin/bash

# Bash script for macOS and Linux

OS="$(uname -s)"
if [ "$OS" == "Darwin" ]; then
    URL="https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
else
    URL="https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
fi

DEST_DIR="./platform-tools"

if [ ! -f "$DEST_DIR/adb" ]; then
    echo "Downloading Android Platform Tools..."
    curl -L $URL -o platform-tools.zip
    echo "Extracting..."
    unzip -o platform-tools.zip
    rm platform-tools.zip
fi

export PATH=$PATH:$(pwd)/platform-tools

echo "--- ADB Status ---"
adb devices

echo "--- Connecting to device via TCP/IP ---"
read -p "Enter device IP address: " IP
adb tcpip 5555
adb connect $IP:5555

echo "--- Pair device (if needed) ---"
read -p "Do you want to pair? (y/n): " CHOICE
if [ "$CHOICE" == "y" ]; then
    read -p "Enter pairing address (IP:PORT): " PAIR_IP
    read -p "Enter pairing code: " PAIR_CODE
    adb pair $PAIR_IP $PAIR_CODE
fi

echo "--- Logs ---"
adb logcat *:S SensorService:D WearDataManager:D MongoRepository:D
