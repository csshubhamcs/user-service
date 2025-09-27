FROM bellsoft/liberica-openjdk-alpine-musl:17

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew bootJar

EXPOSE 7501

CMD ["java", "-jar", "build/libs/*.jar"]
