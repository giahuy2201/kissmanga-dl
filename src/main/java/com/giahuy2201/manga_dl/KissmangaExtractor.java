package com.giahuy2201.manga_dl;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extract some basic info for metadata
 */
public class KissmangaExtractor extends Extractor {

    final String BASE_URL = "https://kissmanga.org";

    public KissmangaExtractor(String url) throws Exception {
        super(url);
    }

    public KissmangaExtractor(File dir) throws Exception {
        super(dir);
    }

    protected static boolean valid(String url) {
        return Pattern.matches("https?://kissmanga\\.org/manga/[^/]+/?", url);
    }

    protected String escape(String str) {
        return str.replace("/", "|").replace("-", "").replace(".", ":").trim();
    }

    protected void retrieveTitle() {
        setTitle(getPage().select("h2 strong.bigChar").first().text());
    }

    protected void retrieveAuthors() {
        setAuthors(getPage().select("p.info:nth-child(3) a.dotUnder").first().text());
    }

    protected void retrieveChaptersNames() {
        Elements elements = getPage().select(".listing h3 a[href]");
        List<String> chaptersNames = new ArrayList<>();
        for (Element element : elements) {
            String name = element.text();
            chaptersNames.add(escape(escape(name).replace(getTitle(), "")));
        }
        Collections.reverse(chaptersNames);
        setChaptersNames(chaptersNames);
    }

    protected void retrieveChaptersURLs() {
        Elements elements = getPage().select(".listing h3 a[href]");
        List<String> chaptersURLs = new ArrayList<>();
//        try {
//            FileWriter f = new FileWriter(getTitle() + ".txt");
        for (Element element : elements) {
            chaptersURLs.add(BASE_URL + element.attr("href"));
//            f.write(element.attr("href")+"\n");
        }
        Collections.reverse(chaptersURLs);
        setChaptersURLs(chaptersURLs);
//        f.close();
//    } catch (Exception e) {
//    }
    }

    protected void retrieveChaptersPNGs() {
        Elements elements = getPage().select("div#centerDivVideo img");
        List<List<String>> chaptersPNGs = this.getChaptersPNGs();
        List<String> chapterPNGs = new ArrayList<>();
        for (Element element : elements) {
            chapterPNGs.add(element.attr("src"));
        }
        chaptersPNGs.add(chapterPNGs);
        setChaptersPNGs(chaptersPNGs);
    }
}