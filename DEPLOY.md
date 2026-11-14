# Bilim Scan Server — Ubuntu 26.04 serverga deploy qilish

Bu yo'riqnoma **yangi, bo'sh Ubuntu 26.04** server uchun. Natijada:

- Java 21 + Spring Boot ilova `systemd` servisi sifatida ishlaydi (avtomatik qayta ishga tushadi);
- MongoDB Docker konteynerida, faqat `127.0.0.1` da, parol bilan ishlaydi;
- maxfiy sozlamalar (`JWT_SECRET`, Mongo paroli, admin paroli) `/etc/bilim-scan/bilim-scan.env` faylida turadi;
- (ixtiyoriy) Nginx + HTTPS orqali domen bilan chiqariladi.

> Buyruqlardagi `<...>` qiymatlarni o'zingiznikiga almashtiring.

---

## 0. Deploydan oldin loyihada bilish kerak bo'lgan narsalar

| # | Holat | Izoh |
|---|-------|------|
| 1 | ✅ **Tuzatildi** | `application.properties` da `spring.mongodb.uri` yozilgan edi. Spring Boot 3.3 da bu kalit **o'qilmaydi** (to'g'risi `spring.data.mongodb.uri`). Shu sababli Windows serverda ilova Mongo'ga parolsiz, `test` bazasiga ulangan edi (`bilim-scan-service.out.log` da `credential=null` ko'rinadi). Endi kalit to'g'rilandi. |
| 2 | ⚠️ | Eski Windows serverdagi ma'lumotlar **`test`** nomli bazada turibdi. Ko'chirishda shuni hisobga oling (11-qadam). |
| 3 | ⚠️ | `application.properties` ichida standart (default) parollar bor: Mongo paroli, `JWT_SECRET`, `admin123`. Ular git tarixida qolgan — **serverda albatta yangi qiymatlarni env fayl orqali bering** (5-qadam). |
| 4 | ⚠️ | `uploads/` papkasi va `file.upload-dir` **nisbiy yo'l** (relative path). Ilova albatta `/opt/bilim-scan` papkasidan ishga tushishi kerak (`WorkingDirectory` systemd'da berilgan). |
| 5 | ℹ️ | `NetworkDiscoveryService` UDP **8888** portda LAN broadcast orqali serverni topadi. Bu faqat server mijozlar bilan **bitta lokal tarmoqda** bo'lsa ishlaydi. VPS/bulutda bu ishlamaydi — mobil ilovaga server manzilini qo'lda kiritish kerak bo'ladi. |
| 6 | ℹ️ | `BilimScanApplicationTests` Mongo'siz o'tmaydi, shuning uchun build `-DskipTests` bilan qilinadi. |
| 7 | ℹ️ | Repoda Windows qoldiqlari bor: `bilim-scan-service.exe` (18 MB), `*.log`, `bilim-scan-service.xml`, `uploads/profiles/*.jpg`. Linuxda kerak emas — repodan olib tashlab, `.gitignore` ga qo'shish tavsiya qilinadi. |

Shu tuzatish commit qilinib, git'ga push qilinganidan keyin deployni boshlang.

---

## 1. Serverga kirish va tizimni yangilash

```bash
ssh root@<SERVER_IP>

apt update && apt upgrade -y
timedatectl set-timezone Asia/Tashkent
apt install -y curl git unzip ufw ca-certificates
```

## 2. Alohida foydalanuvchi yaratish (root bilan ishlamaslik uchun)

```bash
# admin uchun sudo foydalanuvchi (ixtiyoriy, lekin tavsiya)
adduser deploy
usermod -aG sudo deploy

# ilovani ishga tushiradigan tizim foydalanuvchisi (login qila olmaydi)
useradd --system --create-home --home-dir /opt/bilim-scan --shell /usr/sbin/nologin bilimscan
```

Keyingi qadamlarni `deploy` foydalanuvchisi bilan `sudo` orqali bajarish mumkin.

## 3. Firewall (UFW)

```bash
sudo ufw allow OpenSSH
```

Keyin tanlovingizga qarab:

**A variant — server lokal tarmoqda (maktab/ofis LAN), Nginx'siz:**
```bash
sudo ufw allow 4257/tcp     # API
sudo ufw allow 8888/udp     # mobil ilova serverni avtomatik topishi uchun
```

**B variant — VPS / domen bilan, Nginx orqali:**
```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
# 4257 tashqariga OCHILMAYDI, Nginx ichkaridan murojaat qiladi
```

```bash
sudo ufw enable
sudo ufw status
```

> MongoDB porti (27017) hech qachon tashqariga ochilmaydi.

## 4. Java 21 o'rnatish

```bash
sudo apt install -y openjdk-21-jdk-headless
java -version   # 21.x chiqishi kerak
```

