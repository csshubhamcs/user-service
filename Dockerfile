FROM bellsoft/liberica-openjdk-alpine-musl:17

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew bootJar

# Copy the JAR with a fixed name
RUN cp build/libs/*.jar app.jar

EXPOSE 7501

# Use fixed JAR name
CMD ["java", "-jar", "app.jar"]
