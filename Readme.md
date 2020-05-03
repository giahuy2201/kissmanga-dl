# Kissmanga Downloader

Don't wanna right click and save as image for every single page of your favorite manga on KissManga? Automatically download all the manga instead! Inspired by [this](https://github.com/aviaryan/Kissanime-Batch-Downloader), manga should be downloadable as well.  
 
 __Before using this script, read [TERMS OF USING](terms-of-using.md).__

Put the url of the manga you want to download from kissmanga.com (i.e. http://kissmanga.com/Manga/Shingeki-no-Kyojin) on line 16 of the docker-compose.yml file, and simply run `make go`.
 
Each manga is downloaded as a separate directory under their names. All manga directories are downloaded to `<current-working-directory>/output`. 

If you want to have cover images, before the downloading finishes, just simply go to their directories and rename the png file you want to make cover image to cover.png.

I am working on the chapter marking feature.

## How it Works
Selenium is used to control a firefox browser running in a docker container, in order to render the manga pages, and retrieve all image urls. The images are then downloaded en masse and packed into a epub file. 
Currently only supports `kissmanga.com`, no support yet for `kissmanga.io`, and other tld variants yet.
 
