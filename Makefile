export IMAGE_PREFIX = giahuy2201
export IMAGE_NAME = kissmanga-dl
export TAG = latest

go: clean build remove
	export LINK=$(LINK)
	docker-compose up

build:
	mvn package
	docker build -t=$(IMAGE_PREFIX)/$(IMAGE_NAME):$(TAG) .

run: remove
	export LINK=$(LINK)
	docker-compose up

clean:
	mvn clean

remove:
	(docker stop $(IMAGE_NAME) && docker rm $(IMAGE_NAME)) 2>/dev/null || :
	docker-compose down

install:
	mvn package
	cat base.sh target/kissmanga-dl-1.0.jar > kissmanga-dl && chmod +x kissmanga-dl
	cp kissmanga-dl /usr/local/bin

push:
	docker push $(IMAGE_PREFIX)/$(IMAGE_NAME):$(TAG)
