#!/usr/bin/env bash
# FlashTest (Bilim Scan) — backend + admin sayt + kursant sayti uchun yagona deploy skripti.
#
# Ishlatish (root bilan):
#   ./deploy.sh              # hammasi: backend + admin + user
#   ./deploy.sh backend      # faqat backend
#   ./deploy.sh web          # faqat ikkala sayt
#   ./deploy.sh admin        # faqat admin/o'qituvchi sayti
#   ./deploy.sh user         # faqat kursant sayti
#   ./deploy.sh --no-pull    # git pull qilmasdan (har qanday buyruq bilan birga)
#
# Birinchi ishga tushishda serverni o'zi tayyorlaydi: paketlar, firewall, MongoDB (Docker),
# maxfiy sozlamalar (/etc/bilim-scan/bilim-scan.env), systemd servisi, Nginx va SSL.
# Keyingi ishga tushishlarda mavjud sozlamalarga tegmaydi, faqat yangi versiyani chiqaradi.

set -Eeuo pipefail

# ============================ SOZLAMALAR ============================
DOMAIN="flashtest.uz"
ADMIN_DOMAIN="admin.${DOMAIN}"
API_DOMAIN="api.${DOMAIN}"
CERTBOT_EMAIL=""                       # bo'sh bo'lsa SSL email'siz olinadi

BACKEND_SRC="/root/bilim_scan_server"
ADMIN_SRC="/root/bilimscan_admin/web"
USER_SRC="/root/bilimscan_user/web"

APP_DIR="/opt/bilim-scan"
ENV_FILE="/etc/bilim-scan/bilim-scan.env"
SERVICE="bilim-scan"
WWW_USER="/var/www/${DOMAIN}"
WWW_ADMIN="/var/www/${ADMIN_DOMAIN}"
APP_PORT=4257
# ====================================================================

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step() { echo -e "\n${GREEN}==> $*${NC}"; }
warn() { echo -e "${YELLOW}[!] $*${NC}"; }
die()  { echo -e "${RED}[X] $*${NC}" >&2; exit 1; }
trap 'die "Xatolik: ${BASH_COMMAND} (qator ${LINENO})"' ERR

[[ $EUID -eq 0 ]] || die "Skriptni root bilan ishga tushiring"

TARGET="all"; PULL=1
for arg in "$@"; do
    case "$arg" in
        all|backend|web|admin|user) TARGET="$arg" ;;
        --no-pull) PULL=0 ;;
        -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
        *) die "Noma'lum argument: $arg (yordam: ./deploy.sh --help)" ;;
    esac
done

NEW_SECRETS=""

