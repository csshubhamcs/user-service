FROM bellsoft/liberica-openjdk-alpine-musl:17

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew bootJar

# Copy only the executable JAR (not the plain one)
RUN cp build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 7501

CMD ["java", "-jar", "app.jar"]
