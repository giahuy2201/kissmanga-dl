package com.giahuy2201.manga_dl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.IOException;
import java.util.logging.Level;


public class GeneralTest {

//    @Test
    public void testChromeDriver() {
        System.setProperty("webdriver.gecko.driver", "/Users/giahuy/Downloads/geckodriver");
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        WebDriver drv = new FirefoxDriver(options);
        drv.get("https://kissmanga.org/manga/kxqh9261558062112");
//        Extractor kex = new KissmangaExtractor();
//        System.out.println(kex.getTitle());
        drv.close();
    }

    @Test
    public void testPDFbox() throws IOException {
    	final String DEST = "test.pdf";
        try {
            PDDocument pdDoc = new PDDocument();
            PDImageXObject image = PDImageXObject.createFromFile("1.png", pdDoc);
            int w = image.getWidth();
            int h = image.getHeight();
            PDRectangle pdr = new PDRectangle(w,h);
            PDPage page = new PDPage(pdr);
            // add page to the document
            pdDoc.addPage(page);
            // Create image object using the image location
            // write to a page content stream
            try(PDPageContentStream cs = new PDPageContentStream(pdDoc, page)){;
                cs.drawImage(image, 0, 0, w,h);
            }
            // 2nd page
            PDImageXObject img2 = PDImageXObject.createFromFile("2.png", pdDoc);
            int w2 = img2.getWidth();
            int h2 = img2.getHeight();
            PDRectangle pdr2 = new PDRectangle(w2,h2);
            PDPage p2 = new PDPage(pdr2);
            // add p2 to the document
            pdDoc.addPage(p2);
            // Create image object using the image location
            // write to a p2 content stream
            try(PDPageContentStream cs = new PDPageContentStream(pdDoc, p2)){
                cs.drawImage(img2, 0, 0, w2,h2);
            }
            // save and close PDF document
            pdDoc.save(DEST);
            pdDoc.close();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testPDFboxTOC(){
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 10; i++) {
                document.addPage(new PDPage());
            }

            PDDocumentOutline documentOutline = new PDDocumentOutline();
            document.getDocumentCatalog().setDocumentOutline(documentOutline);

            PDOutlineItem pagesOutline = new PDOutlineItem();
            pagesOutline.setTitle("All Pages");
            documentOutline.addLast(pagesOutline);

            for(int i = 0; i < document.getNumberOfPages(); i++) {
                PDPageDestination pageDestination = new PDPageFitWidthDestination();
                pageDestination.setPage(document.getPage(i));

                PDOutlineItem bookmark = new PDOutlineItem();
                bookmark.setDestination(pageDestination);
                bookmark.setTitle("Document Page " + (i + 1));
                pagesOutline.addLast(bookmark);
            }

            pagesOutline.openNode();
            documentOutline.openNode();

            document.getDocumentCatalog().setPageMode(PageMode.USE_OUTLINES);

            document.save("x.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
