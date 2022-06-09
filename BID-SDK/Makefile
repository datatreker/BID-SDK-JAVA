.ONESHELL:
.PHONY: all dev clean build up down run publish
all: build run
dev: build run

.PHONY: build
build:
	gradle clean build --refresh-dependencies -x test
	echo "Build phrase done."

.PHONY: publish
publish:
	gradle clean publish -x test
	echo "Publish done"