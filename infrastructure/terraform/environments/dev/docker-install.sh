#!/bin/bash

set -e

# Amazon Linux 2 usa yum, não apt-get
yum update -y
yum install -y docker

systemctl enable docker
systemctl start docker

mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

usermod -aG docker ec2-user

sudo yum install git -y