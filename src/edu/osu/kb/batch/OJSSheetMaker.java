package edu.osu.kb.batch;

import com.csvreader.CsvReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: peterdietz
 * Date: 11/21/11
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class OJSSheetMaker {
    private static File input;
    private static CsvReader inputCSV;

    public static void main(String[] args) {
        if(args.length < 2) {
            System.err.println("USAGE: OJSSheetMaker inputDirectory metadataFile");
            System.exit(-1);
        }

        String inputDir = args[0];
        String metaFile = args[1];

        OJSSheetMaker maker = new OJSSheetMaker();
        try {
            maker.processMetaSheet(inputDir, metaFile);
        } catch (IOException e) {
            System.err.println(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public void processMetaSheet(String inputDir, String metafile) throws IOException {
        openCSV(inputDir, metafile);

        inputCSV.readHeaders();

        while(inputCSV.readRecord()) {
            processBodyRow();
        }
    }

    public void processBodyRow() throws IOException {
        String[] currentLine = inputCSV.getValues();
        //Peter's
        // 0 issue_id, 1 section_name, 2 xml-body, 3 arbitrary number, 4 journal path, 5 xmlFileName, 6 command


        //Henry's
        // 0 xmlBody, 1 filename, 2 command
        final int columnXmlBody = 4;
        final int columnXmlFileName = 5;
        final int columnCommand = 6;

        String xmlBody = currentLine[columnXmlBody];
        String fileName = currentLine[columnXmlFileName];
        String command = currentLine[columnCommand];

        File xmlFile = new File(input.getAbsolutePath() + "/" + fileName);
        BufferedWriter xmlWriter = new BufferedWriter(new FileWriter(xmlFile));
        xmlWriter.append(xmlBody);
        xmlWriter.close();

        //Give us output such that when we run these commands in batch, it will echo the command, and execute the command.
        System.out.println("echo "+command);
        System.out.println(command);
    }

    /**
     * Gets a "handle" on the metadata file
     * @param inputDir Path to input directory.
     * @param metaFile Filename of the CSV
     */
    protected void openCSV(String inputDir, String metaFile)
    {
        input = new File(inputDir);

        if(!inputDir.endsWith("\\/")) {
            inputDir = inputDir + "/";
        }
        String absoluteFileName = inputDir + metaFile;
        try {
            inputCSV = new CsvReader(absoluteFileName);
        } catch (Exception e) {
            System.out.println(input.getAbsolutePath());
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Opened CSV File:" + absoluteFileName);
    }
}
