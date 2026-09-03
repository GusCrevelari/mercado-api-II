FROM eclipse-temurin:17-jdk AS build

WORKDIR /app
COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
RUN cp target/mercado-mvc-*.jar /app/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
