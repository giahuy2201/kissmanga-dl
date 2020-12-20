TEST="https://kissmanga.org/manga/seshiji_o_pin_to_shikakou_kyougi_dancebu_e_youkoso"

run:
	gradle run --args=$(TEST)

down:
	gradle run --args="download https://kissmanga.org/manga/seshiji_o_pin_to_shikakou_kyougi_dancebu_e_youkoso"

pack:
	gradle run --args="bundle \"Seshiji O Pin! To - Shikakou Kyougi Dance-Bu E Youkoso\""

help:
	gradle run --args="-h"

install:
	cat base.sh build/libs/manga-dl-2.0.jar > manga-dl && chmod +x manga-dl
	cp manga-dl /usr/local/bin

