TEST=https://kissmanga.org/manga/kimi_no_tsuku_uso_to_hontou

run:
	gradle run --args=$(TEST)

exec:
	java -cp target/kissmanga-dl-2.0.jar kissmanga_dl  $(TEST)

install:
	cat base.sh target/kissmanga-dl-1.0.jar > kissmanga-dl && chmod +x kissmanga-dl
	cp kissmanga-dl /usr/local/bin

