import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;
import pdf.converter.epub.EpubCreator;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Note that this only works on the kissmanga.com domain, and not the
 * kissmanga.io domain TODO: Later create abstract class if we want to extend to
 * other kissmanga tlds TODO: maybe experiment with Guice dependency injection
 * TODO: support merging all pngs to pdf
 */
public class KissMangaComDownloader implements Closeable {
    private WebDriver driver;
    private final Logger logger;
    private final File outputDirectory;
    private File mangaDirectory; // save each manga to the folder of its name
    private String mangaTitle;
    private boolean mangaIsCompleted = true;
    private static final String BASE_URL = "http://kissmanga.com";

    public KissMangaComDownloader() {
        // disable popups
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("dom.popup_maximum", 0);
        profile.setPreference("privacy.popups.showBrowserMessage", false);
        profile.setPreference("dom.disable_beforeunload", true);

        logger = Logger.getLogger(KissMangaComDownloader.class.getName());
        outputDirectory = new File("output/");

        URL webdriverUrl = null;
        String seleniumHost = envOrDefault("SELENIUM_HOST", "localhost");
        String seleniumPort = envOrDefault("SELENIUM_PORT", "4444");

        try {
            webdriverUrl = new URL("http://" + seleniumHost + ":" + seleniumPort + "/wd/hub");
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Failed to parse URL", e);
            System.exit(1);
        }

        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
        driver = new RemoteWebDriver(webdriverUrl, capabilities);
    }

    private String envOrDefault(String env, String defaultValue) {
        String toReturn = System.getenv(env);
        if (toReturn == null || toReturn.isEmpty()) {
            return defaultValue;
        }
        return toReturn;
    }

    /**
     * go to any kissmanga url
     *
     * @param url
     */
    private void gotoPage(String url) {
        driver.get(url);
        // todo more robust way of checking if cloudflare is present, instead of
        // checking if the title contains "manga"
        logger.info("Waiting for cloudflare protection to be over...");
        // wait time: 120s
        WebDriverWait wait = new WebDriverWait(driver, 120, 50);
        wait.until(ExpectedConditions.titleContains("manga"));
        waitFor(3000);
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
        WebElement webElement = driver.findElement(By.id("divImage"));
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
     * Get the manga name from page title. Eg. Kimi no Na wa. manga | Read Kimi no
     * Na wa. manga online in high quanlity -> Kimi-no-Na-wa.
     *
     * @param title
     * @return
     */
    private String stripTitle(String title) {
        int cutBefore = title.indexOf(" manga");
        return title.substring(0, cutBefore).trim().replace(' ', '-').replace('/', '_');
    }

    /**
     * Format index number into ### for better EPUB bundling. Eg. 1 -> 001
     * 
     * @param url
     * @param index
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

    public void downloadIndividualMangaChapter(String url, int index) {
        gotoPage(url);
        changeToAllPagesMode();
        List<String> urlsToDownload = collectMangaImagesUrls();

        int count = 0;
        for (String urlString : urlsToDownload) {
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
                    logger.log(Level.WARNING, "Failed to retrieve: " + urlString, e);
                    logger.info("Trying a " + (failure + 1) + " times ...");
                }
                endTime = System.nanoTime();
            } while (endTime - startTime <= 60 * (long) 1000000000);
            // Has it get the frame eventually
            if (!frameIsCompleted && mangaIsCompleted) {
                mangaIsCompleted = false;
                logger.severe("Cannot bundle " + mangaTitle);
            }
            count++;
        }
    }

    /**
     * downloads all of the manga from a root manga page into separate directories
     * eg: "http://kissmanga.com/Manga/Shingeki-no-Kyojin"
     *
     * @param rootMangaPage
     */
    public void downloadAll(String rootMangaPage) {
        gotoPage(rootMangaPage);
        mangaTitle = stripTitle(driver.getTitle());
        mangaDirectory = new File(outputDirectory, mangaTitle);
        // Save logs to files
        try {
            FileHandler logFileHandler = new FileHandler(outputDirectory.getPath() + "/" + mangaTitle + ".log");
            logFileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(logFileHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // Get chapters
        String html = driver.getPageSource();
        Document page = Jsoup.parse(html);
        Elements elements = page.select("td a[href]");
        List<String> mangaChapterUrls = new ArrayList<>();
        for (Element element : elements) {
            mangaChapterUrls.add(BASE_URL + element.attr("href"));
        }
        Collections.reverse(mangaChapterUrls);
        int chapterIndex = 1;
        for (String chapterNumber : mangaChapterUrls) {
            logger.info("Downloading manga chapter from: " + chapterNumber);
            downloadIndividualMangaChapter(chapterNumber, chapterIndex);
            chapterIndex++;
        }
        if (mangaIsCompleted) {
            convert2epub();
        }
    }

    /**
     * Convert all downloaded pngs into a single epub file
     */
    private void convert2epub() {
        try {
            // exclude cover
            File cover = new File(mangaDirectory, "cover.png");
            if (cover.exists()) {
                cover.renameTo(new File(mangaDirectory, "cover.pngx"));
            }
            // convert
            String epubFileName = mangaTitle + ".epub";
            File epubFile = new File(outputDirectory, epubFileName);
            EpubCreator epubCreator = new EpubCreator();
            logger.info("Bundling: " + epubFileName);
            epubCreator.create(mangaTitle, mangaDirectory, epubFile);
            // add cover image
            cover = new File(mangaDirectory, "cover.pngx");
            if (cover.exists()) {
                EpubReader epubReader = new EpubReader();
                epubFile = new File(outputDirectory, epubFileName);
                Book book = epubReader.readEpub(new FileInputStream(epubFile));
                book.setCoverImage(new Resource(new FileInputStream(cover), "cover.png"));
                EpubWriter epubWriter = new EpubWriter();
                epubFile = new File(outputDirectory, epubFileName);
                epubWriter.write(book, new FileOutputStream(epubFile));
                // rename it back
                cover.renameTo(new File(mangaDirectory, "cover.png"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * close KissMangaComDownloader when finished downloading you cannot invoke any
     * other methods on this object afterwards, and must create a new
     * KissMangaComDownloader object
     */
    public void close() {
        if (driver != null) {
            System.out.println("DONE.");
            driver.quit();
        }
        driver = null;
    }

    public static void main(String[] args) {
        System.out.println("Waiting for selenium container to start up...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // default to attack on titan
        if (args.length < 1) {
            System.err.println("Expected usage: java -jar kissmanga-downloader.jar <url of manga>");
            System.exit(1);
        }

        try (KissMangaComDownloader downloader = new KissMangaComDownloader()) {
            for (String url : args) {
                downloader.downloadAll(url);
            }
        }
    }
}
