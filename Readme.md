# kissmanga-dl

_A word from the original repo's author:_

> Don't wanna right click and save as image for every single page of your favorite manga on KissManga?
> Automatically download all the manga instead! Inspired by [this](https://github.com/aviaryan/Kissanime-Batch-Downloader), manga should be downloadable as well.

Simply, you give _kissmanga-dl_ the link to your favorite manga, and it will take care of all downloading and packing, and return you an **EPUB** file that you can read anywhere on your smartphones.

Here is how the manga look like in [_Lithium_](https://play.google.com/store/apps/details?id=com.faultexception.reader) app.
![Created EPUB files on mobile](screens-demo.png)

**Before using this script, read [TERMS OF USING](terms-of-using.md).**

## Features

- [x] Download and pack manga into **EPUB** files.
- [x] Chapter mark manga **EPUB** files.
- [ ] Minimize created **EPUB** file size.
- [x] Run as a CLI command like _youtube-dl_

## Usage

_kissmanga-dl runs on [Docker](https://docs.docker.com/get-docker/) containers and uses [Maven](http://maven.apache.org/install.html) as build tool, so you should have them installed._

1. Fetch this repo.
2. Run `make run LINK=<your-manga-link>`.

_Note: This step will run kissmanga-dl using docker-compose._

### For Mac/Linux

You can run it without _docker-compose_

- Run `make install` in the repo directory to install _kissmanga-dl_ systemwide, and you can run it anywhere like _youtube-dl_

_Note: This step will automatically copy the generated executable kissmanga-dl to `/usr/local/bin`._

Alternatively, you can also download the executable file in the Release section, make it executable using command `chmod +x kissmanga-dl` and run it `./kissmanga-dl` in that same directory.

### Examples

Run using _docker-compose_:

```
make run LINK=https://kissmanga.com/Manga/Kimi-no-Na-wa
```

Run using executable after installation:

```
kissmanga-dl -vl https://kissmanga.com/Manga/Kimi-no-Na-wa
```

will download the manga, print all messages and generate a log file.

```
kissmanga-dl -d https://kissmanga.com/Manga/Kimi-no-Na-wa https://kissmanga.com/Manga/Kimi-no-Suizou-wo-Tabetai https://kissmanga.com/Manga/Sins
```

will only download all 3 manga without detail output.

```
kissmanga-dl -pl
```

will pack all found manga in current directory and generate log files.

Each manga is downloaded as a separate directory under their names and their **EPUB** files will be located at your current working directory.

For those who install _kissmanga-dl_ on Mac/Linux, the CLI offers you options to download and pack files seperately as well as generate log files. That is if you would like a bit more customization over your **EPUB** files, you can download the manga first, then edit the `manga.xml` file in each manga folder, and pack them into **EPUB** files when you are ready.

## How it Works

Selenium is used to control a firefox browser running in a docker container, in order to render the manga pages, and retrieve all image urls. The images are then downloaded en masse and packed into a epub file.
Currently only supports `kissmanga.com`, no support yet for `kissmanga.io`, and other tld variants yet.
