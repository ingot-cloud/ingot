#!/usr/bin/env bash

docker network create \
  --driver overlay \
  --attachable \
  --subnet 10.10.0.0/16 \
  ingot-overlay

