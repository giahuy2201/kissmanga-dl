import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class DownloaderTest {
    // @Test
    public void testLoging() {
        Logger logger = Logger.getLogger(DownloaderTest.class.getName());
        logger.warning("Not in file!");
        try {
            // FileHandler cant create directory itself
            new File("logs/").mkdir();
            File op = new File("logs/");
            System.out.println(op.getPath());
            FileHandler fh = new FileHandler(op.getPath() + "/abc.log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.info("Start");
            TimeUnit.SECONDS.sleep(6);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.severe("Hahahah");
        assertTrue(new File("logs/abc.log").exists());
    }

    // @Test
    public void testResourceLoader() {
        String[] resourceNames = { "chapter.html", "manga.xml", "page_styles.css", "stylesheet.css" };
        ClassLoader loader = getClass().getClassLoader();
        // Class loader = getClass();
        for (String name : resourceNames) {
            // assertTrue(new File(loader.getResource(name).getFile()).exists());
            System.out.println(loader.getResource(name).getFile());
            System.out.println(new File(loader.getResource(name).getFile()).exists());
        }
    }
}