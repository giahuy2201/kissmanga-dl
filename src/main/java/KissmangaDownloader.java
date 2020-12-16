
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloader class to work with Selenium container
 */
public class KissmangaDownloader {
    private final WebDriver driver;
    private final Logger logger;
    private final File outputDirectory;
    private File mangaDirectory; // save each manga to the folder of its name
    private boolean mangaIsCompleted = true;
    private final boolean verboseIsOn;
    private KissmangaExtractor extractor;
    private int resumingIndex;

    public KissmangaDownloader(File outputDirectory, Logger logger, boolean verboseIsOn, String url) throws IOException {
        this.verboseIsOn = verboseIsOn;
        if (!verboseIsOn) {
            Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        }
        this.logger = logger;
        this.outputDirectory = outputDirectory;

        System.setProperty("webdriver.gecko.driver", "/Users/giahuy/Downloads/geckodriver");
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");

        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        // disable popups
        options.addPreference("dom.popup_maximum", 0);
        options.addPreference("privacy.popups.showBrowserMessage", false);
        options.addPreference("dom.disable_beforeunload", true);
        driver = new FirefoxDriver(options);

        setup(url);
    }

    /**
     * go to any kissmanga url
     *
     * @param url
     */
    private void gotoPage(String url) {
        if (!driver.getCurrentUrl().equals(url)) {
            driver.get(url);
            logger.info("Waiting for cloudflare protection to be over...");
            WebDriverWait wait = new WebDriverWait(driver, 1, 50);
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("navbar")));
            waitFor(1000);
        }
    }

    private void waitFor(long millis) {
        try {
            logger.info("Waiting for " + millis + " ms...");
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void changeToAllPagesMode() {
        Select select = new Select(driver.findElement(By.id("selectReadType")));
        WebElement element = select.getFirstSelectedOption();
        if (!element.getText().equals("All pages")) {
            logger.info("Switching to all pages mode...");
            select.selectByVisibleText("All pages");
            waitFor(15000);
            logger.info("Finished waiting...");
        }
    }

    private List<String> collectMangaImagesUrls() {
        logger.info("Getting all manga images urls...");
        WebElement webElement = driver.findElement(By.id("centerDivVideo"));
        String html = webElement.getAttribute("outerHTML");
        Document document = Jsoup.parseBodyFragment(html);
        Elements elements = document.getElementsByAttribute("src");
        List<String> toReturn = new ArrayList<String>();
        for (Element element : elements) {
            toReturn.add(element.attr("src"));
        }
        return toReturn;
    }

    /**
     * Format index number into ### for better EPUB bundling. Eg. 1 -> 001
     *
     * @param count decimal number
     */
    private String formatIndex(int count) {
        final int LENGTH = 3;
        String index = count + "";
        int countLength = index.length();
        for (int i = 0; i < LENGTH - countLength; i++) {
            index = "0" + index;
        }
        return index;
    }

    public File getMangaDirectory() {
        return mangaDirectory;
    }

    public KissmangaExtractor getExtractor() {
        return extractor;
    }

    public void downloadChapter(String url, int index) {
        gotoPage(url);
        // changeToAllPagesMode();
        List<String> urlsToDownload = collectMangaImagesUrls();
        Iterable<String> urlList = urlsToDownload;
        if (!verboseIsOn) {
            urlList = ProgressBar.wrap(urlsToDownload, "Chapter " + (index + 1));
        }

        int count = 0;
        for (String urlString : urlList) {
            logger.info("Retrieving: " + urlString);
            // todo infer correct extension, instead of hardcoding png
            String frameFileName = formatIndex(index) + "-" + formatIndex(count) + ".png";
            File outputFile = new File(mangaDirectory, frameFileName);
            // Keep trying to download the frame for 60s
            long startTime = System.nanoTime();
            long endTime = startTime;
            long failure = 0;
            boolean frameIsCompleted = false;
            do {
                try {
                    URL mangaChapterUrl = new URL(urlString);
                    FileUtils.copyURLToFile(mangaChapterUrl, outputFile);
                    frameIsCompleted = true;
                    if (failure > 0) {
                        logger.info("Retrieved successfully!");
                    }
                    break;
                } catch (IOException e) {
                    failure++;
                    logger.log(Level.WARNING, "Failed to retrieve: " + urlString);
                    logger.info("Trying a " + (failure + 1) + " times ...");
                }
                endTime = System.nanoTime();
            } while (endTime - startTime <= 60 * (long) 1000000000);
            // Has it get the frame eventually
            if (!frameIsCompleted && mangaIsCompleted) {
                mangaIsCompleted = false;
                logger.severe("Cannot bundle " + extractor.getTitle());
            }
            count++;
        }
    }

    /**
     * Prepare
     */
    public void setup(String url) throws IOException {
        gotoPage(url);
        String html = driver.getPageSource();
        Document page = Jsoup.parse(html);
        // Extract manga info
        extractor = new KissmangaExtractor(page);
        mangaDirectory = new File(outputDirectory, extractor.getTitle());
        mangaDirectory.mkdir();
        exportProfile(extractor.getTitle(), extractor.getAuthors());
        // Resume
        this.resumingIndex = getLatestDownloadedChapter();
        if (resumingIndex != 0) {
            List<String> mangaChapterNames = extractor.getChapterNames();
            System.out.println("Resuming at " + mangaChapterNames.get(resumingIndex));
        }
    }

    /**
     * Download
     *
     * @throws IOException
     */
    public void download() throws IOException {
        List<String> chapterUrls = extractor.getChapterUrls();
        chapterUrls = chapterUrls.subList(resumingIndex, chapterUrls.size());
        int index = resumingIndex;

        for (String chapterUrl : chapterUrls) {
            logger.info("Downloading manga chapter from: " + chapterUrl);
            downloadChapter(chapterUrl, index++);
        }
        logger.info("Downloading finished");
        driver.close();
    }

    /**
     * Export manga profile with known metadata to manga.xml
     */
    private void exportProfile(String title, String author) {
        try {
            String baseXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("manga.xml"), "UTF-8");
            baseXML = baseXML.replace("$TITLE", title);
            baseXML = baseXML.replace("$COVER", "000-000.png");
            baseXML = baseXML.replace("$LANG", "en");
            baseXML = baseXML.replace("$AUTHOR", author);
            PrintWriter printWriter = new PrintWriter(new File(mangaDirectory, "manga.xml"));
            printWriter.write(baseXML);
            printWriter.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create manga.xml file", e);
            e.printStackTrace();
        }
    }

    /**
     * Find the latest chapter number to resume
     */
    private int getLatestDownloadedChapter() {
        File[] mangaFrames = mangaDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().matches("\\d{3}-\\d{3}.png$");
            }
        });
        if (mangaFrames == null || mangaFrames.length == 0) {
            return 0;
        }
        File latestFile = Collections.max(Arrays.asList(mangaFrames));
        return Integer.parseInt(latestFile.getName().substring(0, 3));
    }
}
