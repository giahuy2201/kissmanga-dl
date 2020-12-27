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
public class MangaRawr implements Extractable {

	@Override
	public boolean validate(String url) {
		return Pattern.matches("https?://mangarawr\\.com/manga/[^/]+/?", url);
	}

	@Override
	public void waitLoading(WebDriver page) {
		WebDriverWait wait = new WebDriverWait(page, 2, 0);
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("navbar-nav")));
	}

	private String escape(String str) {
		return str.replace("\n", "").replace("/", "|").replace(".", ":").strip();
	}

	@Override
	public String retrieveTitle(Document page) {
		return escape(page.select(".post-title h3").first().text());
	}

	@Override
	public String retrieveAuthors(Document page) {
		return escape(page.select(".author-content a[href]").first().text());
	}

	@Override
	public List<String> retrieveChaptersNames(Document page) {
		Elements elements = page.select("li.wp-manga-chapter a[href]");
		List<String> chaptersNames = new ArrayList<>();
		for (Element element : elements) {;
			chaptersNames.add(escape(element.text()));
		}
		Collections.sort(chaptersNames);
		return chaptersNames;
	}

	@Override
	public List<String> retrieveChaptersURLs(Document page) {
		Elements elements = page.select("li.wp-manga-chapter a[href]");
		List<String> chaptersURLs = new ArrayList<>();
		for (Element element : elements) {
			chaptersURLs.add( element.attr("href"));
		}
		Collections.sort(chaptersURLs);
		return chaptersURLs;
	}

	@Override
	public List<String> retrieveChapterPNGs(Document page) {
		Elements elements = page.select("div.page-break img[src]");
		List<String> chapterPNGs = new ArrayList<>();
		for (Element element : elements) {
			chapterPNGs.add(element.attr("src"));
		}
		return chapterPNGs;
	}
}