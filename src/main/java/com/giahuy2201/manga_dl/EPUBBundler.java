package com.giahuy2201.manga_dl;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to create EPUB file from PNGs
 */

public class EPUBBundler {
    // manga folder contains file manga.xml and images files
    private ClassLoader resourceLoader;

    public EPUBBundler() {
        resourceLoader = getClass().getClassLoader();
    }

    /**
     * Create Epub book file from images and metadata in manga.xml file in
     * mangaDirectory
     *
     * @throws IOException
     */
    public void create() throws Exception {
        File mangaDirectory = MangaDL.extractor.getMangaDirectory();
        // create book
        Book book = new Book();
        // Set the metadata
        String title = MangaDL.extractor.getTitle();
        String cover = MangaDL.extractor.getCover();
        book.getMetadata().addTitle(title);
        List<Author> authors = new ArrayList<>();
        authors.add(new Author(MangaDL.extractor.getAuthors()));
        book.getMetadata().setAuthors(authors);
        // Set cover image
        book.setCoverImage(new Resource(new FileInputStream(new File(mangaDirectory, cover)), "cover.png"));
        // Add css file
        book.addResource(new Resource(resourceLoader.getResourceAsStream("stylesheet.css"), "stylesheet.css"));
        // Add images
        List<List<File>> chapterList = collectPNGs();
        for (List<File> chapter : chapterList) {
            for (File image : chapter) {
                book.addResource(new Resource(new FileInputStream(image), "images/" + image.getName()));
            }
        }
        // Add chapters
        for (int i = 0; i < chapterList.size(); i++) {
            String chapterName = MangaDL.extractor.getChaptersNames().get(i);
            InputStream chapterHTML = toInputStream(buildChapter(chapterList.get(i)));
            book.addSection(chapterName, new Resource(chapterHTML, chapterName + ".html"));
        }
        // Write the Book as Epub
        EpubWriter epubWriter = new EpubWriter();
        MangaDL.logger.info("Writing to file " + title + ".epub");
        epubWriter.write(book, new FileOutputStream(title + ".epub"));
        MangaDL.logger.info("Packing finished");
    }

    /**
     * Construct a html file with chapter images
     *
     * @param PNGs
     * @return
     */
    public String buildChapter(List<File> PNGs) throws IOException {
        StringBuilder content = new StringBuilder();
        for (File image : PNGs) {
            content.append(String.format("<p class=\"image-wrapper\"><img src=\"images/%s\" class=\"image\"/></p>\n",
                    image.getName()));
        }
        String baseHTML = IOUtils.toString(resourceLoader.getResourceAsStream("chapter.html"), "UTF-8");
        baseHTML = baseHTML.replace("$CONTENT", content.toString());
        return baseHTML;
    }

    /**
     * Sort images into a list of chapter
     *
     * @return
     */
    private List<List<File>> collectPNGs() throws  Exception{
        File[] files = MangaDL.extractor.getMangaDirectory().listFiles();
        // list of chapters
        List<List<File>> PNGs = new ArrayList<>();
        for (File file : files) {
            // Skip cover PNG
            if (file.getName().equals(MangaDL.extractor.getCover())) {
                continue;
            }
            int groupIndex = Integer.parseInt(file.getName().substring(0, 3));
            // Fill in new chapter lists
            for (int i = PNGs.size(); i <= groupIndex; i++) {
                PNGs.add(new ArrayList<>());
            }
            PNGs.get(groupIndex).add(file);
        }
        for (List<File> list : PNGs) {
            Collections.sort(list);
        }
        return PNGs;
    }

    private InputStream toInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

}