#!/bin/bash

echo "init docker dir"
mkdir -p ./data/jarboot1 ./data/jarboot2 ./logs/jarboot1 ./logs/jarboot2 ./workspace1 ./workspace2
echo "change owner"
sudo chown -R 1000:1000 ./data ./logs ./workspace1 ./workspace2 ./config
echo "success!"
