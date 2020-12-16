
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extract some basic info for metadata
 */
public class KissmangaExtractor {

    final String BASE_URL = "https://kissmanga.org";
    private Document page;
    private String title;
    private String authors;
    private List<String> chapterNames;
    private List<String> chapterUrls;

    public KissmangaExtractor(Document pageSource) {
        page = pageSource;
    }

    public String getTitle() {
        if(title!=null){
            return title;
        }
        title = page.select("h2 strong.bigChar").first().text().replace("/","|").trim();
        return title;
    }

    public String getAuthors() {
        if(authors!=null){
            return authors;
        }
        authors = page.select("p.info:nth-child(3) a.dotUnder").first().text().trim();
        return authors;
    }

    public List<String> getChapterUrls() {
        Elements elements = page.select(".listing h3 a[href]");
        chapterUrls = new ArrayList<>();
        for (Element element : elements) {
            chapterUrls.add(BASE_URL + element.attr("href"));
        }
        Collections.reverse(chapterUrls);
        return chapterUrls;
    }

    public List<String> getChapterNames() {
        Elements elements = page.select(".listing h3 a[href]");
        chapterNames = new ArrayList<>();
        if (title == null) {
            getTitle();
        }
        for (Element element : elements) {
            String name = element.text().replace(title, "").replace("-", " ").trim();
            chapterNames.add(name);
        }
        Collections.reverse(chapterNames);
        return chapterNames;
    }
}