package com.giahuy2201.manga_dl;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

	public boolean validate(String url) {
		return Pattern.matches("https?://kissmanga\\.org/manga/[^/]+/?", url);
	}

	private String escape(String str) {
		return str.replace("/", "|").replace(".", ":").strip();
	}

	public String retrieveTitle(Document page) {
		this.title = escape(page.select("h2 strong.bigChar").first().text());
		return title;
	}

	public String retrieveAuthors(Document page) {
		return escape(page.select("p.info:nth-child(3) a.dotUnder").first().text());
	}

	public List<String> retrieveChaptersNames(Document page) {
		Elements elements = page.select(".listing h3 a[href]");
		List<String> chaptersNames = new ArrayList<>();
		for (Element element : elements) {
			// raw processing
			String name = escape(element.text()).replace(title, "").strip();
			name = name.startsWith("-") ? name.substring(1) : name;
			chaptersNames.add(escape(name));
		}
		Collections.reverse(chaptersNames);
		return chaptersNames;
	}

	public List<String> retrieveChaptersURLs(Document page) {
		Elements elements = page.select(".listing h3 a[href]");
		List<String> chaptersURLs = new ArrayList<>();
		for (Element element : elements) {
			chaptersURLs.add(BASE_URL + element.attr("href"));
		}
		Collections.reverse(chaptersURLs);
		return chaptersURLs;
	}

	public List<String> retrieveChapterPNGs(Document page) {
		Elements elements = page.select("div#centerDivVideo img");
		List<String> chapterPNGs = new ArrayList<>();
		for (Element element : elements) {
			chapterPNGs.add(element.attr("src"));
		}
		return chapterPNGs;
	}
}