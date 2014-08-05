package safbuilder;

import org.apache.commons.cli.*;

import java.io.IOException;

public class BatchProcess {

    public static void main(String[] args) throws IOException, ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("c", "csv", true, "Filename with path of the CSV spreadsheet. This must be in the same directory as the content files");
        options.addOption("h", "help", false, "Display the Help");
        options.addOption("z", "zip", false, "(optional) ZIP the output");

        CommandLine commandLine = parser.parse(options, args);

        if(commandLine.hasOption('h') || !commandLine.hasOption('c')) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("SAFBuilder\n", options);
            System.exit(0);
        }

        if(commandLine.hasOption('c')) {
            SAFPackage safPackageInstance = new SAFPackage();
            safPackageInstance.processMetaPack(commandLine.getOptionValue('c'));
        }
    }
}
