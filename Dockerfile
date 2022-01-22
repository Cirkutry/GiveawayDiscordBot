FROM maven:3.6.3-openjdk-15

ENV HOME=/home/usr/app

RUN mkdir -p $HOME

WORKDIR $HOME

ADD pom.xml $HOME

RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "verify", "clean", "--fail-never"]

ADD . $HOME

RUN ["mvn", "install"]

ENTRYPOINT ["java", "-jar", "./target/giveaway.discord.bot-0.0.1-SNAPSHOT.jar"]