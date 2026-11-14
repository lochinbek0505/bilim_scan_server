# FlashTest (Bilim Scan) — Ubuntu 26.04 serverga deploy qilish

Yangi, bo'sh Ubuntu 26.04 server uchun. Barcha buyruqlar **root** foydalanuvchisi bilan bajariladi.

| Manzil | Nima turadi | Serverdagi joyi |
|--------|-------------|-----------------|
| `https://api.flashtest.uz/api/...` | Spring Boot backend (+ `/uploads/...` rasmlar) | `/opt/bilim-scan` → `127.0.0.1:4257` |
| `https://admin.flashtest.uz` | Flutter web — admin / o'qituvchi (`APP_TYPE=admin`) | `/var/www/admin.flashtest.uz` |
| `https://flashtest.uz` | Flutter web — kursant / user (`APP_TYPE=student`) | `/var/www/flashtest.uz` |

```
Brauzer ──HTTPS──► Nginx ─┬─ flashtest.uz        → /var/www/flashtest.uz (static)
                          ├─ admin.flashtest.uz  → /var/www/admin.flashtest.uz (static)
                          └─ api.flashtest.uz    → 127.0.0.1:4257 (Spring Boot) ──► MongoDB (Docker, 127.0.0.1:27017)
```

> `<...>` ichidagi qiymatlarni o'zingiznikiga almashtiring.

---

## ⚡ Tez yo'l: bitta skript bilan (`deploy.sh`)

Serverdagi papkalar:
```
/root/bilim_scan_server        ← backend repo (deploy.sh shu yerda)
/root/bilimscan_admin/web      ← admin/o'qituvchi Flutter web build  (APP_TYPE=admin)
/root/bilimscan_user/web       ← kursant Flutter web build           (APP_TYPE=student)
```

