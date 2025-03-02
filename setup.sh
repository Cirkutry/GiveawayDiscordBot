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

# Set default port
DB_PORT=3306

# Check if port 3306 is already in use
if command -v netstat > /dev/null 2>&1; then
  if netstat -tuln | grep -q ":3306 "; then
    echo -e "${YELLOW}Port 3306 is already in use. This might be another MySQL/MariaDB instance.${NC}"
    echo -e "Would you like to:"
    echo -e "1) Use a different port for the database container"
    echo -e "2) Attempt to stop the existing service on port 3306"
    echo -e "3) Exit and resolve manually"
    read -p "Enter your choice (1/2/3): " PORT_CHOICE
    
    case $PORT_CHOICE in
      1)
        read -p "Enter an alternative port to use (e.g., 3307): " DB_PORT
        ;;
      2)
        echo -e "${YELLOW}Attempting to stop existing MySQL/MariaDB service...${NC}"
        systemctl stop mysql mariadb 2>/dev/null || service mysql stop 2>/dev/null || service mariadb stop 2>/dev/null
        if netstat -tuln | grep -q ":3306 "; then
          echo -e "${RED}Failed to stop the service on port 3306. Please resolve manually.${NC}"
          exit 1
        else
          echo -e "${GREEN}Successfully stopped the service on port 3306.${NC}"
          DB_PORT=3306
        fi
        ;;
      3)
        echo -e "${YELLOW}Exiting. Please resolve the port conflict manually.${NC}"
        exit 0
        ;;
      *)
        echo -e "${RED}Invalid option. Exiting.${NC}"
        exit 1
        ;;
    esac
  fi
elif command -v ss > /dev/null 2>&1; then
  if ss -tuln | grep -q ":3306 "; then
    echo -e "${YELLOW}Port 3306 is already in use. This might be another MySQL/MariaDB instance.${NC}"
    echo -e "Would you like to:"
    echo -e "1) Use a different port for the database container"
    echo -e "2) Attempt to stop the existing service on port 3306"
    echo -e "3) Exit and resolve manually"
    read -p "Enter your choice (1/2/3): " PORT_CHOICE
    
    case $PORT_CHOICE in
      1)
        read -p "Enter an alternative port to use (e.g., 3307): " DB_PORT
        ;;
      2)
        echo -e "${YELLOW}Attempting to stop existing MySQL/MariaDB service...${NC}"
        systemctl stop mysql mariadb 2>/dev/null || service mysql stop 2>/dev/null || service mariadb stop 2>/dev/null
        if ss -tuln | grep -q ":3306 "; then
          echo -e "${RED}Failed to stop the service on port 3306. Please resolve manually.${NC}"
          exit 1
        else
          echo -e "${GREEN}Successfully stopped the service on port 3306.${NC}"
          DB_PORT=3306
        fi
        ;;
      3)
        echo -e "${YELLOW}Exiting. Please resolve the port conflict manually.${NC}"
        exit 0
        ;;
      *)
        echo -e "${RED}Invalid option. Exiting.${NC}"
        exit 1
        ;;
    esac
  fi
fi

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

# Create docker-compose.yml - Hardcoding values instead of using variable substitution to avoid issues
cat > docker-compose.yml << EOL
services:
  db:
    image: mariadb:10.6
    container_name: giveaway_db
    restart: always
    environment:
      MARIADB_ROOT_PASSWORD: "${DB_ROOT_PASSWORD}"
      MARIADB_DATABASE: giveaway_db
      MARIADB_USER: giveaway_user
      MARIADB_PASSWORD: "${DB_PASSWORD}"
    volumes:
      - giveaway_db_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
    ports:
      - "${DB_PORT}:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
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
    command: /wait-for-db.sh db root "${DB_ROOT_PASSWORD}" giveaway_db
    restart: "no"

  giveaway:
    container_name: giveaway_bot
    image: cirkutry/nobrand-giveaway:latest
    restart: always
    volumes:
      - ./giveaway_logs:/app/logs
    environment:
      DATABASE_URL: "jdbc:mariadb://db:3306/giveaway_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&rewriteBatchedStatements=true"
      DATABASE_USER: "giveaway_user"
      DATABASE_PASS: "${DB_PASSWORD}"
      TOKEN: "${BOT_TOKEN}"
    depends_on:
      db-init:
        condition: service_completed_successfully

volumes:
  giveaway_db_data:
EOL

