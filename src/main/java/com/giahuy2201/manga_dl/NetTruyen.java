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
public class NetTruyen implements Extractable {

	final String BASE_URL = "http://www.nettruyen.com";
	private String title;

	@Override
	public boolean validate(String url) {
		return Pattern.matches("https?://www\\.nettruyen\\.com/truyen-tranh/[^/]+/?",url);
	}

	@Override
	public void waitLoading(WebDriver page) {
		WebDriverWait wait = new WebDriverWait(page, 10, 0);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("navbar-nav")));
	}

	@Override
	public String baseURL() {
		return BASE_URL;
	}

	private String escape(String str) {
		return str.replace("/", "|").replace(".", ":").strip();
	}

	@Override
	public String retrieveTitle(Document page) {
		this.title = escape(page.select("h1.title-detail").first().text());
		return title;
	}

	@Override
	public String retrieveAuthors(Document page) {
		return escape(page.select("li.author p.col-xs-8").first().text());
	}

	@Override
	public List<String> retrieveChaptersNames(Document page) {
		Elements elements = page.select("div.chapter a[href]");
		List<String> chaptersNames = new ArrayList<>();
		for (Element element : elements) {
			chaptersNames.add(escape(element.text()));
		}
		Collections.reverse(chaptersNames);
		return chaptersNames;
	}

	@Override
	public List<String> retrieveChaptersURLs(Document page) {
		Elements elements = page.select("div.chapter a[href]");
		List<String> chaptersURLs = new ArrayList<>();
		for (Element element : elements) {
			chaptersURLs.add(element.attr("href"));
		}
		Collections.reverse(chaptersURLs);
		return chaptersURLs;
	}

	@Override
	public List<String> retrieveChapterPNGs(Document page) {
		Elements elements = page.select("div.page-chapter img[src]");
		List<String> chapterPNGs = new ArrayList<>();
		for (Element element : elements) {
			chapterPNGs.add("http:"+element.attr("src"));
		}
		return chapterPNGs;
	}
}