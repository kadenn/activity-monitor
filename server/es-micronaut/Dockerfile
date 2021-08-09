FROM openjdk:8-jdk
COPY target/es-micronaut-0.1.jar micronaut-es.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "micronaut-es.jar"]
