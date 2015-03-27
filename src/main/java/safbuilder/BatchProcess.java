package safbuilder;

import org.apache.commons.cli.*;

import java.io.IOException;

public class BatchProcess {

    public static void main(String[] args) throws IOException, ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("c", "csv", true, "Filename with path of the CSV spreadsheet. This must be in the same directory as the content files");
        options.addOption("h", "help", false, "Display the Help");
        options.addOption("m", "manifest", false, "Initialize a spreadsheet, a manifest listing all of the files in the directory, you must specify a CSV for -c ");
        options.addOption("s", "symbolicLink", false, "Set a Symbolic Link for bitstreams (instead of copying them)");
        options.addOption("z", "zip", false, "(optional) ZIP the output");

        CommandLine commandLine = parser.parse(options, args);

        if(commandLine.hasOption('h') || !commandLine.hasOption('c')) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("SAFBuilder\n", options);
            System.exit(0);
        }

        SAFPackage safPackageInstance = new SAFPackage();

        if(commandLine.hasOption('m') && commandLine.hasOption('c')) {
            safPackageInstance.generateManifest(commandLine.getOptionValue('c'));
            System.exit(0);
        }

        if(commandLine.hasOption('c')) {
            if(commandLine.hasOption('s')){
                safPackageInstance.setSymbolicLink(true);
            }
            safPackageInstance.processMetaPack(commandLine.getOptionValue('c'), commandLine.hasOption('z'));
        }
    }
}
