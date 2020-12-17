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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Extractor {

    private WebDriver browser;
    private Document page;
    private String cover;
    private File mangaDirectory;
    private String title;
    private String authors;
    private List<String> chaptersNames;
    private List<String> chaptersURLs;
    private List<List<String>> chaptersPNGs;

    Extractor(String url) throws Exception {
        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, "chromedriver");
        System.setProperty(ChromeDriverService.CHROME_DRIVER_SILENT_OUTPUT_PROPERTY, "true");
        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "/dev/null");

        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        this.browser = new ChromeDriver(options);

        // Retrieve data
        getPage(url);
        retrieveTitle();
        this.mangaDirectory = new File(title);
        File xmlFile = new File(mangaDirectory, "manga.xml");
        if (xmlFile.exists()) {
            readData(xmlFile);
        } else {
            this.chaptersPNGs = new ArrayList<>();
            retrieveData(url);
            writeData();
        }
        browser.close();
    }

    Extractor(File mangaDirectory) throws Exception {
        this.mangaDirectory = mangaDirectory;
        if (!mangaDirectory.exists()) {
            throw new IOException("Manga folder does not exist!");
        }
        File xmlFile = new File(mangaDirectory, "manga.xml");
        readData(xmlFile);
    }

    private void retrieveData(String url) {
        this.cover = "000-000.png";
        retrieveTitle();
        retrieveAuthors();
        retrieveChaptersNames();
        retrieveChaptersURLs();
        Iterable<String> chaptersURLs = ProgressBar.wrap(this.chaptersURLs,"Extracting");
            System.out.println("***");
        for (String chapterUrl : chaptersURLs) {
            getPage(chapterUrl);
//            try{
//                FileWriter f= new FileWriter(title+".txt",true);
//                f.write(chapterUrl+"\n");
//                f.close();
//            }catch(Exception e){}
            retrieveChaptersPNGs();
        }
    }

    private void getPage(String url) {
        browser.get(url);
        WebDriverWait wait = new WebDriverWait(browser, 1, 50);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("navbar")));
        this.page = Jsoup.parse(browser.getPageSource());
    }

    private void readData(File xmlFile) throws Exception {
        DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document mangaProfile = xmlBuilder.parse(xmlFile);
        mangaProfile.getDocumentElement().normalize();

        this.title = mangaProfile.getElementsByTagName("title").item(0).getTextContent();
        this.authors = mangaProfile.getElementsByTagName("authors").item(0).getTextContent();
        this.cover = mangaProfile.getElementsByTagName("cover").item(0).getTextContent();
        this.chaptersNames = new ArrayList<>();
        this.chaptersPNGs = new ArrayList<>();
        NodeList chapters = mangaProfile.getElementsByTagName("chapter");
        for (int i = 0; i < chapters.getLength(); i++) {
            org.w3c.dom.Document chapter = chapters.item(i).getOwnerDocument();
            String chapterName = chapter.getElementsByTagName("name").item(0).getTextContent();
            chaptersNames.add(chapterName);

            List<String> chapterPNGs = new ArrayList<>();
            NodeList PNGs = chapter.getElementsByTagName("img");
            for (int j = 0; j < PNGs.getLength(); j++) {
                String png = PNGs.item(i).getTextContent();
                chapterPNGs.add(png);
            }
            chaptersPNGs.add(chapterPNGs);
        }
    }

    private void writeData() throws Exception {
        DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document mangaProfile = xmlBuilder.newDocument();

        Element mangaElement = mangaProfile.createElement("manga");
        Element titleElement = mangaProfile.createElement("title");
        titleElement.setTextContent(title);
        mangaElement.appendChild(titleElement);
        Element authorsElement = mangaProfile.createElement("authors");
        authorsElement.setTextContent(authors);
        mangaElement.appendChild(authorsElement);
        Element coverElement = mangaProfile.createElement("cover");
        coverElement.setTextContent(cover);
        mangaElement.appendChild(coverElement);
        Element chaptersElement = mangaProfile.createElement("chapters");

        for (int i = 0; i < chaptersPNGs.size(); i++) {
            List<String> chapterPNGs = chaptersPNGs.get(i);
            Element chapterElement = mangaProfile.createElement("chapter");
            Element nameElement = mangaProfile.createElement("name");
            nameElement.setTextContent(chaptersNames.get(i));
            chapterElement.appendChild(nameElement);

            Element imgsElement = mangaProfile.createElement("imgs");
            for (int j = 0; j < chapterPNGs.size(); j++) {
                Element imgElement = mangaProfile.createElement("img");
                imgElement.setTextContent(chapterPNGs.get(j));
                imgsElement.appendChild(imgElement);
            }
            chapterElement.appendChild(imgsElement);
            chaptersElement.appendChild(chapterElement);
        }
        mangaElement.appendChild(chaptersElement);

        mangaProfile.appendChild(mangaElement);

        // Save xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        //for pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(mangaProfile);

        //write to file
        if (!mangaDirectory.exists() && mangaDirectory.mkdir()) {
            StreamResult xmlFile = new StreamResult(new File(mangaDirectory ,"manga.xml"));
            transformer.transform(source, xmlFile);
        } else {
            throw new IOException("Cannot save manga.xml file!");
        }
    }

    static boolean valid(String url) {
        return false;
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
        return chaptersNames;
    }

    public List<String> getChaptersURLs() {
        return chaptersURLs;
    }

    public List<List<String>> getChaptersPNGs() {
        return chaptersPNGs;
    }

    protected void setTitle(String title) {
        this.title = escape(title);
    }

    protected void setAuthors(String authors) {
        this.authors = escape(authors);
    }

    protected void setChaptersNames(List<String> chaptersNames) {
        for (int i = 0; i < chaptersNames.size(); i++) {
            chaptersNames.set(i, escape(chaptersNames.get(i)));
        }
        this.chaptersNames = chaptersNames;
    }

    protected void setChaptersURLs(List<String> chaptersURLs) {
        this.chaptersURLs = chaptersURLs;
    }

    protected void setChaptersPNGs(List<List<String>> chaptersPNGs) {
        this.chaptersPNGs = chaptersPNGs;
    }

    abstract String escape(String str);

    abstract void retrieveTitle();

    abstract void retrieveAuthors();

    abstract void retrieveChaptersNames();

    abstract void retrieveChaptersURLs();

    abstract void retrieveChaptersPNGs();
}
