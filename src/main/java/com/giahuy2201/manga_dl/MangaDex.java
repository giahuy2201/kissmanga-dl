package com.giahuy2201.manga_dl;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Extract some basic info for metadata
 */
public class MangaDex implements Extractable {

	final String BASE_URL = "https://mangadex.org";
	private String title;
	private String url;

	@Override
	public boolean validate(String url) {
		this.url = url;
		return Pattern.matches("https?://mangadex\\.org/title/\\d{5}/[^/]+/?", url);
	}

	@Override
	public void waitLoading(WebDriver page) {
		WebDriverWait wait = new WebDriverWait(page, 10, 0);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("navbar")));
	}

	@Override
	public void setHeader(WebDriver page) {
		page.get(url);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MONTH, 1);
		Cookie cookie = new Cookie("mangadex_filter_langs", "1", ".mangadex.org", "/", calendar.getTime(), true);
		page.manage().addCookie(cookie);
		page.manage().window().setSize(new Dimension(700,900));
	}

	@Override
	public String baseURL() {
		return BASE_URL;
	}

	@Override
	public void getChapterList(WebDriver page) {

	}

	private String escape(String str) {
		return str.replace("/", "|").trim();
	}

	@Override
	public String retrieveTitle(Document page) {
		this.title = escape(page.select("h6.card-header span.mx-1").first().text());
		return title;
	}

	@Override
	public String retrieveAuthors(Document page) {
		return escape(page.select("div.card-body div.row:nth-child(3) div.col-xl-10").first().text());
	}

	@Override
	public List<String> retrieveChapterPages(Document page) {
		List<String> pageURLs = new ArrayList<>();
		Elements elements = page.select("ul.pagination li:not(:first-child):not(:last-child) a.page-link");
		pageURLs.add(url);
		for (int i = 1; i < elements.size(); i++) {
			pageURLs.add(BASE_URL + elements.get(i).attr("href"));
		}
		return pageURLs;
	}

	@Override
	public List<String> retrieveChaptersNames(Document page) {
		Elements elements = page.select("div.chapter-container div.row:not(:first-child) div:nth-child(2) a[href]");
		List<String> chaptersNames = new ArrayList<>();
		for (Element element : elements) {
			chaptersNames.add(escape(element.text()));
		}
		Collections.reverse(chaptersNames);
		return chaptersNames;
	}

	@Override
	public List<String> retrieveChaptersURLs(Document page) {
		Elements elements = page.select("div.chapter-container div.row:not(:first-child) div:nth-child(2) a[href]");
		List<String> chaptersURLs = new ArrayList<>();
		for (Element element : elements) {
			chaptersURLs.add(BASE_URL + element.attr("href"));
		}
		Collections.reverse(chaptersURLs);
		return chaptersURLs;
	}

	@Override
	public List<String> retrieveChapterPNGs(Document page, WebDriver browser) {
		List<String> chapterPNGs = new ArrayList<>();
		waitFor("div.d-lg-none select.form-control",browser);
		Select pngSelector = new Select(browser.findElement(By.cssSelector("div.d-lg-none select.form-control")));
		List<WebElement> elements = pngSelector.getOptions();
		for (int i = 0; i < elements.size(); i++) {
			pngSelector.selectByIndex(i);
			waitFor("div.reader-image-wrapper img[src]",browser);
			String pngURL = browser.findElement(By.cssSelector("div.reader-image-wrapper img[src]")).getAttribute("src");
			chapterPNGs.add(pngURL);
		}
		return chapterPNGs;
	}

	private void waitFor(String element, WebDriver browser){
		WebDriverWait wait = new WebDriverWait(browser, 10, 0);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(element)));
	}
}