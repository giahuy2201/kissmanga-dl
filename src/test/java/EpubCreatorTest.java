
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;

public class EpubCreatorTest {
    // @Test
    public void testEpubAddTitlePage() {
        try {
            File cv = new File("cover.png");
            if (cv.exists()) {
                cv.renameTo(new File("cover.pngx"));
            }
            EpubReader epr = new EpubReader();
            Book bk = epr.readEpub(new FileInputStream("1_2-Love.epub"));
            bk.setCoverImage(new Resource(new FileInputStream("cover.pngx"), "cover.png"));
            // write
            EpubWriter epw = new EpubWriter();
            epw.write(bk, new FileOutputStream("1_2-Love2.epub"));
            // rename it back
            cv = new File("cover.pngx");
            if (cv.exists()) {
                cv.renameTo(new File("cover.png"));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // @Test
    public void testEpubCreatingFromLib() {
        try {
            Book book = new Book();
            // Set the title
            book.getMetadata().addTitle("Kimi no Na wa.");
            String pre = "resources/";
            String img = "resources/images/";
            // Add an Author
            book.getMetadata().addAuthor(new Author("Huy", "Ng."));
            System.out.println(new File("").getAbsolutePath());
            // Set cover image
            book.setCoverImage(new Resource(new FileInputStream(img + "cover.png"), "cover.png"));

            // Add css file
            book.addResource(new Resource(new FileInputStream(pre + "page_styles.css"), "page_styles.css"));
            book.addResource(new Resource(new FileInputStream(pre + "stylesheet.css"), "stylesheet.css"));
            // Add images
            for (int i = 2; i <= 14; i++) {
                book.addResource(new Resource(new FileInputStream(img + i + ".png"), "images/" + i + ".png"));
            }
            // Add Chapter 1
            book.addSection("Chapter 1", new Resource(chapterHTML(2, 5), "chapter1.html"));
            // Add Chapter 2
            book.addSection("Chapter 2", new Resource(chapterHTML(6, 9), "chapter2.html"));
            // Add Chapter 3
            book.addSection("Chapter 3", new Resource(chapterHTML(10, 14), "chapter3.html"));

            // Create EpubWriter
            EpubWriter epubWriter = new EpubWriter();

            // Write the Book as Epub
            epubWriter.write(book, new FileOutputStream("out.epub"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InputStream chapterHTML(int start, int end) {
        String pre = "resources/";
        StringBuilder content = new StringBuilder();
        for (int i = start; i <= end; i++) {
            content.append(String.format(
                    "<p class=\"pdf-converter1\"><a id=\"%s\"></a><img src=\"images/%s\" class=\"pdf-converter2\"/></p>\n",
                    "id" + i, i + ".png"));
        }
        try {
            String index = IOUtils.toString(new FileInputStream(pre + "chapter.html"), "UTF-8");
            index = index.replace("$CONTENT", content.toString());
            return new ByteArrayInputStream(index.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void testEpubCreator() {
        File outputDirectory = new File("output/");
        // Bundle all manga folder found
        for (File folder : outputDirectory.listFiles()) {
            if (folder.isDirectory()) {
                String title = folder.getName();
                File mangaDirectory = new File(outputDirectory, title);
                try {
                    EpubCreator epubCreator = new EpubCreator();
                    epubCreator.create(mangaDirectory, outputDirectory);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                assumeTrue(new File(outputDirectory, title + ".epub").exists());
            }
        }
    }
}