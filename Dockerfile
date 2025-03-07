FROM eclipse-temurin:23-jdk
ARG JAR_FILE=build/libs/spotify_app-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} backend.jar
ENTRYPOINT [ "java","-jar","backend.jar" ]
EXPOSE 8080