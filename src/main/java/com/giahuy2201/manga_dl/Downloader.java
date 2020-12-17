package com.giahuy2201.manga_dl;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Downloader class to work with Selenium container
 */
public class Downloader {
    private int resumingIndex;
    private final List<List<String>> remainingChaptersPNGs;

    public Downloader() {
        readDownloadedChapterIndex();
        List<List<String>> chaptersPNGs = MangaDL.extractor.getChaptersPNGs();
        if (resumingIndex == 0) {
            this.remainingChaptersPNGs = chaptersPNGs;
        } else {
            this.remainingChaptersPNGs = chaptersPNGs.subList(resumingIndex, chaptersPNGs.size());
            System.out.println("Resuming at " + MangaDL.extractor.getChaptersNames().get(resumingIndex));
        }
    }

    /**
     * Download
     *
     * @throws IOException
     */
    public void download() throws IOException {
        Iterable<List<String>> chaptersPNGs = ProgressBar.wrap(remainingChaptersPNGs, "Downloading");
        int chapterIndex = 0, pngIndex = 0;
        for (List<String> chapterPNGs : chaptersPNGs) {
            for (String chapterPNG : chapterPNGs) {
                MangaDL.logger.info("Retrieving: " + chapterPNG);

                // todo infer correct extension, instead of hardcoding png
                String frameFileName = formatIndex(chapterIndex++) + "-" + formatIndex(pngIndex++) + ".png";
                File outputFile = new File(MangaDL.extractor.getMangaDirectory(), frameFileName);
                // Try 3 times if fails
                long attempts = 0;
                boolean failed = true;
                do {
                    try {
                        FileUtils.copyURLToFile(new URL(chapterPNG), outputFile);
                        failed = false;
                        MangaDL.logger.info("Retrieved successfully!");
                        break;
                    } catch (IOException e) {
                        MangaDL.logger.log(Level.WARNING, "Failed to retrieve: " + chapterPNG);
                        MangaDL.logger.info("Trying a " + ++attempts + "th time ...");
                    }
                } while (attempts < 3);

                if (failed) {
                    MangaDL.logger.severe("Cannot retrieve " + chapterPNG);
                    throw new IOException("Cannot retrieve " + chapterPNG);
                }
            }
        }
        MangaDL.logger.info("Downloading finished");
    }

    /**
     * Find the latest chapter number to resume
     */
    private void readDownloadedChapterIndex() {
        File[] mangaFrames = MangaDL.extractor.getMangaDirectory().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().matches("\\d{3}-\\d{3}.png$");
            }
        });
        if (mangaFrames == null || mangaFrames.length == 0) {
            this.resumingIndex = 0;
        } else {
            File latestFile = Collections.max(Arrays.asList(mangaFrames));
            this.resumingIndex = Integer.parseInt(latestFile.getName().substring(0, 3));
        }
    }

    /**
     * Format index number into ### for better EPUB bundling. Eg. 1 -> 001
     *
     * @param count decimal number
     */
    private String formatIndex(int count) {
        final int LENGTH = 3;
        String index = count + "";
        int countLength = index.length();
        for (int i = 0; i < LENGTH - countLength; i++) {
            index = "0" + index;
        }
        return index;
    }

}
