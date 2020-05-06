import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

public class kissmangadlTest {

    // @Test
    public void testCLI() {
        Option downloadOption = Option.builder("d").longOpt("download").desc("Download manga in PNG format").build();
        Option packOption = Option.builder("p").longOpt("pack").desc("Pack all PNGs into an EPUB file").build();
        Options cliOptions = new Options();
        cliOptions.addOption(downloadOption);
        cliOptions.addOption(packOption);

        CommandLineParser cliParser = new DefaultParser();
        // try{
        // CommandLine command = cliParser.parse(cliOptions, args);
        // }
    }
}