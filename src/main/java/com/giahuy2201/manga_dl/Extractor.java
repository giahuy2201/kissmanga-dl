package com.giahuy2201.manga_dl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import me.tongfei.progressbar.ProgressBar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import picocli.CommandLine.ParameterException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class representing manga data
 */
@XmlRootElement(name = "manga")
@XmlAccessorType(XmlAccessType.FIELD)
public class Extractor implements Serializable {

	@XmlAccessorType(XmlAccessType.FIELD)
	private static class Chapter implements Serializable {
		private String name;

		@XmlElementWrapper(name = "images")
		@XmlElement(name = "image")
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
	private List<String> remainingChapterURLs;
	@XmlTransient
	private Document page;
	@XmlTransient
	private File mangaDirectory;
	@XmlTransient
	private int resumingIndex;
	@XmlTransient
	private List<Future<String>> results;

	private String cover;
	private String title;
	private String authors;

	@XmlElementWrapper(name = "chapters")
	@XmlElement(name = "chapter")
	private List<Chapter> chapters;
	private static DockerClient dockerClient;
	private static String containerId;
	private static boolean running;

	Extractor() {
	}

	Extractor(String url) throws Exception {
		selectExtractor(url);
		if (!source.validate(url)) {
			throw new ParameterException(MangaDL.cli, "Unsupported url");
		}

		try {
			startContainer();
		} catch (Exception e) {
			throw new ParameterException(MangaDL.cli, "Check your Docker and try again");
		}
		Extractor.running = true;
		Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
		System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver");
		System.setProperty(ChromeDriverService.CHROME_DRIVER_SILENT_OUTPUT_PROPERTY, "true");
		System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "/dev/null");

		ChromeOptions options = new ChromeOptions();
//		options.setHeadless(true);
//		options.addArguments("--start-maximized");
//		options.setExperimentalOption("excludeSwitches", new String [] {"enable-automation"});
//		options.setExperimentalOption("useAutomationExtension", false);
//		options.addArguments("--user-agent=\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.53 Safari/537.36\"");
		this.browser = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
//		((JavascriptExecutor)browser).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

		retrieveData(url);
		browser.close();
		closeContainer();
		MangaDL.logger.finest("EXTRACTING finished\n");
	}

	Extractor(File mangaDirectory) throws Exception {
		Extractor.running = false;
		this.mangaDirectory = mangaDirectory;
		if (!mangaDirectory.exists()) {
			throw new ParameterException(MangaDL.cli, "Manga folder does not exist!");
		}
		File xmlFile = new File(mangaDirectory, "manga.xml");
		readData(xmlFile);
		MangaDL.logger.finest("EXTRACTING finished\n");
	}

	private void selectExtractor(String url) throws Exception {
		if (url == null) {
			throw new ParameterException(MangaDL.cli, "Missing URL");
		}
		if (url.toLowerCase().contains("kissmanga")) {
			this.source = new Kissmanga();
		} else if (url.toLowerCase().contains("mangadex")) {
			this.source = new MangaDex();
		} else if (url.toLowerCase().contains("mangarawr")) {
			this.source = new MangaRawr();
		} else if (url.toLowerCase().contains("mangafast")) {
			this.source = new Mangafast();
//		}else if(url.toLowerCase().contains("mangafreak")){
//			this.source = new Mangafreak();
		} else if (url.toLowerCase().contains("nettruyen")) {
			this.source = new NetTruyen();
		} else {
			throw new ParameterException(MangaDL.cli, "Unsupported url");
		}
	}

	private void retrieveData(String url) throws Exception {
		source.setHeader(browser);
		getPageSource(url);
		try {
			this.title = source.retrieveTitle(page);
		} catch (Exception e) {
			throw new ParameterException(MangaDL.cli, "Page not found");
		}
		this.mangaDirectory = new File(title);
		File xmlFile = new File(mangaDirectory, "manga.xml");
		if (xmlFile.exists()) {
			readData(xmlFile);
		} else {
			this.chapters = new ArrayList<>();
			System.out.println(title);
		}
		MangaDL.logger.info("Retrieving info of manga " + title);
		this.cover = "000-000";
		this.authors = source.retrieveAuthors(page);

		source.getChapterList(browser);
		List<String> chaptersURLs = new ArrayList<>();
		List<String> chaptersNames = new ArrayList<>();
		List<String> chaptersPages = source.retrieveChapterPages(page);
		for (String pageURL : chaptersPages) {
			if (chaptersPages.indexOf(pageURL) > 0) {
				getPageSource(pageURL);
			}
			chaptersURLs.addAll(0, source.retrieveChaptersURLs(page));
			chaptersNames.addAll(0, source.retrieveChaptersNames(page));
		}
		this.remainingChapterURLs = chaptersURLs;
		if (resumingIndex < remainingChapterURLs.size()) {
			this.remainingChapterURLs = remainingChapterURLs.subList(resumingIndex, remainingChapterURLs.size());
			if (resumingIndex != 0) {
				System.out.println("New chapters found: " + (remainingChapterURLs.size()));
				MangaDL.logger.info("New chapters found: " + (remainingChapterURLs.size()));
			}
			setChaptersNames(chaptersNames);
			collectChaptersImages();
			writeData();
		}
	}

