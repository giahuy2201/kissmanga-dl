package com.giahuy2201.manga_dl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;

/**
 * Class to create EPUB file from PNGs
 */

public class EpubBundler {
    // manga folder contains file manga.xml and images files
    private File mangaDirectory;
    private File outputDirectory;
    private Logger logger;
    private Document mangaProfile;
    private ClassLoader resourceLoader;

    public EpubBundler(File outputDirectory, Logger logger) {
        resourceLoader = getClass().getClassLoader();
        this.outputDirectory = outputDirectory;
        this.logger = logger;
    }

    /**
     * Create Epub book file from images and metadata in manga.xml file in
     * mangaDirectory
     * 
     * @param mangaDirectory  directory holding manga.xml profile file and images
     * @throws IOException
     */
    public void create(File mangaDirectory) throws IOException {
        // validate directory
        if (!new File(mangaDirectory, "manga.xml").exists()) {
            return;
        }
        this.mangaDirectory = mangaDirectory;
        // read manga profile
        importProfile();
        String title = mangaProfile.getElementsByTagName("title").item(0).getTextContent();
        String language = mangaProfile.getElementsByTagName("lang").item(0).getTextContent();
        String cover = mangaProfile.getElementsByTagName("cover").item(0).getTextContent();
        // prioritize cover.png in mangaDirectory
        if (new File(mangaDirectory, "cover.png").exists()) {
            cover = "cover.png";
        }
        List<Author> authors = new ArrayList<>();
        NodeList authorList = mangaProfile.getElementsByTagName("author");
        for (int i = 0; i < authorList.getLength(); i++) {
            String authorName = authorList.item(0).getTextContent();
            authors.add(new Author(authorName));
        }
        // create book
        Book book = new Book();
        // Set the title
        book.getMetadata().addTitle(title);
        // Set the language
        book.getMetadata().setLanguage(language);
        // Set authors
        book.getMetadata().setAuthors(authors);
        // Set cover image
        book.setCoverImage(new Resource(new FileInputStream(new File(mangaDirectory, cover)), "cover.png"));
        // Add css file
        book.addResource(new Resource(resourceLoader.getResourceAsStream("stylesheet.css"), "stylesheet.css"));
        // Add images
        List<List<File>> chapterList = listImages(cover);
        for (List<File> chapter : chapterList) {
            for (File image : chapter) {
                logger.info(image.getAbsolutePath());
                book.addResource(new Resource(new FileInputStream(image), "images/" + image.getName()));
            }
        }
        // Add chapters
        for (int index = 0; index < chapterList.size(); index++) {
            String chapterName = "Chapter " + (index + 1);
            logger.info("Adding " + chapterName);
            InputStream chapterHTML = toInputStream(buildChapter(chapterList.get(index)));
            book.addSection(chapterName, new Resource(chapterHTML, chapterName + ".html"));
        }
        // Create EpubWriter
        EpubWriter epubWriter = new EpubWriter();

        // Write the Book as Epub
        logger.info("Writing to file " + title + ".epub");
        epubWriter.write(book, new FileOutputStream(new File(outputDirectory, title + ".epub")));
        logger.info("Packing finished");
    }

    public String getTitle(File mangaDirectory) {
        // validate directory
        if (!new File(mangaDirectory, "manga.xml").exists()) {
            return null;
        }
        this.mangaDirectory = mangaDirectory;
        importProfile();
        return mangaProfile.getElementsByTagName("title").item(0).getTextContent();
    }

    /**
     * Read manga.xml and parse XML document
     */
    private void importProfile() {
        if (mangaProfile != null) {
            return;
        }
        try {
            logger.info("Importing metadata from " + mangaDirectory + "/manga.xml");
            File mangaFile = new File(mangaDirectory, "manga.xml");
            DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            mangaProfile = xmlBuilder.parse(mangaFile);
            mangaProfile.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Construct a html file with chapter images
     * 
     * @param imageList
     * @return
     */
    public String buildChapter(List<File> imageList) {
        StringBuilder content = new StringBuilder();
        for (File image : imageList) {
            content.append(String.format("<p class=\"image-wrapper\"><img src=\"images/%s\" class=\"image\"/></p>\n",
                    image.getName()));
        }
        try {
            String baseHTML = IOUtils.toString(resourceLoader.getResourceAsStream("chapter.html"), "UTF-8");
            baseHTML = baseHTML.replace("$CONTENT", content.toString());
            return baseHTML;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sort images into a list of chapter
     * 
     * @return
     */
    private List<List<File>> listImages(String ignoredImage) {
        // collect files
        File[] files = mangaDirectory.listFiles((dir, name) -> {
            return !name.toLowerCase().equals(ignoredImage) && name.toLowerCase().endsWith(".png");
        });
        // list of chapters
        List<List<File>> lists = new ArrayList<>();
        for (File file : files) {
            // chapter number
            int groupIndex = Integer.parseInt(file.getName().substring(0, 3));
            if (groupIndex > lists.size() - 1) {
                // fill in non-existing lists
                for (int i = lists.size(); i <= groupIndex; i++) {
                    lists.add(new ArrayList<>());
                }
            }
            lists.get(groupIndex).add(file);
        }
        for (List<File> list : lists) {
            Collections.sort(list, (o1, o2) -> o1.toString().compareTo(o2.toString()));
        }
        return lists;
    }

    private InputStream toInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

}