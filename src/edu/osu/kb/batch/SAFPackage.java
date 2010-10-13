package edu.osu.kb.batch;

import org.apache.commons.io.FileUtils;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SAFPackage
{

    /**
     * Directory of this input collection.
     */
    private File input;
    /**
     * The csv file with the metadata. Assumed that the first row has field names, including filename, and dc.element.qualifier, dc....
     */
    private InputCSV icsv;
    private static ArrayList header = new ArrayList();
    private static List parsedCSV;

    /**
     * Default constructor. Main method of this class is processMetaPack. The goal of this is to create a Simple Archive Format
     * package from input of files and csv metadata.
     */
    public SAFPackage()
    {
    }

    /**
     * Gets a "handle" on the metadata file
     * @param inputDir
     * @param metaFile
     */
    private void openCSV(String inputDir, String metaFile)
    {

        input = new File(inputDir);
        String absoluteFileName = inputDir + "/" + metaFile;
        try {
            icsv = new InputCSV(absoluteFileName);
        } catch (Exception e) {
            System.out.println(input.getAbsolutePath());
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Opened CSV File:" + absoluteFileName);
        parsedCSV = icsv.parse();
    }

    /**
     * open metafile
     * foreach(metarows as metarow)
     *      makeDirectory(increment)
     *      copy filenames into directory
     *      make contents file with entries for each filename
     *      foreach(metarow.columns as column)
     *          add meta entry to metadata xml
     * @param pathToDirectory
     * @param metaFileName
     */
    public void processMetaPack(String pathToDirectory, String metaFileName)
    {
        openCSV(pathToDirectory, metaFileName);

        scanAllFiles();                                                         // For Reporting file usage

        prepareSimpleArchiveFormatDir();

        processMetaHeader();
        processMetaBody();

        printFiles(0);                                                          // print a report of files not used
    }

    /**
     * Creates a clean/empty SimpleArchiveFormat directory for the output to go.
     */
    private void prepareSimpleArchiveFormatDir()
    {

        File newDirectory = new File(input.getPath() + "/SimpleArchiveFormat");
        if (newDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(newDirectory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        newDirectory.mkdir();
    }
    private static String[][] fileListFake;

    private void scanAllFiles()
    {
        String[] files = input.list();

        fileListFake = new String[files.length][2];
        for (int i = 0; i < files.length; i++) {
            fileListFake[i][0] = files[i];
            fileListFake[i][1] = "0";
        }
    }

    private void incrementFileHit(String filename)
    {
        int i = 0;
        boolean found = false;
        while (!found && (i < fileListFake.length)) {
            if (fileListFake[i][0].contentEquals(filename)) {
                int current = Integer.parseInt(fileListFake[i][1]);
                int increment = current + 1;
                fileListFake[i][1] = String.valueOf(increment);
                found = true;
            }

            i++;
        }
    }

    private void printFiles(Integer numHits)
    {
        for (int i = 0; i < fileListFake.length; i++) {
            if (fileListFake[i][1].contentEquals(numHits.toString())) {
                System.out.println("File: " + fileListFake[i][0] + " has been used " + numHits + " times.");
            }
        }
    }

    private void processMetaHeader()
    {
        String[] firstLine = (String[]) parsedCSV.get(0);
        for (int j = 0; j < firstLine.length; j++) {
            header.add(j, firstLine[j]);
        }
    }

    private String getHeaderField(int columnNum)
    {
        return header.get(columnNum).toString();
    }

    private void processMetaBody()
    {
        int contentRowStart = 1;
        for (int i = contentRowStart; i < parsedCSV.size(); i++) {
            processMetaBodyRow(i);
        }
    }

    private void processMetaBodyRow(int rowNumber)
    {
        String currentItemDirectory = makeNewDirectory(rowNumber);
        String dcFileName = currentItemDirectory + "/dublin_core.xml";
        File contentsFile = new File(currentItemDirectory + "/contents");

        try {
            BufferedWriter contentsWriter = new BufferedWriter(new FileWriter(contentsFile));

            String[] currentLine = (String[]) parsedCSV.get(rowNumber);

            OutputXML xmlWriter = new OutputXML(dcFileName);
            xmlWriter.start();

            for (int j = 0; j < header.size(); j++) {
                if (j >= currentLine.length) {
                    break;
                }
                if (currentLine[j].length() == 0) {
                    continue;
                }

                if (getHeaderField(j).contentEquals("filename")) {
                    processMetaBodyRowFile(contentsWriter, currentItemDirectory, currentLine[j]);
                } else if (getHeaderField(j).contains("filename-")) {
                    //This file is destined for a bundle
                    String[] filenameParts = getHeaderField(j).split("-");
                    String bundle = filenameParts[1];
                    processMetaBodyRowFile(contentsWriter, currentItemDirectory, currentLine[j], bundle);
                } else {
                    processMetaBodyRowField(getHeaderField(j), currentLine[j], xmlWriter);
                }
            }
            contentsWriter.close();
            xmlWriter.end();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the values for the specific piece of metadata to the output. Accepts
     * multiple values per value so long as they are separated by ;
     *
     * @param field_header Field name, such as dc.description or dc.description.abstract
     * @param field_value Metadata value or values. Multiple values can be separated by a ";"
     * @param xmlWriter
     */
    private void processMetaBodyRowField(String field_header, String field_value, OutputXML xmlWriter)
    {
        // process Metadata field. Multiple entries can be specified with seperator character
        String[] fieldValues = field_value.split(";");
        for (int valueNum = 0; valueNum < fieldValues.length; valueNum++) {
            if (fieldValues[valueNum].trim().length() > 0) {
                xmlWriter.writeOneDC(field_header, fieldValues[valueNum].trim());
            } else {
                continue;
            }
        }
        //TODO test that this works in both cases of single value and multiple value
    }

    /**
     * A file that doesn't need to go into a special bundle, it will go into Original bundle
     * @param contentsWriter
     * @param itemDirectory
     * @param filenames
     */
    private void processMetaBodyRowFile(BufferedWriter contentsWriter, String itemDirectory, String filenames)
    {
        processMetaBodyRowFile(contentsWriter, itemDirectory, filenames, "");
    }

    /**
     * open contents
     * for-each files as file
     *      copy file into directory
     *      add file to contents
     *
     * @param contentsWriter
     * @param itemDirectory
     * @param filenames
     * @param bundleName
     */
    private void processMetaBodyRowFile(BufferedWriter contentsWriter, String itemDirectory, String filenames, String bundleName)
    {
        String[] files = filenames.split(";");
        for (int j = 0; j < files.length; j++) {
            try {
                FileUtils.copyFileToDirectory(new File(input.getPath() + "/" + files[j]), new File(itemDirectory));
                incrementFileHit(files[j]);

                String contentsRow = files[j];
                if (bundleName.length() > 0) {
                    contentsRow = contentsRow.concat("\tbundle:" + bundleName);
                }
                contentsWriter.append(contentsRow);

                contentsWriter.newLine();
            } catch (FileNotFoundException fnf) {
                System.out.println("There is no file named " + files[j] + " in " + input.getPath() + " while making " + itemDirectory);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    /**
     * /path/to/input/SimpleArchiveFormat/item_27/
     * @param itemNumber
     * @return
     */
    private String makeNewDirectory(int itemNumber)
    {
        File newDirectory = new File(input.getPath() + "/SimpleArchiveFormat/item_" + itemNumber);
        newDirectory.mkdir();
        return newDirectory.getAbsolutePath();
    }
}
