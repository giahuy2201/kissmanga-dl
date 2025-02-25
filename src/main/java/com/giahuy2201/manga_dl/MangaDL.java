package com.giahuy2201.manga_dl;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.*;

/**
 * Sub-command: download
 */
@Command(name = "download", description = "Only download image files (.png).")
class MangaDownLoad implements Callable<Integer> {

	@Parameters(paramLabel = "URL", description = "Link to manga.")
	private String mangaUrl;

	@Override
	public Integer call() throws Exception {
		MangaDL.setVerbose(MangaDL.verbose);
		MangaDL.setLogFile(MangaDL.log);
		MangaDL.extractor = new Extractor(mangaUrl);
		MangaDL.download();
		MangaDL.saveLog();
		System.out.println("done.");
		return 0;
	}
}

/**
 * Sub-command: bundle
 */
@Command(name = "bundle", description = "Pack image files (.png) into an EPUB file")
class MangaPack implements Callable<Integer> {

	@Parameters(paramLabel = "path", description = "Manga directory.")
	private String mangaDirectory;

	@Override
	public Integer call() throws Exception {
		MangaDL.setVerbose(MangaDL.verbose);
		MangaDL.setLogFile(MangaDL.log);
		MangaDL.extractor = new Extractor(new File(mangaDirectory));
		MangaDL.pack();
		MangaDL.saveLog();
		System.out.println("done.");
		return 0;
	}
}

/**
 * Main commander of the CLI
 */
@Command(name = "manga-dl", subcommands = {MangaDownLoad.class, MangaPack.class}, mixinStandardHelpOptions = true,version = "2.1", footerHeading = "Supported sites: ", footer = "Kissmanga, MangaRawr, Nettruyen")
public class MangaDL implements Callable<Integer> {

	@Parameters(paramLabel = "URL", description = "Link to manga.", arity = "0..1")
	private String mangaUrl;

	@Spec
	Model.CommandSpec spec;

	protected static int nThreads = 10;
	@Option(names = {"--threads", "-t"}, description = "Number of threads.", hidden = true, scope = ScopeType.INHERIT)
	public void setThreads(int value) {
		if (value < 1) {
			throw new ParameterException(spec.commandLine(),
					String.format("Invalid value '%s' for thread number: " +
							"nThreads > 0", value));
		}
		nThreads = value;
	}

	protected static String format = "epub";
	@Option(names = {"--format", "-f"}, description = "Output format of manga (i.e. epub, pdf).", scope = ScopeType.INHERIT)
	public void setFormat(String value) {
		if (!value.equalsIgnoreCase("epub") && !value.equalsIgnoreCase("pdf")) {
			throw new ParameterException(spec.commandLine(),
					String.format("Invalid value '%s' for output format: " +
							"epub | pdf", value));
		}
		format = value.toLowerCase();
	}

	@Option(names = {"--log", "-l"}, description = "Save log file.", scope = ScopeType.INHERIT)
	protected static boolean log = false;

	@Option(names = {"--verbose", "-v"}, description = "Enable console log.", scope = ScopeType.INHERIT)
	protected static boolean verbose = false;

	static CommandLine cli;
	static Logger logger;
	static Extractor extractor;

	public static void main(String... args) {
		MangaDL.cli = new CommandLine(new MangaDL());
		int exitCode = 1;
		try {
			exitCode = MangaDL.cli.execute(args);
			Extractor.closeContainer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(exitCode);
	}

	MangaDL() {
		MangaDL.logger = Logger.getLogger(getClass().getName());
		logger.setLevel(Level.ALL);
		// disable console output by default
		MangaDL.logger.setUseParentHandlers(false);
	}

	public Integer call() throws Exception {
		MangaDL.setVerbose(verbose);
		MangaDL.setLogFile(log);
		MangaDL.extractor = new Extractor(mangaUrl);
		MangaDL.download();
		MangaDL.pack();
		MangaDL.saveLog();
		System.out.println("done.");
		return 0;
	}

	/**
	 * Just download
	 *
	 * @throws IOException
	 */
	protected static void download() throws Exception {
		new Downloader().download();
	}

	/**
	 * Pack a manga
	 *
	 * @throws Exception
	 */
	protected static void pack() throws Exception {
		new Bundler().pack();
	}

	/**
	 * Create a log file
	 */
	protected static void setLogFile(boolean logOn) throws IOException {
		if (logOn) {
			FileHandler logFileHandler = new FileHandler("log.txt");
			logFileHandler.setFormatter(new SimpleFormatter());
			logger.addHandler(logFileHandler);
		}
	}

	/**
	 * Save log file
	 */
	protected static void saveLog() {
		File log = new File("./", "log.txt");
		if (log.exists()) {
			log.renameTo(new File(extractor.getTitle() + ".txt"));
		}
	}

	/**
	 * Enable/Disable console output
	 * @param verboseOn
	 */
	protected static void setVerbose(boolean verboseOn) {
		if (verboseOn) {
			Handler console = new ConsoleHandler();
			console.setLevel(Level.ALL);
			logger.addHandler(console);
		}
	}
}
