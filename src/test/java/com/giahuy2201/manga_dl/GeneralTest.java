package com.giahuy2201.manga_dl;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.logging.Level;


public class GeneralTest {

    @Test
    public void testChromeDriver() {
        System.setProperty("webdriver.gecko.driver", "/Users/giahuy/Downloads/geckodriver");
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        WebDriver drv = new FirefoxDriver(options);
        drv.get("https://kissmanga.org/manga/kxqh9261558062112");
        KissmangaExtractor kex = new KissmangaExtractor(Jsoup.parse(drv.getPageSource()));
        System.out.println(kex.getTitle());
        drv.close();
    }
}