> Agar serverda bir nechta Java bo'lsa: `sudo update-alternatives --config java` bilan 21 ni tanlang.

## 5. MongoDB (Docker orqali)

MongoDB'ning rasmiy apt repozitoriyasi Ubuntu 26.04 ni hali qo'llab-quvvatlamasligi mumkin, shuning uchun Docker eng ishonchli yo'l.

```bash
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
```

Kuchli parol yarating va eslab qoling (keyingi qadamda kerak):

```bash
openssl rand -hex 24   # -> MONGO_PASSWORD
```

```bash
sudo mkdir -p /opt/mongo
sudo tee /opt/mongo/docker-compose.yml > /dev/null <<'EOF'
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

sudo chmod 600 /opt/mongo/docker-compose.yml
cd /opt/mongo && sudo docker compose up -d
sudo docker ps    # mongo "Up" bo'lishi kerak
```

Tekshirish:
```bash
sudo docker exec -it mongo mongosh -u dbadmin -p '<MONGO_PASSWORD>' --authenticationDatabase admin --eval 'db.runCommand({ping:1})'
```

## 6. Maxfiy sozlamalar (env fayl)

```bash
openssl rand -base64 48   # -> JWT_SECRET (kamida 32 belgi bo'lishi shart)
```

```bash
sudo mkdir -p /etc/bilim-scan
sudo tee /etc/bilim-scan/bilim-scan.env > /dev/null <<'EOF'
SERVER_PORT=4257
SPRING_DATA_MONGODB_URI=mongodb://dbadmin:<MONGO_PASSWORD>@127.0.0.1:27017/bilim_scan?authSource=admin
JWT_SECRET=<JWT_SECRET>
JWT_EXPIRATION_MS=10800000
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=<KUCHLI_ADMIN_PAROLI>
EOF

sudo chown root:bilimscan /etc/bilim-scan/bilim-scan.env
sudo chmod 640 /etc/bilim-scan/bilim-scan.env
```

> Mongo parolida `@ : / ? #` kabi belgilar bo'lsa, URI'da URL-encode qilish kerak. `openssl rand -hex` faqat harf-raqam beradi — muammo bo'lmaydi.

## 7. Kodni olish va build qilish

```bash
sudo mkdir -p /opt/bilim-scan/src
sudo chown -R deploy:deploy /opt/bilim-scan/src
git clone <GIT_REPO_URL> /opt/bilim-scan/src
cd /opt/bilim-scan/src

chmod +x mvnw
./mvnw -B clean package -DskipTests
ls -lh target/*.jar   # bilim_scan-0.0.1-SNAPSHOT.jar
```

JAR'ni ishlash joyiga ko'chirish va papkalarni tayyorlash:

```bash
sudo cp target/bilim_scan-0.0.1-SNAPSHOT.jar /opt/bilim-scan/app.jar
sudo mkdir -p /opt/bilim-scan/uploads/profiles
sudo chown -R bilimscan:bilimscan /opt/bilim-scan/app.jar /opt/bilim-scan/uploads
```

> Private repo bo'lsa: serverda `ssh-keygen -t ed25519`, public kalitni GitHub → Deploy keys'ga qo'shing va `git@github.com:...` manzil bilan clone qiling.

## 8. systemd servisi

```bash
sudo tee /etc/systemd/system/bilim-scan.service > /dev/null <<'EOF'
[Unit]
Description=Bilim Scan Spring Boot server
After=network-online.target docker.service
Wants=network-online.target
Requires=docker.service

[Service]
User=bilimscan
Group=bilimscan
WorkingDirectory=/opt/bilim-scan
EnvironmentFile=/etc/bilim-scan/bilim-scan.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/bilim-scan/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now bilim-scan
sudo systemctl status bilim-scan
```

Loglarni ko'rish:
```bash
sudo journalctl -u bilim-scan -f
```

Kutilgan qatorlar:
- `Tomcat started on port 4257`
- `Bootstrap orqali ADMIN foydalanuvchi yaratildi: admin`
- `UDP Kashf qilish (Discovery) xizmati 8888-portda ishga tushdi...`

> `-Xmx512m` — server RAM'iga qarab o'zgartiring (1 GB RAM bo'lsa 384m, 4 GB bo'lsa 1g).

## 9. Ishlashini tekshirish

```bash
curl -i -X POST http://127.0.0.1:4257/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<KUCHLI_ADMIN_PAROLI>"}'
```

Javobda token qaytsa — hammasi joyida.

**Admin yaratilgandan keyin bootstrap'ni o'chiring:**
```bash
sudo sed -i 's/^BOOTSTRAP_ADMIN_ENABLED=.*/BOOTSTRAP_ADMIN_ENABLED=false/' /etc/bilim-scan/bilim-scan.env
sudo systemctl restart bilim-scan
```

