#!/bin/bash

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Giveaway Discord Bot Setup ===${NC}"
echo "This script will set up the Giveaway Discord Bot with MariaDB."

# Prompt for configuration
echo -e "\n${GREEN}Please provide the following information:${NC}"
read -p "Discord Bot Token: " BOT_TOKEN
read -p "Database Password: " DB_PASSWORD
read -p "Database Root Password: " DB_ROOT_PASSWORD

# Clean up any existing setup
echo -e "\n${YELLOW}Cleaning up any existing containers...${NC}"
docker compose down -v 2>/dev/null
rm -f docker-compose.yml init.sql wait-for-db.sh

# Create wait-for-db script
cat > wait-for-db.sh << 'EOL'
#!/bin/bash
set -e

host="$1"
user="$2"
password="$3"
database="$4"

until mysql -h "$host" -u "$user" -p"$password" -e "SELECT 1;" >/dev/null 2>&1; do
  echo "MySQL is unavailable - sleeping"
  sleep 2
done

echo "MySQL is up - checking database"
if mysql -h "$host" -u "$user" -p"$password" -e "USE $database;" >/dev/null 2>&1; then
  echo "Database $database exists and is accessible"
else
  echo "Database $database is not accessible - creating"
  mysql -h "$host" -u "$user" -p"$password" -e "CREATE DATABASE IF NOT EXISTS $database CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
  echo "Database created successfully"
fi

echo "Ensuring user has proper permissions"
mysql -h "$host" -u "$user" -p"$password" << EOF
CREATE USER IF NOT EXISTS 'giveaway_user'@'%' IDENTIFIED BY '$password';
GRANT ALL PRIVILEGES ON $database.* TO 'giveaway_user'@'%';
FLUSH PRIVILEGES;
EOF

echo "Database initialization complete"
EOL

chmod +x wait-for-db.sh

# Create docker-compose.yml
cat > docker-compose.yml << EOL
version: '3.8'