# ---------------------------------------------------------------- tizim
setup_packages() {
    local pkgs=(curl git rsync ufw ca-certificates openssl nginx certbot python3-certbot-nginx
                openjdk-21-jdk-headless docker.io)
    local missing=()
    for p in "${pkgs[@]}"; do dpkg -s "$p" &>/dev/null || missing+=("$p"); done
    if ((${#missing[@]})); then
        step "Paketlar o'rnatilmoqda: ${missing[*]}"
        apt-get update -y
        DEBIAN_FRONTEND=noninteractive apt-get install -y "${missing[@]}"
    fi
    systemctl enable --now docker nginx >/dev/null

    JAVA_HOME=$(ls -d /usr/lib/jvm/java-21-openjdk-* 2>/dev/null | head -1)
    [[ -x "$JAVA_HOME/bin/java" ]] || die "Java 21 topilmadi (/usr/lib/jvm/java-21-openjdk-*)"
    export JAVA_HOME
}

setup_firewall() {
    if ! ufw status | grep -q "Status: active"; then
        step "Firewall yoqilmoqda (22, 80, 443)"
        ufw allow OpenSSH >/dev/null
        ufw allow 'Nginx Full' >/dev/null
        ufw --force enable
    fi
}

# ---------------------------------------------------------------- sozlamalar
setup_env() {
    if [[ -f "$ENV_FILE" ]]; then return; fi
    step "Maxfiy sozlamalar yaratilmoqda: $ENV_FILE"
    local mongo_pass jwt admin_pass
    mongo_pass=$(openssl rand -hex 24)
    jwt=$(openssl rand -hex 48)
    admin_pass=$(openssl rand -base64 12 | tr -dc 'A-Za-z0-9' | head -c 14)
    mkdir -p "$(dirname "$ENV_FILE")"
    cat > "$ENV_FILE" <<EOF
# FlashTest backend sozlamalari. O'zgartirgandan keyin: systemctl restart ${SERVICE}
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=${APP_PORT}
MONGO_PASSWORD=${mongo_pass}
SPRING_DATA_MONGODB_URI=mongodb://dbadmin:${mongo_pass}@127.0.0.1:27017/bilim_scan?authSource=admin
JWT_SECRET=${jwt}
JWT_EXPIRATION_MS=10800000
CORS_ALLOWED_ORIGINS=https://${DOMAIN},https://www.${DOMAIN},https://${ADMIN_DOMAIN}
DISCOVERY_ENABLED=false
MAX_UPLOAD_SIZE=500MB
BACKUP_DIR=${APP_DIR}/backups
JAVA_OPTS=-Xms256m -Xmx512m
# Bazada ADMIN bo'lmasa shu login/parol bilan yaratiladi (bor bo'lsa hech narsa qilmaydi)
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=${admin_pass}
EOF
    chmod 600 "$ENV_FILE"
    NEW_SECRETS="admin / ${admin_pass}"
}

env_get() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2-; }

mongo_shell() { # 4.4 image'da faqat "mongo", 5+ da "mongosh" bor
    if docker exec mongo sh -c 'command -v mongosh' &>/dev/null; then echo mongosh; else echo mongo; fi
}

setup_mongo() {
    [[ -n "$(env_get MONGO_PASSWORD)" ]] || die "$ENV_FILE ichida MONGO_PASSWORD yo'q"

    # MongoDB 5.0+ protsessorda AVX talab qiladi. AVX bo'lmasa 4.4 ishlatiladi.
    local image="mongo:8.0"
    if ! grep -qw avx /proc/cpuinfo; then
        image="mongo:4.4"
        warn "Protsessorda AVX yo'q — MongoDB 4.4 ishlatiladi"
    fi

    if docker ps -a --format '{{.Names}}' | grep -qx mongo; then
        local current
        current=$(docker inspect -f '{{.Config.Image}}' mongo)
        if [[ "$current" != "$image" && "$image" == "mongo:4.4" ]]; then
            # AVX'siz serverda 5+ versiya hech qachon ishga tushmagan — unda ma'lumot yo'q
            step "Ishlamaydigan $current konteyneri o'chirilib, $image bilan qayta yaratilmoqda"
            docker rm -f mongo >/dev/null
            docker volume rm mongo-data >/dev/null 2>&1 || true
        else
            docker start mongo >/dev/null
        fi
    fi

    if ! docker ps -a --format '{{.Names}}' | grep -qx mongo; then
        step "MongoDB konteyneri yaratilmoqda ($image)"
        docker run -d --name mongo --restart unless-stopped \
            -p 127.0.0.1:27017:27017 \
            -v mongo-data:/data/db \
            -e MONGO_INITDB_ROOT_USERNAME=dbadmin \
            -e MONGO_INITDB_ROOT_PASSWORD="$(env_get MONGO_PASSWORD)" \
            "$image" >/dev/null
    fi

    local i shell
    for i in $(seq 1 30); do
        shell=$(mongo_shell)
        docker exec mongo "$shell" --quiet -u dbadmin -p "$(env_get MONGO_PASSWORD)" \
            --authenticationDatabase admin --eval 'db.runCommand({ping:1}).ok' &>/dev/null && return
        sleep 2
    done
    docker logs --tail 20 mongo || true
    die "MongoDB ishga tushmadi (yuqoridagi log)"
}

setup_service() {
    mkdir -p "$APP_DIR/uploads/profiles" "$APP_DIR/backups"
    cat > "/etc/systemd/system/${SERVICE}.service" <<EOF
[Unit]
Description=FlashTest (Bilim Scan) backend
After=network-online.target docker.service
Wants=network-online.target
Requires=docker.service

[Service]
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=${JAVA_HOME}/bin/java \$JAVA_OPTS -jar ${APP_DIR}/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable "$SERVICE" &>/dev/null
}

# ---------------------------------------------------------------- nginx + ssl
write_site() { # $1=fayl nomi, keyin config matni stdin'dan. Mavjud bo'lsa tegilmaydi (certbot o'zgartirgan bo'ladi)
    local f="/etc/nginx/sites-available/$1"
    if [[ ! -f "$f" ]]; then cat > "$f"; else cat > /dev/null; fi
    ln -sf "$f" "/etc/nginx/sites-enabled/$1"
}

setup_nginx() {
    mkdir -p "$WWW_USER" "$WWW_ADMIN"
    cat > /etc/nginx/snippets/flutter-web.conf <<'EOF'
index index.html;
# Flutter fayl nomlari hash'lanmagan — brauzer har safar ETag bilan tekshiradi (o'zgarmagan bo'lsa 304)
add_header Cache-Control "no-cache" always;
gzip on;
gzip_types text/plain text/css application/javascript application/json application/wasm image/svg+xml;
gzip_min_length 1024;
location / {
    try_files $uri $uri/ /index.html;
}
EOF

    write_site "$DOMAIN" <<EOF
server {
    listen 80;
    server_name ${DOMAIN} www.${DOMAIN};
    root ${WWW_USER};
    include snippets/flutter-web.conf;
}
EOF
    write_site "$ADMIN_DOMAIN" <<EOF
server {
    listen 80;
    server_name ${ADMIN_DOMAIN};
    root ${WWW_ADMIN};
    include snippets/flutter-web.conf;
}
EOF
    write_site "$API_DOMAIN" <<EOF
server {
    listen 80;
    server_name ${API_DOMAIN};
    client_max_body_size 500M;
    proxy_request_buffering off;
    location / {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
}
EOF
    rm -f /etc/nginx/sites-enabled/default
    nginx -t -q
    systemctl reload nginx
}

setup_ssl() {
    local existing domains=() d
    existing=$(certbot certificates 2>/dev/null || true)
    for d in "$DOMAIN" "www.$DOMAIN" "$ADMIN_DOMAIN" "$API_DOMAIN"; do
        if ! getent ahostsv4 "$d" &>/dev/null; then
            warn "DNS yo'q: $d — SSL'ga qo'shilmadi (A yozuvni qo'shib, skriptni qayta ishga tushiring)"
            continue
        fi
        domains+=("$d")
    done
    ((${#domains[@]})) || return 0

    local need=0
    for d in "${domains[@]}"; do
        grep -Eq "Domains:.* ${d//./\\.}( |$)" <<<"$existing" || need=1
    done
    ((need)) || return 0

    step "SSL sertifikat olinmoqda: ${domains[*]}"
    local args=(--nginx --redirect --agree-tos -n --expand --cert-name "$DOMAIN")
    if [[ -n "$CERTBOT_EMAIL" ]]; then args+=(-m "$CERTBOT_EMAIL"); else args+=(--register-unsafely-without-email); fi
    for d in "${domains[@]}"; do args+=(-d "$d"); done
    certbot "${args[@]}" || warn "SSL olinmadi — saytlar hozircha http:// orqali ishlaydi. DNS'ni tekshirib qayta ishga tushiring."
}

# ---------------------------------------------------------------- deploy
git_pull() { # $1=papka
    ((PULL)) || return 0
    if [[ -d "$1/.git" ]]; then
        step "git pull: $1"
        git -C "$1" pull --ff-only
    fi
}

deploy_backend() {
    [[ -d "$BACKEND_SRC" ]] || die "Backend papkasi topilmadi: $BACKEND_SRC"
    git_pull "$BACKEND_SRC"

    step "Backend build qilinmoqda (Java 21)"
    chmod +x "$BACKEND_SRC/mvnw"
    (cd "$BACKEND_SRC" && ./mvnw -B -q clean package -DskipTests)
    local jar
    jar=$(ls "$BACKEND_SRC"/target/*.jar | grep -v -- '-plain' | head -1)
    [[ -f "$jar" ]] || die "JAR topilmadi: $BACKEND_SRC/target"

    [[ -f "$APP_DIR/app.jar" ]] && cp "$APP_DIR/app.jar" "$APP_DIR/app.jar.bak"
    cp "$jar" "$APP_DIR/app.jar"

    step "Backend qayta ishga tushirilmoqda"
    systemctl restart "$SERVICE"
    if wait_backend; then
        echo "Backend ishlayapti: http://127.0.0.1:${APP_PORT}"
        return
    fi

    journalctl -u "$SERVICE" -n 40 --no-pager || true
    if [[ -f "$APP_DIR/app.jar.bak" ]]; then
        warn "Yangi versiya ishga tushmadi — oldingi versiyaga qaytarilmoqda"
        cp "$APP_DIR/app.jar.bak" "$APP_DIR/app.jar"
        systemctl restart "$SERVICE"
        wait_backend && die "Yangi versiyada xato bor (yuqoridagi log). Oldingi versiya qayta ishga tushirildi."
    fi
    die "Backend ishga tushmadi. Log: journalctl -u ${SERVICE} -n 200"
}

wait_backend() {
    local i code
    for i in $(seq 1 60); do
        code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "http://127.0.0.1:${APP_PORT}/api/auth/login" \
            -H 'Content-Type: application/json' -d '{}' || true)
        [[ "$code" =~ ^[234] ]] && return 0
        systemctl is-active --quiet "$SERVICE" || { sleep 2; systemctl is-active --quiet "$SERVICE" || return 1; }
        sleep 2
    done
    return 1
}

deploy_site() { # $1=nomi $2=manba web papka $3=nginx papka
    local name=$1 src=$2 dst=$3
    git_pull "$(dirname "$src")"
    [[ -f "$src/index.html" && -f "$src/main.dart.js" ]] \
        || die "$name: $src ichida Flutter web build topilmadi (index.html, main.dart.js bo'lishi kerak)"
    step "$name sayti yuklanmoqda: $src -> $dst"
    mkdir -p "$dst"
    rsync -a --delete "$src/" "$dst/"
    chown -R www-data:www-data "$dst"
}

check_web_builds() {
    if [[ -f "$ADMIN_SRC/main.dart.js" && -f "$USER_SRC/main.dart.js" ]] \
        && cmp -s "$ADMIN_SRC/main.dart.js" "$USER_SRC/main.dart.js"; then
        warn "Admin va user build'lari bir xil! APP_TYPE to'g'ri berilganini tekshiring (--dart-define=APP_TYPE=admin|student)"
    fi
}

# ---------------------------------------------------------------- main
setup_packages
setup_firewall
setup_env
setup_mongo
setup_service
setup_nginx

case "$TARGET" in
    all)     deploy_backend; check_web_builds
             deploy_site "Admin" "$ADMIN_SRC" "$WWW_ADMIN"; deploy_site "User" "$USER_SRC" "$WWW_USER" ;;
    backend) deploy_backend ;;
    web)     check_web_builds
             deploy_site "Admin" "$ADMIN_SRC" "$WWW_ADMIN"; deploy_site "User" "$USER_SRC" "$WWW_USER" ;;
    admin)   deploy_site "Admin" "$ADMIN_SRC" "$WWW_ADMIN" ;;
    user)    deploy_site "User" "$USER_SRC" "$WWW_USER" ;;
esac

setup_ssl
nginx -t -q && systemctl reload nginx

scheme=http; [[ -d "/etc/letsencrypt/live/$DOMAIN" ]] && scheme=https
step "Tayyor"
echo "  Kursant:  ${scheme}://${DOMAIN}"
echo "  Admin:    ${scheme}://${ADMIN_DOMAIN}"
echo "  API:      ${scheme}://${API_DOMAIN}/api"
if [[ -n "$NEW_SECRETS" ]]; then
    echo
    warn "Birinchi ADMIN login/parol: ${NEW_SECRETS}"
    warn "Barcha parollar shu faylda: ${ENV_FILE} — uni saqlab qo'ying!"
fi