# Create init.sql safely with here-document to avoid bash parsing the SQL content
cat > init.sql << 'EOSQL'
-- Create tables if they don't exist
CREATE TABLE IF NOT EXISTS `active_giveaways` (
    `count_winners`        int(11) DEFAULT NULL,
    `finish`               bit(1) NOT NULL,
    `is_for_specific_role` bit(1)       DEFAULT NULL,
    `min_participants`     int(11) DEFAULT NULL,
    `channel_id`           bigint(20) NOT NULL,
    `created_user_id`      bigint(20) NOT NULL,
    `date_end`             datetime(6) DEFAULT NULL,
    `guild_id`             bigint(20) NOT NULL,
    `message_id`           bigint(20) NOT NULL,
    `role_id`              bigint(20) DEFAULT NULL,
    `title`                varchar(255) DEFAULT NULL,
    `url_image`            varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `list_users` (
    `created_user_id` bigint(20) NOT NULL,
    `giveaway_id`     bigint(20) NOT NULL,
    `guild_id`        bigint(20) NOT NULL,
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id`         bigint(20) NOT NULL,
    `nick_name`       varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `notification` (
    `user_id_long`        varchar(255) NOT NULL,
    `notification_status` enum('ACCEPT','DENY') NOT NULL,
    PRIMARY KEY (`user_id_long`),
    UNIQUE KEY `user_id_long` (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `participants` (
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `message_id` bigint(20) NOT NULL,
    `user_id`    bigint(20) NOT NULL,
    `nick_name`  varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK5wwgegod4ejelbpml5lgnic9b` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci AUTO_INCREMENT=2004;

CREATE TABLE IF NOT EXISTS `scheduling` (
    `count_winners`        int(11) DEFAULT NULL,
    `is_for_specific_role` bit(1)       DEFAULT NULL,
    `min_participants`     int(11) DEFAULT NULL,
    `channel_id`           bigint(20) NOT NULL,
    `create_giveaway`      datetime(6) NOT NULL,
    `created_user_id`      bigint(20) NOT NULL,
    `date_end`             datetime(6) DEFAULT NULL,
    `guild_id`             bigint(20) NOT NULL,
    `role_id`              bigint(20) DEFAULT NULL,
    `id_salt`              varchar(255) NOT NULL,
    `title`                varchar(255) DEFAULT NULL,
    `url_image`            varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id_salt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `settings` (
    `server_id` bigint(20) NOT NULL,
    `color_hex` varchar(255) DEFAULT NULL,
    `language`  varchar(255) NOT NULL,
    `text`      varchar(255) DEFAULT NULL,
    PRIMARY KEY (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Add the primary key to active_giveaways if it doesn't exist
ALTER TABLE `active_giveaways` ADD PRIMARY KEY IF NOT EXISTS (`message_id`);

-- Check if foreign key exists before adding it (safer SQL approach)
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
EOSQL

echo -e "\n${GREEN}Configuration files created successfully.${NC}"
echo -e "Starting the containers..."

# Start the containers
docker compose up -d

echo -e "\n${YELLOW}Waiting for services to initialize...${NC}"
sleep 15

# Check if services are running properly
if docker ps | grep -q "giveaway_bot" && docker ps | grep -q "giveaway_db"; then
  echo -e "\n${GREEN}Services are running!${NC}"
  
  # Wait a bit more for the bot to fully initialize
  echo -e "Waiting for the bot to fully initialize..."
  sleep 15
  
  # Check bot logs for any errors
  BOT_LOGS=$(docker logs giveaway_bot 2>&1 | grep -i "error\|exception" | tail -10)
  if [ -n "$BOT_LOGS" ]; then
    echo -e "\n${YELLOW}Warning: Found potential errors in bot logs:${NC}"
    echo -e "$BOT_LOGS"
  else
    echo -e "\n${GREEN}No obvious errors found in bot logs.${NC}"
  fi
else
  echo -e "\n${RED}Services are not running correctly. Checking logs...${NC}"
  echo -e "\n${YELLOW}Database container logs:${NC}"
  docker logs giveaway_db 2>&1 | tail -20
  echo -e "\n${YELLOW}DB Init container logs:${NC}"
  docker logs $(docker ps -aqf "name=db-init") 2>&1 | tail -20
  echo -e "\n${YELLOW}Bot container logs:${NC}"
  docker logs giveaway_bot 2>&1 | tail -20
fi

echo -e "\n${GREEN}Setup complete!${NC}"
echo -e "Your Giveaway Discord Bot should now be running."
echo -e "You can check the status with ${BLUE}docker compose ps${NC}"
echo -e "View logs with ${BLUE}docker compose logs -f giveaway${NC}"
echo -e "If you experience issues, try: ${BLUE}docker compose down -v${NC} and then run this script again"
