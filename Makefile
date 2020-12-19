TEST="https://kissmanga.org/manga/seshiji_o_pin_to_shikakou_kyougi_dancebu_e_youkoso"

run:
	gradle run --args=$(TEST)

down:
	gradle run --args="download https://kissmanga.org/manga/seshiji_o_pin_to_shikakou_kyougi_dancebu_e_youkoso"

pack:
	gradle run --args="bundle \"Seshiji O Pin! To - Shikakou Kyougi Dance-Bu E Youkoso\""

help:
	gradle run --args="-h"

exec:
	gradle build
	java -cp target/kissmanga-dl-2.0.jar MangaDL $(TEST)

install:
	cat base.sh target/kissmanga-dl-1.0.jar > kissmanga-dl && chmod +x kissmanga-dl
	cp kissmanga-dl /usr/local/bin

