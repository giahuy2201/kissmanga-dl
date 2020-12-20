package com.giahuy2201.manga_dl;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EPUB implements Packable {

	private final ClassLoader resourceLoader;
	private Book book;

	EPUB() {
		this.resourceLoader = getClass().getClassLoader();
		this.book = new Book();
	}

	public boolean selfTracking(){
		return false;
	}

	public void addMetadata() throws IOException {
		book.getMetadata().addTitle(MangaDL.extractor.getTitle());
		List<Author> authors = new ArrayList<>();
		authors.add(new Author(MangaDL.extractor.getAuthors()));
		book.getMetadata().setAuthors(authors);
		book.setCoverImage(new Resource(new FileInputStream(
				new File(MangaDL.extractor.getMangaDirectory(), MangaDL.extractor.getCover() + ".png")), "cover.png"));
	}

	public void addResources(List<List<File>> chapterList) throws IOException {
		book.addResource(new Resource(resourceLoader.getResourceAsStream("stylesheet.css"), "stylesheet.css"));
		for (int i = 0; i < chapterList.size(); i++) {
			List<File> chapter = chapterList.get(i);
			for (File image : chapter) {
				book.addResource(new Resource(new FileInputStream(image), "images/" + image.getName()));
			}
			String chapterName = MangaDL.extractor.getChaptersNames().get(i);
			MangaDL.logger.info("Adding chapter \"" + chapterName + "\"");
			InputStream chapterHTML = chapter2InputStream(chapterList.get(i));
			book.addSection(chapterName, new Resource(chapterHTML, chapterName + ".html"));
		}
	}

	/**
	 * Create Epub book file from images and metadata in manga.xml file in
	 * mangaDirectory
	 *
	 * @throws IOException
	 */
	public void saveBook() throws IOException {
		EpubWriter epubWriter = new EpubWriter();
		epubWriter.write(book, new FileOutputStream(MangaDL.extractor.getTitle() + ".epub"));
	}

	/**
	 * Construct a html file with chapter images
	 *
	 * @param PNGs
	 * @return
	 */
	private InputStream chapter2InputStream(List<File> PNGs) throws IOException {
		StringBuilder content = new StringBuilder();
		for (File image : PNGs) {
			content.append(String.format("<p class=\"image-wrapper\"><img src=\"images/%s\" class=\"image\"/></p>\n",
					image.getName()));
		}
		String baseHTML = IOUtils.toString(resourceLoader.getResourceAsStream("chapter.html"), "UTF-8");
		baseHTML = baseHTML.replace("$CONTENT", content.toString());
		return new ByteArrayInputStream(baseHTML.getBytes(StandardCharsets.UTF_8));
	}
}