	private void getPageSource(String url) {
		MangaDL.logger.info("Opening page " + url);
		browser.get(url);
		source.waitLoading(browser);
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
		this.chapters = ext.chapters;
		// new chapter added to the site ?
		this.resumingIndex = chapters.size();
	}

	private void writeData() throws Exception {
		MangaDL.logger.info("Saving manga.xml");
		JAXBContext context = JAXBContext.newInstance(Extractor.class);
		Marshaller mar = context.createMarshaller();
		mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		//write to file
		if (!mangaDirectory.exists()) {
			if (!mangaDirectory.mkdir()) {
				throw new IOException("Cannot save manga.xml file!");
			}
		}
		mar.marshal(this, new File(mangaDirectory, "manga.xml"));
	}

	private void collectChaptersImages() throws Exception {
		Iterable<String> remainingChapterURLs = this.remainingChapterURLs;
		if (!MangaDL.verbose) {
			remainingChapterURLs = ProgressBar.wrap(remainingChapterURLs, "Extracting");
		}
		int index = 0;
		for (String chapterURL : remainingChapterURLs) {
			getPageSource(chapterURL);
			setChapterPNGs(resumingIndex + index, source.retrieveChapterPNGs(page,browser));
			index++;
		}
	}

	public String getBaseURL() {
		return source.baseURL();
	}

	public String getCover() {
		return cover;
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
		int index = 0;
		for (String chaptersName : chaptersNames) {
			if (chapters.size() <= index) {
				chapters.add(new Chapter(chaptersName));
			}
			index++;
		}
	}

	protected void setChapterPNGs(int index, List<String> chapterPNGs) {
		chapters.get(index).images = chapterPNGs;
	}

	protected void startContainer() throws Exception {
		DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("unix:///var/run/docker.sock")
				.build();
		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
				.dockerHost(dockerConfig.getDockerHost())
				.build();
		Extractor.dockerClient = DockerClientImpl.getInstance(dockerConfig, httpClient);

		final String imageName = "selenium/standalone-chrome:87.0";
		PullImageResultCallback pulledImg = new PullImageResultCallback();
		List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec();
		boolean imageNotFound = true;
		for (Image image : images) {
			String[] repoTags = image.getRepoTags();
			if (repoTags != null && repoTags.length > 0 && repoTags[0].equals(imageName)) {
				imageNotFound = false;
				break;
			}
		}
		if (imageNotFound) {
			System.out.println("Pulling Docker image " + imageName);
			MangaDL.logger.info("Pulling image " + imageName);
			dockerClient.pullImageCmd(imageName).exec(pulledImg);
			pulledImg.awaitCompletion();
		}
		Ports portBindings = new Ports();
		portBindings.bind(ExposedPort.tcp(4444), Ports.Binding.bindPort(4444));
		portBindings.bind(ExposedPort.tcp(5900), Ports.Binding.bindPort(5900));
		HostConfig hostConfig = new HostConfig().withShmSize(2 * 1024 * 1024 * (long) 1024).withPortBindings(portBindings);
		CreateContainerResponse container = dockerClient.createContainerCmd(imageName).withHostConfig(hostConfig).exec();
		Extractor.containerId = container.getId();
		dockerClient.startContainerCmd(containerId).exec();
		Thread.sleep(6000);
	}

	protected static void closeContainer() throws Exception {
		if (running) {
			MangaDL.logger.info("Closing container " + containerId);
			dockerClient.stopContainerCmd(containerId).exec();
			dockerClient.removeContainerCmd(containerId).exec();
			dockerClient.close();
			Extractor.running = false;
		}
	}
}
