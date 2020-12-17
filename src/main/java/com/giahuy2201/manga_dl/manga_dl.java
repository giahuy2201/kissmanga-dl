package com.giahuy2201.manga_dl;

import org.apache.commons.cli.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main class controlling CLI and branching
 */

public class manga_dl implements Closeable {
    protected CommandLine command;
    protected Options cliOptions;
    protected Logger logger;
    protected FileHandler logFileHandler;
    private boolean logFileIsOn = false;
    private boolean verboseIsOn = false;

    public static void main(String[] args) {

        try (manga_dl cli = new manga_dl(args)) {
            try {
                // start kissmanga cli
                File currentDirectory = new File("./");
                // options
                if (cli.command.hasOption("help")) {
                    HelpFormatter helpFormatter = new HelpFormatter();
                    helpFormatter.printHelp("kissmanga-dl [options] URL [URL...]", "\nwhere options include:",
                            cli.cliOptions, "Neither -d nor -p options passed, packing will follow downloading");
                    System.exit(0);
                }
                // logging on
                if (cli.command.hasOption("l")) {
                    cli.setLogger(true);
                }
                // verbose on
                if (cli.command.hasOption("v")) {
                    cli.setVerbose(true);
                }
                // if there are links
                if (cli.command.getArgs().length > 0) {
                    String[] urls = cli.command.getArgs();
                    // check url
                    if (!KissmangaExtractor.valid(urls[0])) {
                        printUsage("Unsupported URL");
                    }
                    // download
                    if (cli.command.hasOption("d")) {
//                        String port = cli.startSelenium();
                        // wait for seleninum docker starting
                        Thread.sleep(5000);
                        // download only
                        cli.download(urls[0], currentDirectory);
                    } else if (cli.isDefaultOption()) {
//                        String port = cli.startSelenium();
                        // wait for seleninum docker starting
                        Thread.sleep(5000);
                        // by default download and pack
                        cli.downloadPack(urls[0], currentDirectory);
                    } else {
                        // unwanted syntax: -p URL
                        printUsage("Unwanted syntax");
                    }
                } else {
                    if (cli.command.hasOption('p')) {
                        // pack all manga
                        cli.packAll(currentDirectory);
                    } else {
                        printUsage("You must provide at least one URL");
                    }
                }

                // } catch (DockerException e) {
                //     System.out.println("Problem starting Selenium container. Restart Docker or check your installation");
                // } catch (IOException e) {
                //     System.out.println("ERROR: Invalid URL");
                // } catch (TimeoutException e) {
                //     System.out.println("ERROR: Problem retrieving pages. Try again in a moment");
                // } catch (WebDriverException e) {
                //     System.out.println("ERROR: Problem connecting to Selenium container. Try again in a moment");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    manga_dl(String[] args) {
        // Create optionsgetCommand
        cliOptions = new Options();
        Option downloadOption = Option.builder("d").desc("Download manga in PNG format").build();
        cliOptions.addOption(downloadOption);
        Option packOption = Option.builder("p").desc("Pack all PNGs in manga folder into an EPUB file").build();
        cliOptions.addOption(packOption);
        Option logOption = Option.builder("l").desc("Print output log files").build();
        cliOptions.addOption(logOption);
        Option verboseOption = Option.builder("v").desc("Show all output messages").build();
        cliOptions.addOption(verboseOption);
        Option helpOption = Option.builder().longOpt("help").desc("Show this help").build();
        cliOptions.addOption(helpOption);
        // Parse command
        CommandLineParser cliParser = new DefaultParser();
        try {
            command = cliParser.parse(cliOptions, args);
        } catch (ParseException e) {
            printUsage("Invalid option(s).");
        }
        // Create logger, logger doesnt print on screen by default
        logger = Logger.getLogger(getClass().getName());
        setLogger(false);
        setVerbose(false);
    }


    /**
     * Wrong syntax detected, print message
     *
     * @param message
     */
    public static void printUsage(String message) {
        // no url found
        System.out.println("usage: kissmanga-dl [options] URL [URL...]\n" + "\nkissmanga-dl: error: " + message
                + "\nType kissmanga-dl --help to see a list of all options.");
        System.exit(1);
    }

    /**
     * Download then pack
     *
     * @param url
     * @param outputDirectory
     * @throws IOException
     */
    public void downloadPack(String url, File outputDirectory) throws IOException {
        Downloader downloader = new Downloader(outputDirectory, logger, verboseIsOn, url);
        String name = downloader.getExtractor().getTitle();
        if (logFileIsOn) {
            addLogFile(outputDirectory, name);
        }
        System.out.println("Downloading " + name);
        File mangaDirectory = downloader.getMangaDirectory();
        downloader.download();
        pack(mangaDirectory, outputDirectory, true);
    }

    /**
     * Just download
     *
     * @param url
     * @param outputDirectory
     * @throws IOException
     */
    public void download(String url, File outputDirectory) throws IOException {
        Downloader downloader = new Downloader(outputDirectory, logger, verboseIsOn, url);
        String name = downloader.getExtractor().getTitle();
        if (logFileIsOn) {
            addLogFile(outputDirectory, name);
        }
        System.out.println("Downloading " + name);
        downloader.download();
    }

    /**
     * Pack all manga found in the directory
     *
     * @param outputDirectory
     * @throws IOException
     */
    public void packAll(File outputDirectory) throws IOException {
        // Bundle all manga folder found
        for (File folder : outputDirectory.listFiles()) {
            String name = folder.getName();
            File mangaDirectory = new File(outputDirectory, name);
            pack(mangaDirectory, outputDirectory, false);
        }
    }

    /**
     * Pack a manga
     *
     * @param mangaDirectory
     * @param outputDirectory
     * @param sameLogFile
     * @throws IOException
     */
    public void pack(File mangaDirectory, File outputDirectory, boolean sameLogFile) throws IOException {
        EpubBundler creator = new EpubBundler(outputDirectory, logger);
        if (mangaDirectory.isDirectory()) {
            String name = creator.getTitle(mangaDirectory);
            if (name == null) {
                return;
            }
            if (logFileIsOn && !sameLogFile) {
                addLogFile(outputDirectory, name);
            }
            System.out.println("Bundling " + name);
            creator.create(mangaDirectory);
        }
    }

    public void setLogger(boolean on) {
        logFileIsOn = on;
    }

    public void setVerbose(boolean on) {
        verboseIsOn = on;
        logger.setUseParentHandlers(on);
    }

    /**
     * Create a log file
     *
     * @param outputDirectory
     * @param name
     */
    public void addLogFile(File outputDirectory, String name) {
        try {
            // close and remove the previous log file handler currently in logger
            if (logFileHandler != null) {
                logFileHandler.flush();
                logFileHandler.close();
            }
            logger.removeHandler(logFileHandler);
            if (new File(outputDirectory, name + ".log").exists()) {
                logFileHandler = new FileHandler(outputDirectory.getPath() + "/" + name + ".log", true);
            } else {
                logFileHandler = new FileHandler(outputDirectory.getPath() + "/" + name + ".log");
            }
            logFileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(logFileHandler);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create log file", e);
            // e.printStackTrace();
        }
    }

    /**
     * Only v or l option provided
     *
     * @return
     */
    protected boolean isDefaultOption() {
        int commandNumber = command.getOptions().length;
        if (commandNumber == 0) {
            return true;
        }
        if (commandNumber == 1 && (command.hasOption("v") || command.hasOption("l"))) {
            return true;
        }
        if (commandNumber == 2 && command.hasOption("v") && command.hasOption("l")) {
            return true;
        }
        return false;
    }

    @Override
    public void close() { }
}