services:
  db:
    image: mariadb:10.6
    container_name: giveaway_db
    restart: always
    environment:
      MARIADB_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MARIADB_DATABASE: giveaway_db
      MARIADB_USER: giveaway_user
      MARIADB_PASSWORD: ${DB_PASSWORD}
    volumes:
      - giveaway_db_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p\${MARIADB_ROOT_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 30s

  db-init:
    image: mariadb:10.6
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./wait-for-db.sh:/wait-for-db.sh
    command: /wait-for-db.sh db root ${DB_ROOT_PASSWORD} giveaway_db
    restart: "no"

  giveaway:
    container_name: giveaway_bot
    image: cirkutry/nobrand-giveaway:latest
    restart: always
    volumes:
      - ./giveaway_logs:/app/logs
    environment:
      DATABASE_URL: jdbc:mariadb://db:3306/giveaway_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&rewriteBatchedStatements=true
      DATABASE_USER: giveaway_user
      DATABASE_PASS: ${DB_PASSWORD}
      TOKEN: ${BOT_TOKEN}
    depends_on:
      db-init:
        condition: service_completed_successfully

volumes:
  giveaway_db_data:
EOL

# Create init.sql from the schema
cat > init.sql << EOL
CREATE TABLE IF NOT EXISTS \`active_giveaways\`
(
    \`count_winners\`        int(11) DEFAULT NULL,
    \`finish\`               bit(1) NOT NULL,
    \`is_for_specific_role\` bit(1)       DEFAULT NULL,
    \`min_participants\`     int(11) DEFAULT NULL,
    \`channel_id\`           bigint(20) NOT NULL,
    \`created_user_id\`      bigint(20) NOT NULL,
    \`date_end\`             datetime(6) DEFAULT NULL,
    \`guild_id\`             bigint(20) NOT NULL,
    \`message_id\`           bigint(20) NOT NULL,
    \`role_id\`              bigint(20) DEFAULT NULL,
    \`title\`                varchar(255) DEFAULT NULL,
    \`url_image\`            varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS \`list_users\`
(
    \`created_user_id\` bigint(20) NOT NULL,
    \`giveaway_id\`     bigint(20) NOT NULL,
    \`guild_id\`        bigint(20) NOT NULL,
    \`id\`              bigint(20) NOT NULL AUTO_INCREMENT,
    \`user_id\`         bigint(20) NOT NULL,
    \`nick_name\`       varchar(255) NOT NULL,
    PRIMARY KEY (\`id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS \`notification\`
(
    \`user_id_long\`        varchar(255) NOT NULL,
    \`notification_status\` enum('ACCEPT','DENY') NOT NULL,
    PRIMARY KEY (\`user_id_long\`),
    UNIQUE KEY \`user_id_long\` (\`user_id_long\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS \`participants\`
(
    \`id\`         bigint(20) NOT NULL AUTO_INCREMENT,
    \`message_id\` bigint(20) NOT NULL,
    \`user_id\`    bigint(20) NOT NULL,
    \`nick_name\`  varchar(255) NOT NULL,
    PRIMARY KEY (\`id\`),
    KEY \`FK5wwgegod4ejelbpml5lgnic9b\` (\`message_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=2004;

CREATE TABLE IF NOT EXISTS \`scheduling\`
(
    \`count_winners\`        int(11) DEFAULT NULL,
    \`is_for_specific_role\` bit(1)       DEFAULT NULL,
    \`min_participants\`     int(11) DEFAULT NULL,
    \`channel_id\`           bigint(20) NOT NULL,
    \`create_giveaway\`      datetime(6) NOT NULL,
    \`created_user_id\`      bigint(20) NOT NULL,
    \`date_end\`             datetime(6) DEFAULT NULL,
    \`guild_id\`             bigint(20) NOT NULL,
    \`role_id\`              bigint(20) DEFAULT NULL,
    \`id_salt\`              varchar(255) NOT NULL,
    \`title\`                varchar(255) DEFAULT NULL,
    \`url_image\`            varchar(255) DEFAULT NULL,
    PRIMARY KEY (\`id_salt\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE SEQUENCE IF NOT EXISTS sequence_id_auto_gen START WITH 1 INCREMENT BY 100;

CREATE TABLE IF NOT EXISTS \`settings\`
(
    \`server_id\` bigint(20) NOT NULL,
    \`color_hex\` varchar(255) DEFAULT NULL,
    \`language\`  varchar(255) NOT NULL,
    \`text\`      varchar(255) DEFAULT NULL,
    PRIMARY KEY (\`server_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Only add the foreign key if the tables exist
ALTER TABLE \`active_giveaways\`
    ADD PRIMARY KEY IF NOT EXISTS (\`message_id\`);

-- Add the foreign key if it doesn't exist
SET @exist := (
    SELECT COUNT(1) constraint_exists
    FROM information_schema.table_constraints 
    WHERE table_name = 'participants' 
    AND constraint_name = 'FK5wwgegod4ejelbpml5lgnic9b'
);

SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE `participants` ADD CONSTRAINT `FK5wwgegod4ejelbpml5lgnic9b` FOREIGN KEY (`message_id`) REFERENCES `active_giveaways` (`message_id`) ON DELETE CASCADE', 
    'SELECT "Foreign key already exists"');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;
EOL

echo -e "\n${GREEN}Configuration files created successfully.${NC}"
echo -e "Starting the containers..."

# Start the containers
docker compose up -d

echo -e "\n${YELLOW}Waiting for services to initialize...${NC}"
sleep 10

# Check if services are running properly
if docker ps | grep -q "giveaway_bot" && docker ps | grep -q "giveaway_db"; then
  echo -e "\n${GREEN}Services are running!${NC}"
else
  echo -e "\n${RED}Services are not running correctly. Checking logs...${NC}"
  docker compose logs
fi

echo -e "\n${GREEN}Setup complete!${NC}"
echo -e "Your Giveaway Discord Bot should now be running."
echo -e "You can check the status with ${BLUE}docker compose ps${NC}"
echo -e "View logs with ${BLUE}docker compose logs -f giveaway${NC}"
echo -e "If you experience issues, try: ${BLUE}docker compose down -v${NC} and then run this script again"
