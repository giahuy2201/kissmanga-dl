package com.giahuy2201.manga_dl;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main class controlling CLI and branching
 */

@Parameters(commandDescription = "Download and pack manga")
public class MangaDL {

    @Parameters(commandDescription = "Only download image files (.png)")
    private class MangaDownLoad {

        @Parameter(description = "URL", required = true)
        private String mangaUrl;

        @Parameter(names = {"--log", "-l"}, description = "Save log file")
        private boolean log = false;
    }

    @Parameters(commandDescription = "Pack image files (.png) into EPUB files")
    private class MangaPack {

        @Parameter(description = "PNG folder path")
        private String mangaUrl;

        @Parameter(names = {"--log", "-l"}, description = "Save log file")
        private boolean log = false;
    }

    @Parameter(description = "URL", required = true)
    private String mangaUrl;

    @Parameter(names = {"--log", "-l"}, description = "Save log file")
    private boolean log = false;

    @Parameter(names = "--help", description = "Show this help", help = true)
    private boolean help;

    JCommander commander;
    MangaDownLoad downLoad;
    MangaPack pack;
    static Logger logger;
    static Extractor extractor;

    public static void main(String[] args) {
        MangaDL mdl = new MangaDL();

        try {
            mdl.commander.parse(args);
            mdl.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    MangaDL() {
        this.downLoad = new MangaDownLoad();
        this.pack = new MangaPack();
        this.commander = JCommander.newBuilder().addObject(this).addCommand("download", downLoad).addCommand("pack", pack).build();
        MangaDL.logger = Logger.getLogger(getClass().getName());
    }

    public void run() throws Exception {
        if (help) {
            commander.usage();
        } else {
            String command = commander.getParsedCommand();
            String url = this.mangaUrl;
            boolean log = this.log;

            if (command.equals("pack")) {
                pack();
            } else if (command.equals("download")) {
                url = downLoad.mangaUrl;
                log = downLoad.log;
            }

            if (Extractor.valid(url)) {
                this.extractor = new KissmangaExtractor(url);
                System.out.println(extractor.getTitle());

                if (log) {
                    addLogFile();
                }
                download();
                if (command.equals("")) {
                    pack();
                }
            } else {
                System.out.println("Unsupported URL");
            }
        }
    }


    /**
     * Just download
     *
     * @throws IOException
     */
    public void download() throws IOException {
        new Downloader().download();
    }

    /**
     * Pack a manga
     *
     * @throws IOException
     */
    public void pack() throws Exception {
        File mangaDirectory = extractor.getMangaDirectory();
        List<File> folders = new ArrayList<>();

        if (mangaDirectory == null) {
            folders = Arrays.asList(new File("./").listFiles());
        } else {
            folders.add(mangaDirectory);
        }

        EPUBBundler creator;
        for (File folder : folders) {
            creator = new EPUBBundler();
            if (mangaDirectory.isDirectory()) {
                creator.create();
            }
        }
    }

    /**
     * Create a log file
     */
    public void addLogFile() throws IOException {
        FileHandler logFileHandler = new FileHandler("log.txt");
        logFileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(logFileHandler);
    }
}