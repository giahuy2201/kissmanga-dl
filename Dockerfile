FROM java:latest

WORKDIR /
COPY target/kissmanga-dl-1.0.jar /kissmanga-dl.jar
CMD ["java", "-jar", "/kissmanga-dl.jar","--help"]
