FROM eclipse-temurin:23-jre-alpine
ARG JAR_FILE=build/libs/tarkov-keytool-*.jar
COPY ${JAR_FILE} application.jar
EXPOSE 8080
LABEL author="Philipp Gatzka"
LABEL org.opencontainers.image.source=https://github.com/philipp-gatzka/tarkov-keytool
ENTRYPOINT ["java", "-jar", "/application.jar"]