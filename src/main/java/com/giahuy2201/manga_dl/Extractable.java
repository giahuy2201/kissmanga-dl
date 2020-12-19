package com.giahuy2201.manga_dl;

import org.jsoup.nodes.Document;

import java.util.List;

public interface Extractable {

	boolean validate(String url);

	String retrieveTitle(Document page);

	String retrieveAuthors(Document page);

	List<String> retrieveChaptersNames(Document page);

	List<String> retrieveChaptersURLs(Document page);

	List<String> retrieveChapterPNGs(Document page);
}
