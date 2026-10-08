#!/usr/bin/env bash
# ==============================================================================
# RR JAGGERY TRADERS — OVHcloud VPS Provisioning Script
# Sprint 10 — Production Deployment
# ==============================================================================
# Run as: sudo bash vps-provision.sh
# Target: OVHcloud VPS, Ubuntu 24.04 LTS
# ==============================================================================
set -euo pipefail

echo "======================================================================"
echo " RR Jaggery Traders — VPS Provisioning Script"
echo " Sprint 10 | OVHcloud VPS | Ubuntu 24.04 LTS"
echo "======================================================================"

# ------------------------------------------------------------------
# 1. OS Update
# ------------------------------------------------------------------
echo "[1/8] Updating OS packages..."
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get upgrade -y
apt-get install -y ca-certificates curl gnupg lsb-release ufw unzip htop logrotate git

# ------------------------------------------------------------------
# 2. UFW Firewall Configuration
# ------------------------------------------------------------------
echo "[2/8] Configuring UFW firewall..."
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh         # Port 22
ufw allow http        # Port 80
ufw allow https       # Port 443
# Explicitly DENY public exposure of internal ports
ufw deny 5432/tcp     # PostgreSQL - internal only
ufw deny 8081/tcp     # auth-service - internal only
ufw deny 8082/tcp     # commerce-service - internal only
ufw deny 8083/tcp     # customer-finance-service - internal only
ufw deny 8084/tcp     # inventory-procurement-service - internal only
ufw deny 8086/tcp     # production-service - internal only
ufw --force enable
echo "UFW status:"
ufw status verbose

# ------------------------------------------------------------------
# 3. Install Docker Engine (Official)
# ------------------------------------------------------------------
echo "[3/8] Installing Docker Engine..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
echo "Docker installed:"
docker version --format "Docker Engine {{.Server.Version}}"
docker compose version

# ------------------------------------------------------------------
# 4. Create Deployment User (non-root)
# ------------------------------------------------------------------
echo "[4/8] Setting up deployment user..."
if ! id "deploy" &>/dev/null; then
  useradd -m -s /bin/bash deploy
  usermod -aG docker deploy
  echo "User 'deploy' created and added to docker group."
else
  usermod -aG docker deploy
  echo "User 'deploy' already exists; added to docker group."
fi

# ------------------------------------------------------------------
# 5. Create Project Directory Structure
# ------------------------------------------------------------------
echo "[5/8] Creating project directories..."
mkdir -p /opt/rr-jaggery
mkdir -p /var/backups/rr-jaggery
mkdir -p /var/log/rr-jaggery
chmod 700 /var/backups/rr-jaggery
chown -R deploy:deploy /opt/rr-jaggery
chown -R deploy:deploy /var/log/rr-jaggery

# ------------------------------------------------------------------
# 6. Configure Log Rotation
# ------------------------------------------------------------------
echo "[6/8] Setting up log rotation..."
cat > /etc/logrotate.d/rr-jaggery << 'EOF'
/var/log/rr-jaggery/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 deploy deploy
}
EOF

# ------------------------------------------------------------------
# 7. Configure Docker Disk Cleanup (weekly cron)
# ------------------------------------------------------------------
echo "[7/8] Setting up Docker image pruning cron job..."
cat > /etc/cron.weekly/docker-cleanup << 'EOF'
#!/usr/bin/env bash
# Remove unused Docker images/containers older than 7 days
# NEVER removes postgres_data, uploads_data, or active containers
docker image prune -af --filter "until=168h" >> /var/log/rr-jaggery/docker-cleanup.log 2>&1
docker container prune -f >> /var/log/rr-jaggery/docker-cleanup.log 2>&1
docker volume prune -f --filter "label!=keep" >> /var/log/rr-jaggery/docker-cleanup.log 2>&1
EOF
chmod +x /etc/cron.weekly/docker-cleanup

# ------------------------------------------------------------------
# 8. System Info Summary
# ------------------------------------------------------------------
echo "[8/8] System information:"
echo "--- OS ---"
lsb_release -a
echo "--- CPU ---"
nproc
echo "--- Memory ---"
free -h
echo "--- Disk ---"
df -h /
echo "======================================================================"
echo " Provisioning complete."
echo " Next: Clone repo, configure .env, run deploy.sh"
echo "======================================================================"
