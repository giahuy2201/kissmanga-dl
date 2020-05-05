FROM java:latest

WORKDIR /
COPY target/kissmanga-downloader-2.4.jar /kissmanga-downloader.jar
CMD ["java", "-jar", "/kissmanga-downloader.jar"]