## 10. (B variant) Nginx + HTTPS

Domen A-yozuvi server IP'siga yo'naltirilgan bo'lishi kerak.

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

sudo tee /etc/nginx/sites-available/bilim-scan > /dev/null <<'EOF'
server {
    listen 80;
    server_name <DOMEN>;

    client_max_body_size 500M;   # backup import uchun (profil rasmi baribir 5MB bilan cheklangan)
    proxy_request_buffering off;

    location / {
        proxy_pass http://127.0.0.1:4257;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;   # katta backup eksport/import uchun
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/bilim-scan /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

sudo certbot --nginx -d <DOMEN> --redirect -m <EMAIL> --agree-tos -n
```

Tekshirish: `curl -i https://<DOMEN>/api/auth/login -X POST -H 'Content-Type: application/json' -d '{}'`

Nginx bilan ilovani faqat lokalga bog'lash uchun env faylga qo'shing (ixtiyoriy, qo'shimcha himoya):
```bash
echo 'SERVER_ADDRESS=127.0.0.1' | sudo tee -a /etc/bilim-scan/bilim-scan.env
sudo systemctl restart bilim-scan
```

## 11. (Ixtiyoriy) Eski Windows serverdan ma'lumotlarni ko'chirish

> Kelajakda serverlar orasida ko'chirish uchun 12-bo'limdagi eksport/import funksiyasidan foydalaning. Quyidagi usul faqat **hozirgi** Windows server uchun, chunki unda yangi versiya hali o'rnatilmagan va ma'lumotlar `test` bazasida turibdi.

Eski serverda ma'lumot **`test`** bazasida (0-bo'limning 1-bandiga qarang).

**Windows'da** (MongoDB Database Tools o'rnatilgan bo'lishi kerak):
```powershell
mongodump --uri="mongodb://localhost:27017/test" --archive=bilim_scan.archive --gzip
```

Arxivni va rasmlarni serverga yuborish:
```powershell
scp bilim_scan.archive deploy@<SERVER_IP>:/tmp/
scp -r C:\Users\UltraPC\IdeaProjects\bilim_scan_server\uploads\profiles deploy@<SERVER_IP>:/tmp/
```

**Serverda:**
```bash
sudo systemctl stop bilim-scan

sudo docker cp /tmp/bilim_scan.archive mongo:/tmp/
sudo docker exec mongo mongorestore \
  -u dbadmin -p '<MONGO_PASSWORD>' --authenticationDatabase admin \
  --archive=/tmp/bilim_scan.archive --gzip \
  --nsFrom='test.*' --nsTo='bilim_scan.*' --drop

sudo cp -r /tmp/profiles/* /opt/bilim-scan/uploads/profiles/
sudo chown -R bilimscan:bilimscan /opt/bilim-scan/uploads

sudo systemctl start bilim-scan
```

> Ko'chirilgan bazada ADMIN allaqachon bor bo'ladi — bootstrap uni qayta yaratmaydi, eski admin paroli ishlaydi.

## 12. Bazani eksport / import qilish (ilova ichidan)

Ilovada ADMIN uchun ikkita endpoint bor. Ular butun bazani (barcha kolleksiyalar) va profil rasmlarini **bitta `.zip` faylga** joylaydi. Shu faylni boshqa istalgan serverga yuklasangiz, tizim aynan o'sha holatdan davom etadi.

| Amal | So'rov |
|------|--------|
| Eksport (yuklab olish) | `GET /api/admin/backup/export` → `bilim_scan_backup_YYYY-MM-DD_HHmmss.zip` |
| Import (tiklash) | `POST /api/admin/backup/import`, `multipart/form-data`, maydon nomi: `file` |

```bash
# token olish
TOKEN=$(curl -s -X POST https://<DOMEN>/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<PAROL>"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# eksport
curl -OJ -H "Authorization: Bearer $TOKEN" https://<DOMEN>/api/admin/backup/export

# boshqa serverga import
curl -H "Authorization: Bearer $TOKEN" -F file=@bilim_scan_backup_2026-11-14_174356.zip \
  https://<YANGI_SERVER>/api/admin/backup/import
```

Bilish kerak bo'lgan narsalar:
- **Import hozirgi bazani to'liq almashtiradi** (backup'da yo'q kolleksiyalar ham tozalanadi, rasmlar ham almashtiriladi).
- Import boshlanishidan oldin hozirgi holat avtomatik `/opt/bilim-scan/backups/pre_import_*.zip` ga saqlanadi. Xato fayl yuklansangiz, o'sha faylni qayta import qilib orqaga qaytasiz.
- Fayl avval to'liq tekshiriladi. Buzilgan yoki begona fayl bo'lsa `400` qaytadi va bazaga tegilmaydi.
- Import'dan keyin foydalanuvchilar backup'dagi login/parollari bilan kiradi. Serverlar orasida `JWT_SECRET` farq qilsa, hamma qayta login qiladi.
- Import paytida boshqa foydalanuvchilar ishlamayotgan bo'lgani ma'qul (masalan, imtihon vaqtida qilmang).
- Maksimal fayl hajmi standart holatda 500MB. O'zgartirish uchun env faylga `MAX_UPLOAD_SIZE=1GB` qo'shing (Nginx'dagi `client_max_body_size` ni ham).

## 13. Avtomatik zaxira nusxa (cron)

```bash
sudo mkdir -p /var/backups/bilim-scan
sudo tee /usr/local/bin/bilim-scan-backup > /dev/null <<'EOF'
#!/bin/bash
set -e
source /etc/bilim-scan/bilim-scan.env
TS=$(date +%F_%H%M)
DIR=/var/backups/bilim-scan
docker exec mongo mongodump --uri="$SPRING_DATA_MONGODB_URI" --archive --gzip > "$DIR/db_$TS.archive.gz"
tar -czf "$DIR/uploads_$TS.tar.gz" -C /opt/bilim-scan uploads
find "$DIR" -type f -mtime +14 -delete
EOF
sudo chmod 700 /usr/local/bin/bilim-scan-backup

# har kuni soat 03:00 da
echo '0 3 * * * root /usr/local/bin/bilim-scan-backup' | sudo tee /etc/cron.d/bilim-scan-backup
```

> `SPRING_DATA_MONGODB_URI` dagi `127.0.0.1` konteyner ichida ham Mongo'ning o'ziga ishora qiladi, shuning uchun skript to'g'ri ishlaydi. Backuplarni vaqti-vaqti bilan boshqa joyga ham ko'chirib turing.

## 14. Yangi versiyani chiqarish (update)

```bash
cd /opt/bilim-scan/src
git pull
./mvnw -B clean package -DskipTests

sudo cp /opt/bilim-scan/app.jar /opt/bilim-scan/app.jar.bak
sudo cp target/bilim_scan-0.0.1-SNAPSHOT.jar /opt/bilim-scan/app.jar
sudo chown bilimscan:bilimscan /opt/bilim-scan/app.jar
sudo systemctl restart bilim-scan
sudo journalctl -u bilim-scan -n 50 --no-pager
```

Muammo chiqsa, orqaga qaytarish:
```bash
sudo cp /opt/bilim-scan/app.jar.bak /opt/bilim-scan/app.jar && sudo systemctl restart bilim-scan
```

## 15. Foydali buyruqlar

| Vazifa | Buyruq |
|--------|--------|
| Servis holati | `sudo systemctl status bilim-scan` |
| Qayta ishga tushirish | `sudo systemctl restart bilim-scan` |
| Loglar (jonli) | `sudo journalctl -u bilim-scan -f` |
| Mongo konsoli | `sudo docker exec -it mongo mongosh -u dbadmin -p --authenticationDatabase admin` |
| Ilova ichidan backup | `curl -OJ -H "Authorization: Bearer $TOKEN" http://127.0.0.1:4257/api/admin/backup/export` |
| Mongo loglari | `sudo docker logs -f mongo` |
| Port band qilinganmi | `sudo ss -tulpn \| grep -E '4257\|8888\|27017'` |

## 16. Tez-tez uchraydigan xatolar

| Belgi | Sabab / yechim |
|-------|----------------|
| `Command ... requires authentication` / `Unauthorized` | `SPRING_DATA_MONGODB_URI` noto'g'ri yoki parol mos emas. Env faylni tekshiring. |
| `WeakKeyException` | `JWT_SECRET` 32 baytdan qisqa. Uzunroq qiling. |
| `username band - admin` bilan ilova to'xtaydi | Bazada `admin` bor, lekin roli ADMIN emas. `BOOTSTRAP_ADMIN_USERNAME` ni o'zgartiring yoki bootstrap'ni o'chiring. |
| Import'da `413 Request Entity Too Large` | Nginx `client_max_body_size` yoki `MAX_UPLOAD_SIZE` kichik. |
| Rasm yuklanmaydi / 500 | `/opt/bilim-scan/uploads` egasi `bilimscan` emas → `chown -R bilimscan:bilimscan /opt/bilim-scan/uploads`. Nginx'da 413 bo'lsa → `client_max_body_size`. |
| Mobil ilova serverni avtomatik topmaydi | Server boshqa tarmoqda (VPS) yoki `8888/udp` yopiq. Manzilni qo'lda kiriting yoki portni oching. |
| `Port 4257 was already in use` | `sudo ss -tulpn \| grep 4257` bilan jarayonni toping. |
