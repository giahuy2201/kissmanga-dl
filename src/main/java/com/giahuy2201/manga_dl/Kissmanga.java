package com.giahuy2201.manga_dl;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extract some basic info for metadata
 */
public class Kissmanga implements Extractable {

	final String BASE_URL = "https://kissmanga.org";
	private String title;
	private String url;

	@Override
	public boolean validate(String url) {
		this.url = url;
		return Pattern.matches("https?://kissmanga\\.org/manga/[^/]+/?", url);
	}

	@Override
	public void waitLoading(WebDriver page) {
		WebDriverWait wait = new WebDriverWait(page, 1, 50);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("navbar")));
	}

	@Override
	public void setHeader(WebDriver page) {

	}

	@Override
	public String baseURL() {
		return BASE_URL;
	}

	@Override
	public void getChapterList(WebDriver page) {

	}

	private String escape(String str) {
		return str.replace("/", "|").replace(".", ":").trim();
	}

	@Override
	public String retrieveTitle(Document page) {
		this.title = escape(page.select("h2 strong.bigChar").first().text());
		return title;
	}

	@Override
	public String retrieveAuthors(Document page) {
		return escape(page.select("p.info:nth-child(3) a.dotUnder").first().text());
	}

	@Override
	public List<String> retrieveChapterPages(Document page) {
		List<String> pageURLs = new ArrayList<>();
		pageURLs.add(url);
		return pageURLs;
	}

	@Override
	public List<String> retrieveChaptersNames(Document page) {
		Elements elements = page.select(".listing h3 a[href]");
		List<String> chaptersNames = new ArrayList<>();
		for (Element element : elements) {
			// raw processing
			String name = escape(element.text()).replace(title, "").trim();
			name = name.startsWith("-") ? name.substring(1) : name;
			chaptersNames.add(escape(name));
		}
		Collections.reverse(chaptersNames);
		return chaptersNames;
	}

	@Override
	public List<String> retrieveChaptersURLs(Document page) {
		Elements elements = page.select(".listing h3 a[href]");
		List<String> chaptersURLs = new ArrayList<>();
		for (Element element : elements) {
			chaptersURLs.add(BASE_URL + element.attr("href"));
		}
		Collections.reverse(chaptersURLs);
		return chaptersURLs;
	}

	@Override
	public List<String> retrieveChapterPNGs(Document page, WebDriver browser) {
		Elements elements = page.select("div#centerDivVideo img");
		List<String> chapterPNGs = new ArrayList<>();
		for (Element element : elements) {
			chapterPNGs.add(element.attr("src"));
		}
		return chapterPNGs;
	}
}