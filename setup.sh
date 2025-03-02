#!/bin/bash

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Giveaway Discord Bot Setup ===${NC}"
echo "This script will set up the Giveaway Discord Bot with MariaDB."

# Prompt for configuration
echo -e "\n${GREEN}Please provide the following information:${NC}"
read -p "Discord Bot Token: " BOT_TOKEN
read -p "Database Password: " DB_PASSWORD
read -p "Database Root Password: " DB_ROOT_PASSWORD

# Create docker-compose.yml
cat > docker-compose.yml << EOL
version: '3'

services:
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
      - db

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

volumes:
  giveaway_db_data:
EOL

# Create init.sql from the schema
cat > init.sql << EOL
CREATE TABLE \`active_giveaways\`
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

CREATE TABLE \`list_users\`
(
    \`created_user_id\` bigint(20) NOT NULL,
    \`giveaway_id\`     bigint(20) NOT NULL,
    \`guild_id\`        bigint(20) NOT NULL,
    \`id\`              bigint(20) NOT NULL,
    \`user_id\`         bigint(20) NOT NULL,
    \`nick_name\`       varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE \`notification\`
(
    \`user_id_long\`        varchar(255) NOT NULL,
    \`notification_status\` enum('ACCEPT','DENY') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE \`participants\`
(
    \`id\`         bigint(20) NOT NULL,
    \`message_id\` bigint(20) NOT NULL,
    \`user_id\`    bigint(20) NOT NULL,
    \`nick_name\`  varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE \`scheduling\`
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
    \`url_image\`            varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE SEQUENCE sequence_id_auto_gen START WITH 1 INCREMENT BY 100;

CREATE TABLE \`settings\`
(
    \`server_id\` bigint(20) NOT NULL,
    \`color_hex\` varchar(255) DEFAULT NULL,
    \`language\`  varchar(255) NOT NULL,
    \`text\`      varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE \`active_giveaways\`
    ADD PRIMARY KEY (\`message_id\`);

ALTER TABLE \`list_users\`
    ADD PRIMARY KEY (\`id\`);

ALTER TABLE \`notification\`
    ADD PRIMARY KEY (\`user_id_long\`),
  ADD UNIQUE KEY \`user_id_long\` (\`user_id_long\`);

ALTER TABLE \`participants\`
    ADD PRIMARY KEY (\`id\`),
  ADD KEY \`FK5wwgegod4ejelbpml5lgnic9b\` (\`message_id\`);

ALTER TABLE \`scheduling\`
    ADD PRIMARY KEY (\`id_salt\`);

ALTER TABLE \`settings\`
    ADD PRIMARY KEY (\`server_id\`);

ALTER TABLE \`list_users\`
    MODIFY \`id\` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE \`participants\`
    MODIFY \`id\` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2004;

ALTER TABLE \`participants\`
    ADD CONSTRAINT \`FK5wwgegod4ejelbpml5lgnic9b\` FOREIGN KEY (\`message_id\`) REFERENCES \`active_giveaways\` (\`message_id\`) ON DELETE CASCADE;
COMMIT;
EOL

echo -e "\n${GREEN}Configuration files created successfully.${NC}"
echo -e "Starting the containers..."

# Start the containers
docker compose up -d

echo -e "\n${GREEN}Setup complete!${NC}"
echo -e "Your Giveaway Discord Bot should now be running."
echo -e "You can check the status with ${BLUE}docker compose ps${NC}"
echo -e "View logs with ${BLUE}docker compose logs -f giveaway${NC}"
