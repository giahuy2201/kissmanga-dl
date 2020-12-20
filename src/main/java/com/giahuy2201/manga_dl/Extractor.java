package com.giahuy2201.manga_dl;

import me.tongfei.progressbar.ProgressBar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import picocli.CommandLine.ParameterException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlRootElement(name = "manga")
@XmlAccessorType(XmlAccessType.FIELD)
public class Extractor implements Serializable {

	@XmlRootElement(name = "chapter")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class Chapter implements Serializable {
		private String name;

		@XmlElementWrapper(name = "images")
		@XmlElement(name = "images")
		private List<String> images;

		Chapter() {
		}

		Chapter(String name) {
			this.name = name;
		}
	}

	@XmlTransient
	private WebDriver browser;
	@XmlTransient
	private Extractable source;
	@XmlTransient
	private List<String> chaptersURLs;
	@XmlTransient
	private Document page;
	@XmlTransient
	private File mangaDirectory;
	@XmlTransient
	private int index;

	private String cover;
	private String title;
	private String authors;

	@XmlElementWrapper(name = "chapters")
	private List<Chapter> chapters;

	Extractor() {
	}

	Extractor(String url) throws Exception {
		this.source = new Kissmanga();
		if (url == null) {
			throw new ParameterException(MangaDL.cli, "Missing URL");
		}
		if (!source.validate(url)) {
			throw new ParameterException(MangaDL.cli,"Unsupported url");
		}

		Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

		System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver");
		System.setProperty(ChromeDriverService.CHROME_DRIVER_SILENT_OUTPUT_PROPERTY, "true");
		System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "/dev/null");

		ChromeOptions options = new ChromeOptions();
		options.setHeadless(true);
		this.browser = new ChromeDriver(options);

		retrieveData(url);
		browser.close();
		MangaDL.logger.finest("EXTRACTING finished\n");
	}

	Extractor(File mangaDirectory) throws Exception {
		this.mangaDirectory = mangaDirectory;
		if (!mangaDirectory.exists()) {
			throw new IOException("Manga folder does not exist!");
		}
		File xmlFile = new File(mangaDirectory, "manga.xml");
		readData(xmlFile);
		MangaDL.logger.finest("EXTRACTING finished\n");
	}

	private void retrieveData(String url) throws Exception {
		getPage(url);
		this.title = source.retrieveTitle(page);
		this.mangaDirectory = new File(title);
		File xmlFile = new File(mangaDirectory, "manga.xml");
		if (xmlFile.exists()) {
			readData(xmlFile);
		} else {
			System.out.println(title);
			MangaDL.logger.info("Retrieving info of manga " + title);
			this.chapters = new ArrayList<>();
			this.cover = "000-000";
			this.authors = source.retrieveAuthors(page);
			this.chaptersURLs = source.retrieveChaptersURLs(page);
			setChaptersNames(source.retrieveChaptersNames(page));
			Iterable<String> chaptersURLs = this.chaptersURLs;
			if (!MangaDL.verbose) {
				chaptersURLs = ProgressBar.wrap(this.chaptersURLs, "Extracting");
			}
			for (String chapterUrl : chaptersURLs) {
				getPage(chapterUrl);
				MangaDL.logger.info("Collecting image urls from " + chapterUrl);
				setChapterPNGs(source.retrieveChapterPNGs(page));
				MangaDL.logger.fine("Found " + chapters.get(index).images.size() + " frames");
				this.index++;
			}
			writeData();
		}
	}

	private void getPage(String url) {
		MangaDL.logger.info("Opening page " + url);
		browser.get(url);
		WebDriverWait wait = new WebDriverWait(browser, 1, 50);
		wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("navbar")));
		this.page = Jsoup.parse(browser.getPageSource());
	}

	private void readData(File xmlFile) throws Exception {
		MangaDL.logger.fine("Found manga.xml file");
		JAXBContext jaxbContext = JAXBContext.newInstance(Extractor.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Extractor ext = (Extractor) jaxbUnmarshaller.unmarshal(xmlFile);
		// copy them over
		this.title = ext.title;
		System.out.println(title);
		this.cover = ext.cover;
		this.authors = ext.authors;
		this.index = ext.index;
		this.chapters = ext.chapters;
	}

	private void writeData() throws Exception {
		MangaDL.logger.info("Saving manga.xml");
		JAXBContext context = JAXBContext.newInstance(Extractor.class);
		Marshaller mar = context.createMarshaller();
		mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		//write to file
		if (mangaDirectory.mkdir()) {
			mar.marshal(this, new File(mangaDirectory, "manga.xml"));
		} else {
			throw new IOException("Cannot save manga.xml file!");
		}
	}

	public String getCover() {
		return cover;
	}

	public Document getPage() {
		return page;
	}

	public File getMangaDirectory() {
		return mangaDirectory;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthors() {
		return authors;
	}

	public List<String> getChaptersNames() {
		List<String> chaptersNames = new ArrayList<>();
		for (Chapter chapter : chapters) {
			chaptersNames.add(chapter.name);
		}
		return chaptersNames;
	}

	public List<List<String>> getChaptersPNGs() {
		List<List<String>> chaptersPNGs = new ArrayList<>();
		for (Chapter chapter : chapters) {
			chaptersPNGs.add(chapter.images);
		}
		return chaptersPNGs;
	}

	protected void setCover(String cover) {
		this.cover = cover;
	}

	protected void setChaptersNames(List<String> chaptersNames) {
		for (String chaptersName : chaptersNames) {
			chapters.add(new Chapter(chaptersName));
		}
	}

	protected void setChapterPNGs(List<String> chapterPNGs) {
		chapters.get(index).images = chapterPNGs;
	}
}
