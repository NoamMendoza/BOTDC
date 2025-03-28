FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew build

COPY .env /app/

EXPOSE 8080

CMD ["./gradlew", "run"]