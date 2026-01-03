#!/usr/bin/env bash

docker network create -d bridge --subnet "172.88.0.0/16" --gateway "172.88.0.1" ingot-net
