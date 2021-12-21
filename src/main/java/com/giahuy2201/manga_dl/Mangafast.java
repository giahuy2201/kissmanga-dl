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
public class Mangafast implements Extractable {

	final String BASE_URL = "https://mangafast.net";
	private String title;

	@Override
	public boolean validate(String url) {
		return Pattern.matches("https?://mangafast\\.net/read/[^/]+/?",url);
	}

	@Override
	public void waitLoading(WebDriver page) {
		WebDriverWait wait = new WebDriverWait(page, 10, 0);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("Navigation")));
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
		this.title = escape(page.select("div.jd h1").first().text());
		return title;
	}

	@Override
	public String retrieveAuthors(Document page) {
		return escape(page.select("div.jd div.kri").first().text());
	}

	@Override
	public List<String> retrieveChapterPages(Document page) {
		return null;
	}

	@Override
	public List<String> retrieveChaptersNames(Document page) {
		Elements elements = page.select("tr[itemprop] td.jds a[title]");
		List<String> chaptersNames = new ArrayList<>();
		for (Element element : elements) {
			chaptersNames.add(escape(element.attr("title")));
		}
		Collections.reverse(chaptersNames);
		return chaptersNames;
	}

	@Override
	public List<String> retrieveChaptersURLs(Document page) {
		Elements elements = page.select("tr[itemprop] td.jds a[href]");
		List<String> chaptersURLs = new ArrayList<>();
		for (Element element : elements) {
			chaptersURLs.add(element.attr("href"));
		}
		Collections.reverse(chaptersURLs);
		return chaptersURLs;
	}

	@Override
	public List<String> retrieveChapterPNGs(Document page, WebDriver browser) {
		Elements elements = page.select("div.chp2 img[data-src]");
		List<String> chapterPNGs = new ArrayList<>();
		for (Element element : elements) {
			chapterPNGs.add(element.attr("data-src"));
		}
		return chapterPNGs;
	}
}