import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main class controlling CLI and branching
 */

public class kissmanga_dl {
    protected DockerClient docker;
    protected String seleniumID;
    protected CommandLine command;
    protected Options cliOptions;
    protected Logger logger;
    protected FileHandler logFileHandler;
    private boolean logFileIsOn = false;
    private boolean verboseIsOn = false;

    public static void main(String[] args) {

        try {
            // start kissmanga cli
            kissmanga_dl cli = new kissmanga_dl(args);
            File currentDirectory = new File("./");
            // options
            if (cli.command.hasOption("help")) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("kissmanga-dl [options] URL [URL...]", "\nwhere options include:",
                        cli.cliOptions, "Neither -d nor -p options passed, packing will follow downloading.");
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
                if (!fromKissmanga(urls[0])) {
                    printUsage("Unsupported URL.");
                }
                // download
                if (cli.command.hasOption("d")) {
                    String port = cli.startSelenium();
                    // wait for seleninum docker starting
                    Thread.sleep(5000);
                    // download only
                    cli.downloadAll(urls, currentDirectory, port);
                } else if (cli.isDefaultOption()) {
                    String port = cli.startSelenium();
                    // wait for seleninum docker starting
                    Thread.sleep(5000);
                    // by default download and pack
                    cli.downloadAllPack(urls, currentDirectory, port);
                } else {
                    // unwanted syntax: -p URL
                    printUsage("Unwanted syntax.");
                }
            } else {
                if (cli.command.hasOption('p')) {
                    // pack all manga
                    cli.packAll(currentDirectory);
                } else {
                    printUsage("You must provide at least one URL.");
                }
            }

            // stop & remove created container
            if (cli.seleniumID != null) {
                cli.logger.info("Closing Selenium container ...");
                cli.docker.killContainer(cli.seleniumID);
                cli.docker.removeContainer(cli.seleniumID);
            }
        } catch (DockerException e) {
            System.out.println("Problem starting Selenium container. Restart Docker or check your installation.");
        } catch (ParseException e) {
            printUsage("Invalid option(s).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    kissmanga_dl(String[] args) throws ParseException {
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
        command = cliParser.parse(cliOptions, args);
        // Create logger, logger doesnt print on screen by default
        logger = Logger.getLogger(getClass().getName());
        logger.setUseParentHandlers(false);

    }

    /**
     * Start a Selenium container when running with an executable
     * 
     * @return Container port to communicate with downloader
     * @throws DockerException
     * @throws InterruptedException
     * @throws DockerCertificateException
     */
    public String startSelenium() throws DockerException, InterruptedException, DockerCertificateException {
        logger.info("Wait for selenium to start ...");
        // make docker-compose compatible
        String setPort = System.getenv("SELENIUM_PORT");
        if (setPort != null && !setPort.isEmpty()) {
            // running with docker-compose up
            return setPort;
        }
        // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
        docker = DefaultDockerClient.fromEnv().build();
        // shm-size & image
        final long SHM_SIZE = 2 * 1024 * 1024 * (long) 1024;
        final String SELENIUM_IMAGE = "selenium/standalone-firefox-debug:latest";

        // Pull an image
        docker.pull(SELENIUM_IMAGE);

        // Bind container port 4444 to port 4444 in host
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        List<PortBinding> randomPort = new ArrayList<>();
        String port = "" + (int) (32768 + Math.random() * 30000);
        PortBinding portBinding = PortBinding.of("0.0.0.0", port);
        randomPort.add(portBinding);
        portBindings.put("4444/tcp", randomPort);

        // Create container from selenium image
        final HostConfig hostConfig = HostConfig.builder().shmSize(SHM_SIZE).portBindings(portBindings).build();
        final ContainerConfig containerConfig = ContainerConfig.builder().hostConfig(hostConfig).image(SELENIUM_IMAGE)
                .build();
        final ContainerCreation creation = docker.createContainer(containerConfig);
        seleniumID = creation.id();
        // Start container
        docker.startContainer(seleniumID);

        return port;
    }

    /**
     * Check if it is a valid link
     * 
     * @param url
     * @return
     */
    public static boolean fromKissmanga(String url) {
        return url.indexOf("https://kissmanga.com/Manga/") == 0;
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
     * @param urls
     * @param outputDirectory
     * @param port
     * @throws IOException
     */
    public void downloadAllPack(String[] urls, File outputDirectory, String port) throws IOException {
        KissmangaDownloader downloader = new KissmangaDownloader(outputDirectory, logger, port);
        for (String url : urls) {
            if (!fromKissmanga(url)) {
                continue;
            }
            if (logFileIsOn) {
                String name = downloader.getTitle(url);
                addLogFile(outputDirectory, name);
            }
            File mangaDirectory = downloader.download(url);
            pack(mangaDirectory, outputDirectory, true);
        }
    }

    /**
     * Just download
     * 
     * @param urls
     * @param outputDirectory
     * @param port
     */
    public void downloadAll(String[] urls, File outputDirectory, String port) {
        KissmangaDownloader downloader = new KissmangaDownloader(outputDirectory, logger, port);
        for (String url : urls) {
            if (!fromKissmanga(url)) {
                continue;
            }
            if (logFileIsOn) {
                String name = downloader.getTitle(url);
                addLogFile(outputDirectory, name);
            }
            downloader.download(url);
        }
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
        EpubCreator creator = new EpubCreator(outputDirectory, logger);
        if (mangaDirectory.isDirectory()) {
            if (logFileIsOn) {
                String name = creator.getTitle(mangaDirectory);
                if (name == null) {
                    return;
                }
                if (!sameLogFile) {
                    addLogFile(outputDirectory, name);
                }
            }
            creator.create(mangaDirectory);
        }
    }

    public void setLogger(boolean on) {
        logFileIsOn = on;
    }

    public void setVerbose(boolean on) {
        verboseIsOn = on;
        if (on) {
            logger.setUseParentHandlers(true);
        }
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
}