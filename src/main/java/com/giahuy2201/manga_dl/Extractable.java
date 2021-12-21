package com.giahuy2201.manga_dl;

import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * All the methods Extractor needs to collect manga page data
 */
public interface Extractable {

	/**
	 * Check the site
	 * @param url URL to the manga page
	 * @return
	 */
	boolean validate(String url);

	/**
	 * Wait for the page to load
	 * @param page WebDriver object
	 */
	void waitLoading(WebDriver page);

	void setHeader(WebDriver page);

	/**
	 * Get site base URL
	 * @return
	 */
	String baseURL();

	void getChapterList(WebDriver page);

	/**
	 * Use CSS selector to find the manga title
	 * @param page Page source of the manga page
	 * @return
	 */
	String retrieveTitle(Document page);

	/**
	 * Use CSS selector to find the manga authors
	 * @param page Page source of the manga page
	 * @return
	 */
	String retrieveAuthors(Document page);

	/**
	 * Use CSS selector to collect all chapter list page url
	 * @param page Page source of the manga page
	 * @return
	 */
	List<String> retrieveChapterPages(Document page);

	/**
	 * Use CSS selector to collect all chapters names
	 * @param page Page source of the manga page
	 * @return
	 */
	List<String> retrieveChaptersNames(Document page);

	/**
	 * Use CSS selector to collect all chapters URLs
	 * @param page Page source of the manga page
	 * @return
	 */
	List<String> retrieveChaptersURLs(Document page);

	/**
	 * Use CSS selector to collect all image URLs
	 * @param page Page source of the manga chapter
	 * @return
	 */
	List<String> retrieveChapterPNGs(Document page, WebDriver browser);

}