**Birinchi marta** (DNS A yozuvlari qo'shilgan bo'lsin, pastdagi 0-bo'lim):
```bash
cd /root/bilim_scan_server
nano deploy.sh            # xohlasangiz CERTBOT_EMAIL="..." ni yozing
chmod +x deploy.sh
./deploy.sh
```
Skript o'zi: paketlarni o'rnatadi, firewall'ni yoqadi, MongoDB'ni (Docker) ishga tushiradi, parollarni generatsiya qilib `/etc/bilim-scan/bilim-scan.env` ga yozadi, backend'ni build qilib systemd servisi qiladi, ikkala saytni `/var/www/...` ga joylaydi, Nginx va SSL'ni sozlaydi. Oxirida **birinchi admin login/paroli** chiqadi — saqlab qo'ying.

**Keyingi safarlar:**
```bash
./deploy.sh               # hammasi (git pull + backend + admin + user)
./deploy.sh backend       # faqat backend
./deploy.sh web           # faqat ikkala sayt
./deploy.sh admin         # faqat admin sayti
./deploy.sh user          # faqat kursant sayti
./deploy.sh --no-pull     # git pull qilmasdan
```

- Papka git repo bo'lsa, skript avval `git pull --ff-only` qiladi.
- Yangi backend ishga tushmasa, skript avtomatik **oldingi versiyaga qaytaradi** va logni ko'rsatadi.
- `web/` ichida `index.html` va `main.dart.js` bo'lmasa — xato beradi, saytga tegmaydi. Admin va user build'lari bir xil bo'lsa ogohlantiradi (`APP_TYPE` adashgan).
- Nginx fayllari va env fayl faqat birinchi marta yaratiladi — keyin qo'lda o'zgartirsangiz, skript ularni ustidan yozmaydi.
- DNS hali tayyor bo'lmagan domen SSL'siz qoladi — DNS tayyor bo'lgach `./deploy.sh` ni qayta ishga tushirsangiz SSL oladi.

**Flutter build'larni Mac'dan serverga yuborish:**
```bash
cd ~/AndroidStudioProjects/bilim_scan
flutter build web --release --dart-define=APP_TYPE=admin   -o build/web_admin
flutter build web --release --dart-define=APP_TYPE=student -o build/web_user
rsync -az --delete build/web_admin/ root@<SERVER_IP>:/root/bilimscan_admin/web/
rsync -az --delete build/web_user/  root@<SERVER_IP>:/root/bilimscan_user/web/
ssh root@<SERVER_IP> '/root/bilim_scan_server/deploy.sh web'
```

Quyidagi bo'limlar — skript aynan nima qilishining qo'lda bajariladigan varianti (tushunish va muammo bo'lganda kerak).

---

## 0. Deploydan oldin

**Backend repo (`bilim_scan_server`)** — quyidagilar commit + push qilingan bo'lsin:
- `spring.data.mongodb.uri` tuzatishi (oldin `spring.mongodb.uri` edi — Spring Boot 3 uni o'qimaydi, Windows serverda ilova parolsiz `test` bazasiga ulangan edi);
- backup eksport/import funksiyasi (`/api/admin/backup/...`);
- `CORS_ALLOWED_ORIGINS` va `DISCOVERY_ENABLED` sozlamalari.

**Flutter repo (`bilim_scan`)** — `lib/core/config/app_config.dart` endi ilova turini build paytida oladi (`--dart-define=APP_TYPE=admin|student`), koddagi qatorni o'zgartirish shart emas. `ApiConfig.serverUrl` standart holatda `https://api.flashtest.uz`.

**DNS** — domen panelida 4 ta **A** yozuv server IP'siga qaratilgan bo'lsin:

| Host | Tur | Qiymat |
|------|-----|--------|
| `flashtest.uz` (`@`) | A | `<SERVER_IP>` |
| `www` | A | `<SERVER_IP>` |
| `admin` | A | `<SERVER_IP>` |
| `api` | A | `<SERVER_IP>` |

Tekshirish (kompyuteringizda): `dig +short api.flashtest.uz` → server IP chiqishi kerak. DNS tarqalmaguncha 10-qadamdagi SSL olinmaydi.

---

## 1. Tizimni tayyorlash

```bash
ssh root@<SERVER_IP>

apt update && apt upgrade -y
timedatectl set-timezone Asia/Tashkent
apt install -y curl git unzip rsync ufw ca-certificates nginx certbot python3-certbot-nginx \
               openjdk-21-jdk-headless docker.io docker-compose-v2
systemctl enable --now docker nginx
java -version     # 21.x bo'lishi kerak
```

## 2. Firewall

Tashqariga faqat SSH, HTTP va HTTPS ochiladi. Backend (4257) va MongoDB (27017) faqat server ichida.

```bash
ufw allow OpenSSH
ufw allow 'Nginx Full'     # 80 + 443
ufw --force enable
ufw status
```

## 3. MongoDB (Docker)

MongoDB'ning rasmiy apt repozitoriyasi Ubuntu 26.04 ni hali qo'llamasligi mumkin, shuning uchun Docker.

```bash
openssl rand -hex 24      # natijani saqlab qo'ying → <MONGO_PASSWORD>

mkdir -p /opt/mongo
cat > /opt/mongo/docker-compose.yml <<'EOF'
services:
  mongo:
    image: mongo:8.0
    container_name: mongo
    restart: unless-stopped
    ports:
      - "127.0.0.1:27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: dbadmin
      MONGO_INITDB_ROOT_PASSWORD: <MONGO_PASSWORD>
    volumes:
      - mongo-data:/data/db
volumes:
  mongo-data:
EOF
chmod 600 /opt/mongo/docker-compose.yml

cd /opt/mongo && docker compose up -d
docker exec mongo mongosh -u dbadmin -p '<MONGO_PASSWORD>' --authenticationDatabase admin --quiet --eval 'db.runCommand({ping:1})'
# { ok: 1 } chiqishi kerak
```

> Port `127.0.0.1` ga bog'langan — Docker UFW'ni chetlab o'tsa ham tashqaridan kirib bo'lmaydi.

## 4. Backend sozlamalari (env fayl)

```bash
openssl rand -base64 48   # → <JWT_SECRET>

mkdir -p /etc/bilim-scan
cat > /etc/bilim-scan/bilim-scan.env <<'EOF'
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=4257
SPRING_DATA_MONGODB_URI=mongodb://dbadmin:<MONGO_PASSWORD>@127.0.0.1:27017/bilim_scan?authSource=admin
JWT_SECRET=<JWT_SECRET>
JWT_EXPIRATION_MS=10800000
CORS_ALLOWED_ORIGINS=https://flashtest.uz,https://www.flashtest.uz,https://admin.flashtest.uz
DISCOVERY_ENABLED=false
MAX_UPLOAD_SIZE=500MB
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=<KUCHLI_ADMIN_PAROLI>
EOF
chmod 600 /etc/bilim-scan/bilim-scan.env
```

| O'zgaruvchi | Vazifasi |
|-------------|----------|
| `SERVER_ADDRESS=127.0.0.1` | Backend faqat Nginx orqali ochiladi |
| `CORS_ALLOWED_ORIGINS` | Faqat shu saytlardan API'ga so'rov ruxsat etiladi |
| `DISCOVERY_ENABLED=false` | LAN uchun UDP 8888 qidiruv o'chiriladi (domen bilan kerak emas) |
| `MAX_UPLOAD_SIZE` | Backup import fayli uchun limit (profil rasmi baribir 5MB) |
| `BOOTSTRAP_ADMIN_*` | Bo'sh bazada birinchi ADMIN'ni yaratadi; keyin `false` qilinadi |

## 5. Backend'ni build qilish

```bash
git clone <BACKEND_GIT_URL> /opt/bilim-scan/src
cd /opt/bilim-scan/src
chmod +x mvnw
./mvnw -B clean package -DskipTests

cp target/bilim_scan-0.0.1-SNAPSHOT.jar /opt/bilim-scan/app.jar
mkdir -p /opt/bilim-scan/uploads/profiles /opt/bilim-scan/backups
```

> Private repo bo'lsa: `ssh-keygen -t ed25519 -N '' -f ~/.ssh/id_ed25519 && cat ~/.ssh/id_ed25519.pub` — kalitni GitHub → repo → Settings → Deploy keys'ga qo'shing va `git@github.com:...` manzil bilan clone qiling.
> `-DskipTests` — test Mongo'siz o'tmaydi.

## 6. systemd servisi

```bash
cat > /etc/systemd/system/bilim-scan.service <<'EOF'
[Unit]
Description=FlashTest (Bilim Scan) backend
After=network-online.target docker.service
Wants=network-online.target
Requires=docker.service

[Service]
WorkingDirectory=/opt/bilim-scan
EnvironmentFile=/etc/bilim-scan/bilim-scan.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/bilim-scan/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now bilim-scan
journalctl -u bilim-scan -f       # Ctrl+C bilan chiqiladi
```

Kutilgan qatorlar: `Tomcat started on port 4257` va `Bootstrap orqali ADMIN foydalanuvchi yaratildi: admin`.

> `WorkingDirectory` muhim: `uploads/` va `backups/` papkalari shunga nisbatan topiladi.
> `-Xmx512m` — RAM 1 GB bo'lsa `384m`, 4 GB+ bo'lsa `1g` qiling.

Lokal tekshirish:
```bash
curl -s -X POST http://127.0.0.1:4257/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<KUCHLI_ADMIN_PAROLI>"}'
# {"token":"eyJ..."} qaytishi kerak
```

## 7. Saytlar uchun papkalar

```bash
mkdir -p /var/www/flashtest.uz /var/www/admin.flashtest.uz
echo 'tayyorlanmoqda' > /var/www/flashtest.uz/index.html
echo 'tayyorlanmoqda' > /var/www/admin.flashtest.uz/index.html
```

## 8. Nginx

```bash
# Flutter web uchun umumiy sozlamalar (ikkala saytda ishlatiladi)
cat > /etc/nginx/snippets/flutter-web.conf <<'EOF'
index index.html;

# Flutter fayl nomlari hash'lanmagan (main.dart.js va h.k.) — brauzer har safar
# ETag orqali tekshiradi, o'zgarmagan bo'lsa 304 qaytadi. Yangi versiya darhol ko'rinadi.
add_header Cache-Control "no-cache" always;

gzip on;
gzip_types text/plain text/css application/javascript application/json application/wasm image/svg+xml;
gzip_min_length 1024;

location / {
    try_files $uri $uri/ /index.html;
}
EOF

# Kursant sayti
cat > /etc/nginx/sites-available/flashtest.uz <<'EOF'
server {
    listen 80;
    server_name flashtest.uz www.flashtest.uz;
    root /var/www/flashtest.uz;
    include snippets/flutter-web.conf;
}
EOF

# Admin / o'qituvchi sayti
cat > /etc/nginx/sites-available/admin.flashtest.uz <<'EOF'
server {
    listen 80;
    server_name admin.flashtest.uz;
    root /var/www/admin.flashtest.uz;
    include snippets/flutter-web.conf;
}
EOF

# Backend API
cat > /etc/nginx/sites-available/api.flashtest.uz <<'EOF'
server {
    listen 80;
    server_name api.flashtest.uz;

    client_max_body_size 500M;      # backup import uchun (MAX_UPLOAD_SIZE bilan bir xil)
    proxy_request_buffering off;

    location / {
        proxy_pass http://127.0.0.1:4257;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
}
EOF

ln -sf /etc/nginx/sites-available/flashtest.uz       /etc/nginx/sites-enabled/
ln -sf /etc/nginx/sites-available/admin.flashtest.uz /etc/nginx/sites-enabled/
ln -sf /etc/nginx/sites-available/api.flashtest.uz   /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

nginx -t && systemctl reload nginx
```

> API'da butun `/` proxy qilinadi — shunda `/api/...` ham, profil rasmlari `/uploads/profiles/...` ham ishlaydi (Flutter `ApiConfig.getFileUrl` rasmni `https://api.flashtest.uz/uploads/...` dan oladi).

## 9. HTTPS (Let's Encrypt)

```bash
certbot --nginx --redirect --agree-tos -m <EMAIL> -n \
  -d flashtest.uz -d www.flashtest.uz -d admin.flashtest.uz -d api.flashtest.uz

systemctl list-timers | grep certbot     # avtomatik yangilanish yoqilganini tekshirish
certbot renew --dry-run
```

Tekshirish:
```bash
curl -I https://flashtest.uz
curl -I https://admin.flashtest.uz
curl -s -X POST https://api.flashtest.uz/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<KUCHLI_ADMIN_PAROLI>"}'
```

Admin yaratilganidan keyin bootstrap'ni o'chiring:
```bash
sed -i 's/^BOOTSTRAP_ADMIN_ENABLED=.*/BOOTSTRAP_ADMIN_ENABLED=false/' /etc/bilim-scan/bilim-scan.env
systemctl restart bilim-scan
```

## 10. Flutter web'ni build qilib yuklash (Mac'da)

Mac'dagi terminalda, `/Users/uzmacbook/AndroidStudioProjects/bilim_scan` papkasida:

```bash
cd ~/AndroidStudioProjects/bilim_scan
flutter pub get

# 1) Kursant sayti → https://flashtest.uz
flutter build web --release \
  --dart-define=APP_TYPE=student \
  --dart-define=SERVER_URL=https://api.flashtest.uz \
  -o build/web_user

# 2) Admin / o'qituvchi sayti → https://admin.flashtest.uz
flutter build web --release \
  --dart-define=APP_TYPE=admin \
  --dart-define=SERVER_URL=https://api.flashtest.uz \
  -o build/web_admin

# 3) Serverga yuklash (--delete eski fayllarni o'chiradi)
rsync -az --delete build/web_user/  root@<SERVER_IP>:/var/www/flashtest.uz/
rsync -az --delete build/web_admin/ root@<SERVER_IP>:/var/www/admin.flashtest.uz/
```

> `-o` bilan har biri alohida papkaga build qilinadi, bir-birini ustidan yozmaydi.
> Oxiridagi `/` muhim: `build/web_user/` — papkaning **ichidagi** fayllar yuklanadi.
> Saytlar domen ildizida turgani uchun `--base-href` berish shart emas (standart `/`).

Har safar frontend o'zgarganda faqat shu 10-qadamni takrorlaysiz. Brauzerda eski versiya qolsa — `Ctrl+Shift+R`.

Qulaylik uchun Flutter loyiha ichida `deploy_web.sh` qilib saqlab qo'yish mumkin:
```bash
#!/bin/bash
set -e
SERVER=root@<SERVER_IP>
API=https://api.flashtest.uz
flutter build web --release --dart-define=APP_TYPE=student --dart-define=SERVER_URL=$API -o build/web_user
flutter build web --release --dart-define=APP_TYPE=admin   --dart-define=SERVER_URL=$API -o build/web_admin
rsync -az --delete build/web_user/  $SERVER:/var/www/flashtest.uz/
rsync -az --delete build/web_admin/ $SERVER:/var/www/admin.flashtest.uz/
echo "Tayyor: https://flashtest.uz va https://admin.flashtest.uz"
```

## 11. Bazani eksport / import qilish (ilova ichidan)

ADMIN uchun ikkita endpoint. Butun baza (barcha kolleksiyalar) va profil rasmlari **bitta `.zip`** ga tushadi. Shu faylni istalgan serverga yuklasangiz, tizim aynan o'sha holatdan davom etadi.

| Amal | So'rov |
|------|--------|
| Eksport | `GET https://api.flashtest.uz/api/admin/backup/export` → `bilim_scan_backup_YYYY-MM-DD_HHmmss.zip` |
| Import | `POST https://api.flashtest.uz/api/admin/backup/import`, `multipart/form-data`, maydon: `file` |

```bash
TOKEN=$(curl -s -X POST https://api.flashtest.uz/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<PAROL>"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -OJ -H "Authorization: Bearer $TOKEN" https://api.flashtest.uz/api/admin/backup/export
curl -H "Authorization: Bearer $TOKEN" -F file=@bilim_scan_backup_....zip https://api.flashtest.uz/api/admin/backup/import
```

- **Import hozirgi bazani to'liq almashtiradi** (rasmlar ham).
- Import oldidan hozirgi holat avtomatik `/opt/bilim-scan/backups/pre_import_*.zip` ga saqlanadi — xato bo'lsa shuni qayta import qilasiz.
- Buzilgan yoki begona fayl → `400` va sababi, bazaga tegilmaydi.
- Import'dan keyin login/parollar backup'dagidek bo'ladi. Serverlarda `JWT_SECRET` farqli bo'lsa, hamma qayta login qiladi.
- Imtihon ketayotgan paytda import qilmang.

## 12. (Ixtiyoriy) Eski Windows serverdan ko'chirish

Eski serverda yangi versiya o'rnatilmagan va ma'lumot **`test`** bazasida turibdi, shuning uchun bu safar `mongodump` bilan:

**Windows'da** (MongoDB Database Tools kerak):
```powershell
mongodump --uri="mongodb://localhost:27017/test" --archive=old.archive --gzip
scp old.archive root@<SERVER_IP>:/tmp/
scp -r C:\Users\UltraPC\IdeaProjects\bilim_scan_server\uploads\profiles root@<SERVER_IP>:/tmp/
```

**Serverda:**
```bash
systemctl stop bilim-scan
docker cp /tmp/old.archive mongo:/tmp/
docker exec mongo mongorestore -u dbadmin -p '<MONGO_PASSWORD>' --authenticationDatabase admin \
  --archive=/tmp/old.archive --gzip --nsFrom='test.*' --nsTo='bilim_scan.*' --drop
cp -r /tmp/profiles/* /opt/bilim-scan/uploads/profiles/
systemctl start bilim-scan
```

Keyingi safarlar uchun 11-bo'limdagi eksport/import'dan foydalaning.

## 13. Avtomatik kunlik zaxira

```bash
mkdir -p /var/backups/bilim-scan
cat > /usr/local/bin/bilim-scan-backup <<'EOF'
#!/bin/bash
set -e
source /etc/bilim-scan/bilim-scan.env
TS=$(date +%F_%H%M)
DIR=/var/backups/bilim-scan
docker exec mongo mongodump --uri="$SPRING_DATA_MONGODB_URI" --archive --gzip > "$DIR/db_$TS.archive.gz"
tar -czf "$DIR/uploads_$TS.tar.gz" -C /opt/bilim-scan uploads
find "$DIR" -type f -mtime +14 -delete
EOF
chmod 700 /usr/local/bin/bilim-scan-backup
echo '0 3 * * * root /usr/local/bin/bilim-scan-backup' > /etc/cron.d/bilim-scan-backup

/usr/local/bin/bilim-scan-backup && ls -lh /var/backups/bilim-scan     # sinab ko'rish
```

Har kuni 03:00 da ishlaydi, 14 kundan eskilari o'chiriladi. Vaqti-vaqti bilan boshqa joyga (kompyuterga) ko'chirib turing:
`rsync -az root@<SERVER_IP>:/var/backups/bilim-scan/ ~/flashtest-backups/`

## 14. Backend'ni yangilash

```bash
cd /opt/bilim-scan/src && git pull && ./mvnw -B clean package -DskipTests \
  && cp /opt/bilim-scan/app.jar /opt/bilim-scan/app.jar.bak \
  && cp target/bilim_scan-0.0.1-SNAPSHOT.jar /opt/bilim-scan/app.jar \
  && systemctl restart bilim-scan && journalctl -u bilim-scan -n 30 --no-pager
```

Orqaga qaytarish: `cp /opt/bilim-scan/app.jar.bak /opt/bilim-scan/app.jar && systemctl restart bilim-scan`

## 15. Foydali buyruqlar

| Vazifa | Buyruq |
|--------|--------|
| Backend holati / qayta ishga tushirish | `systemctl status bilim-scan` / `systemctl restart bilim-scan` |
| Backend loglari | `journalctl -u bilim-scan -f` |
| Nginx loglari | `tail -f /var/log/nginx/error.log /var/log/nginx/access.log` |
| Nginx sozlamasini tekshirish | `nginx -t && systemctl reload nginx` |
| Mongo konsoli | `docker exec -it mongo mongosh -u dbadmin -p --authenticationDatabase admin bilim_scan` |
| Portlar | `ss -tulpn \| grep -E ':80\|:443\|4257\|27017'` |
| SSL muddati | `certbot certificates` |

## 16. Tez-tez uchraydigan xatolar

| Belgi | Sabab / yechim |
|-------|----------------|
| Saytda "XMLHttpRequest error" / CORS xatosi | Sayt manzili `CORS_ALLOWED_ORIGINS` da yo'q (masalan `www.` bilan kirilgan). Env'ga qo'shib `systemctl restart bilim-scan`. |
| Sayt ochilmaydi, 502 Bad Gateway (api) | Backend ishlamayapti → `journalctl -u bilim-scan -n 100`. |
| Sahifani yangilaganda 404 | `try_files ... /index.html` yo'q → `snippets/flutter-web.conf` ulanganini tekshiring. |
| Saytda eski versiya ko'rinadi | `Ctrl+Shift+R`; rsync'da manzil to'g'riligini tekshiring. |
| Admin saytida kursant login oynasi chiqadi (yoki aksincha) | Build'da `APP_TYPE` adashgan yoki papkalar almashgan (10-qadam). |
| Rasmlar ko'rinmaydi | `https://api.flashtest.uz/uploads/profiles/<fayl>` ochilishini tekshiring; fayl `/opt/bilim-scan/uploads/profiles` da bo'lishi kerak. |
| `certbot` xatosi | DNS hali server IP'ga qaramayapti (`dig +short <domen>`), yoki 80-port yopiq. |
| `Unauthorized` / Mongo auth xatosi | `SPRING_DATA_MONGODB_URI` dagi parol `docker-compose.yml` dagi bilan bir xil emas. |
| `WeakKeyException` | `JWT_SECRET` 32 belgidan qisqa. |
| Import'da `413` | Nginx `client_max_body_size` yoki `MAX_UPLOAD_SIZE` kichik. |
| `MongoDB ishga tushmadi`, `docker logs mongo` da `AVX` haqida xato | VPS protsessorida AVX yo'q — MongoDB 5+ ishlamaydi. `mongo:4.4` image'ini ishlating yoki VPS provayderidan AVX'li tarif so'rang. |
| `deploy.sh` da `git pull` xatosi | Serverdagi papkada lokal o'zgarish bor. `git -C <papka> status` bilan ko'ring yoki `./deploy.sh --no-pull`. |
