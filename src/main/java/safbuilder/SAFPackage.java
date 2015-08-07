package safbuilder;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mozilla.universalchardet.UniversalDetector;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SAFPackage {
    private String seperatorRegex = "\\|\\|";   // Using double pipe || to separate multiple values in a field.

    // Directory on file system of this input collection
    private File input;

    // Storage of the csv data.
    private CsvReader inputCSV;

    private final String simpleArchiveFormat = "SimpleArchiveFormat";
    
    //Set a a Symbolic Link for filesinstead of copying them
    private boolean symbolicLink = false;

    /**
     * Default constructor. Main method of this class is processMetaPack. The goal of this is to create a Simple Archive Format
     * package from input of files and csv metadata.
     */
    public SAFPackage() {
    }

    
    public void setSymbolicLink(boolean symbolicLink) {
        this.symbolicLink = symbolicLink;
    }

    /**
     * Gets a "handle" on the metadata file
     *
     * @param inputDir Path to input directory.
     * @param metaFile Filename of the CSV
     */
    private void openCSV(String inputDir, String metaFile) {
        input = new File(inputDir);

        if (!inputDir.endsWith("\\/")) {
            inputDir = inputDir + "/";
        }
        String absoluteFileName = inputDir + metaFile;
        try {
            InputStream csvStream = new FileInputStream(absoluteFileName);
            inputCSV = new CsvReader(csvStream, detectCharsetOfFile(absoluteFileName));
        } catch (Exception e) {
            System.out.println(input.getAbsolutePath());
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Opened CSV File:" + absoluteFileName);
    }

    /**
     * Try to automatically detect charset/encoding of a file. UTF8 or iso-8859 are likely
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static Charset detectCharsetOfFile(String filePath) throws IOException {
        UniversalDetector detector = new UniversalDetector(null);

        byte[] buf = new byte[4096];
        FileInputStream fis = new FileInputStream(filePath);
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        detector.dataEnd();

        fis.close();

        String charset = detector.getDetectedCharset();
        if(charset == null) {
            charset = "UTF-8";
            System.out.println("Didn't properly detect the charset of file. Setting to UTF-8 as a fallback");
        }
        Charset detectedCharset = Charset.forName(charset);
        System.out.println("Detected input CSV as:" + detectedCharset.displayName());
        return detectedCharset;
    }


    /**
     * open metafile
     * foreach(metarows as metarow)
     * makeDirectory(increment)
     * copy filenames into directory
     * make contents file with entries for each filename
     * foreach(metarow.columns as column)
     * add meta entry to metadata xml
     *
     * @param pathToDirectory Path to the directory containing the content files and CSV
     * @param metaFileName    Filename of the CSV
     * @throws java.io.IOException If the files can't be found or created.
     */
    public void processMetaPack(String pathToDirectory, String metaFileName, Boolean exportToZip) throws IOException {
        openCSV(pathToDirectory, metaFileName);

        scanAllFiles();                                                         // For Reporting file usage

        prepareSimpleArchiveFormatDir();

        processMetaHeader();
        processMetaBody();

        if(exportToZip) {
            exportToZip(pathToDirectory);
        }

        printFiles(0);                                                          // print a report of files not used
    }

    public void processMetaPack(String pathToCSV, Boolean exportToZip) throws IOException {
        File csvFile = new File(pathToCSV);
        processMetaPack(csvFile.getParent(), csvFile.getName(), exportToZip);
    }

    public void exportToZip(String pathToDirectory) {
        String safDirectory = pathToDirectory + "/" + simpleArchiveFormat;
        String zipDest = pathToDirectory + "/" + simpleArchiveFormat + ".zip";
        try {
            ZipUtil.createZip(safDirectory, zipDest);
            System.out.println("ZIP file located at: " + new File(zipDest).getAbsolutePath());
        } catch (IOException e) {
            System.out.println("ERROR Zipping SAF: " + e.getMessage());
        }
    }

    /**
     * Creates a clean/empty SimpleArchiveFormat directory for the output to go.
     */
    private void prepareSimpleArchiveFormatDir() {

        File newDirectory = new File(input.getPath() + "/" + simpleArchiveFormat);
        if (newDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(newDirectory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        newDirectory.mkdir();
        System.out.println("Output directory is: " + newDirectory.getAbsolutePath());
    }

    private static String[][] fileListFake;

    /**
     * Make a list of all the files in the input directory.
     * Initialize the count for each file found to have zero usages.
     */
    private void scanAllFiles() {
        String[] files = input.list();

        fileListFake = new String[files.length][2];
        for (int i = 0; i < files.length; i++) {
            fileListFake[i][0] = files[i];
            fileListFake[i][1] = "0";
        }
    }

    /**
     * Marks that the filename being referred to is counted as being used.
     * This method is used for scanning the files in the directory for ones that are used/unused.
     *
     * @param filename Name of file referred to in CSV
     */
    private void incrementFileHit(String filename) {
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

    /**
     * Displays the files that exist in the directory that have been used the specified number of times.
     * Used for finding files that have not been used.
     *
     * @param numHits The specified number of times the file should have been used. Value of 0 means unused file.
     */
    private void printFiles(Integer numHits) {
        for (int i = 0; i < fileListFake.length; i++) {
            if (fileListFake[i][1].contentEquals(numHits.toString())) {
                System.out.println("File: " + fileListFake[i][0] + " has been used " + numHits + " times.");
            }
        }
    }

    /**
     * Scans the Header row of the metadata csv to usable object.
     *
     * @throws IOException If the CSV can't be found or read
     */
    private void processMetaHeader() throws IOException {
        inputCSV.readHeaders();
    }

    /**
     * Gets the value for a specified header column
     *
     * @param columnNum The integer value
     * @return Text value for the specified header column
     * @throws IOException If the CSV can't be found or read
     */
    private String getHeaderField(int columnNum) throws IOException {
        return inputCSV.getHeader(columnNum);
    }

    /**
     * Method to process the content/body of the metadata csv.
     * Delegate the work of processing each row to other methods.
     * Does not process the header.
     *
     * @throws IOException If the CSV can't be found or read
     */
    private void processMetaBody() throws IOException {
        // The implementation of processing CSV starts counting from 0. 0 = header, 1..n = body/content
        int rowNumber = 1;

        while (inputCSV.readRecord()) {
            processMetaBodyRow(rowNumber++);
        }
    }

    /**
     * Processes a row in the metadata CSV.
     * Processing a row means using all of the metadata fields, and adding all of the files mentioned to the package.
     *
     * @param rowNumber Row in the CSV.
     */
    private void processMetaBodyRow(int rowNumber) {
        String currentItemDirectory = makeNewDirectory(rowNumber);
        String dcFileName = currentItemDirectory + "/dublin_core.xml";
        File contentsFile = new File(currentItemDirectory + "/contents");
        File collectionFile = new File(currentItemDirectory + "/collections");

        //Specify multiple alternatives for filename, to accept wider input.
        String[] filenameColumn = {"filename", "bitstream", "bitstreams"};
        String[] filenameWithPartsColumn = {"filename__", "bitstream__", "bitstreams__"};

        try {
            BufferedWriter contentsWriter = new BufferedWriter(new FileWriter(contentsFile));

            String[] currentLine = inputCSV.getValues();

            OutputXML xmlWriter = new OutputXML(dcFileName);
            xmlWriter.start();
            Map<String, OutputXML> nonDCWriters = new HashMap<String, OutputXML>();

            for (int j = 0; j < inputCSV.getHeaderCount(); j++) {
                if (j >= currentLine.length) {
                    break;
                }
                if (currentLine[j].length() == 0) {
                    continue;
                }

                if (Arrays.asList(filenameColumn).contains(getHeaderField(j))) {
                    // filename
                    processMetaBodyRowFile(contentsWriter, currentItemDirectory, currentLine[j], "");
                } else if (StringUtils.indexOfAny(getHeaderField(j), filenameWithPartsColumn) >= 0 ) {
                    // filename__
                    //This file has extra parameters, such as being destined for a bundle, or specifying primary
                    String[] filenameParts = getHeaderField(j).split("__", 2);
                    processMetaBodyRowFile(contentsWriter, currentItemDirectory, currentLine[j], filenameParts[1]);
                } else if (getHeaderField(j).contains("filegroup")) {
                    String[] parameterParts = getHeaderField(j).split("__", 2);
                    String extraParameter = (parameterParts.length == 1) ? "" : parameterParts[1];
                    processMetaBodyRowFilegroup(contentsWriter, currentItemDirectory, currentLine[j], extraParameter);
                } else if (getHeaderField(j).contains("collection")) {
                    //TODO, figure out strategy for validation
                    processMetaBodyRowCollections(collectionFile, currentLine[j]);
                } else {
                    //Metadata
                    String[] dublinPieces = getHeaderField(j).split("\\.");
                    if (dublinPieces.length < 2) {
                        // strange field, skip
                        continue;
                    }
                    String schema = dublinPieces[0];
                    if (schema.contentEquals("dc")) {
                        processMetaBodyRowField(getHeaderField(j), currentLine[j], xmlWriter);
                    } else {
                        if (!nonDCWriters.containsKey(schema)) {
                            OutputXML schemaWriter = new OutputXML(currentItemDirectory + File.separator + "metadata_" + schema + ".xml", schema);
                            schemaWriter.start();
                            nonDCWriters.put(schema, schemaWriter);
                        }
                        processMetaBodyRowField(getHeaderField(j), currentLine[j], nonDCWriters.get(schema));
                    }
                }
            }
            contentsWriter.close();
            xmlWriter.end();
            for (String key : nonDCWriters.keySet()) {
                nonDCWriters.get(key).end();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the values for the specific piece of metadata to the output. Accepts
     * multiple values per value so long as they are separated by the separator character
     *
     * @param field_header Field name, such as dc.description or dc.description.abstract
     * @param field_value  Metadata value or values. Multiple values can be separated by a separator character.
     * @param xmlWriter    The xml file that the data is being written to
     */
    private void processMetaBodyRowField(String field_header, String field_value, OutputXML xmlWriter) {
        // process Metadata field. Multiple entries can be specified with separator character
        String[] fieldValues = field_value.split(seperatorRegex);
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
     * Processes the files for the filename column.
     * open contents
     * for-each files as file
     * copy file into directory
     * add file to contents
     *
     * @param contentsWriter Writer to the contents file which tracks the files to ingest for item
     * @param itemDirectory  Absolute path to the directory to put the files in
     * @param filenames      String with filename / filenames separated by separator.
     * @param globalFileParameters Parameters for these files. Blank value means nothing special needs to happen.
     */
    private void processMetaBodyRowFile(BufferedWriter contentsWriter, String itemDirectory, String filenames, String globalFileParameters) {
        String[] files = filenames.split(seperatorRegex);

        for (int j = 0; j < files.length; j++) {
            /* Trim whitespace and add a __ at the end to avoid array out of bounds exception
             * on the filenameParts[1] reference. (Could have also done if.. else..)
             * filenameParts[0] = the actual file name
             * filenameParts[1] = the remaining SAF parameters, still delimited by "__"
             */
            String[] filenameParts = (files[j].trim() + "__").split("__", 2);
            String currentFile = filenameParts[0];
            
            /* This takes the parameters as specified at the header row and adds them to the
             * parameters for this individual file. The order is important here: by taking
             * the local parameters first, they are able to override the global params.
             */
            String fileParameters = filenameParts[1] + "__" + globalFileParameters;

            try {
                
                //copying files
                if (!symbolicLink) {
                    FileUtils.copyFileToDirectory(new File(input.getPath() + "/" + currentFile), new File(itemDirectory));
                } 
                //instead of copying them, set a symbolicLink
                else {
                    Path pathLink = (new File(input.getPath() + "/" + currentFile)).toPath();
                    File f = new File(currentFile);
                    Path pathTarget = (new File(itemDirectory + "/" + f.getName())).toPath();
                    Files.createSymbolicLink(pathTarget, pathLink);
                }
                incrementFileHit(currentFile); //TODO fix file counter to deal with multifiles

                String contentsRow = getFilenameName(currentFile);
                if (fileParameters.length() > 0) {
                    // bundle:SOMETHING, primary:TRUE or description:Something, or any combination with "__" in between
                    String[] parameters = fileParameters.split("__");
                    for (String parameter : parameters) {
                        contentsRow = contentsRow.concat("\t" + parameter.trim());
                    }
                }
                contentsWriter.append(contentsRow);

                contentsWriter.newLine();
            } catch (FileNotFoundException fnf) {
                System.out.println("There is no file named " + currentFile + " in " + input.getPath() + " while making " + itemDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Obtain just the filename from the string. The string could include some path information, which is un-needed
     *
     * @param filenameWithPath The filename, may or may not include paths
     * @return The filename with no path or slashes.
     */
    private String getFilenameName(String filenameWithPath) {
        if (filenameWithPath.contains("\\")) {
            String[] pathSegments = filenameWithPath.split("\\");
            return pathSegments[pathSegments.length - 1];
        } else if (filenameWithPath.contains("/")) {
            String[] pathSegments = filenameWithPath.split("/");
            return pathSegments[pathSegments.length - 1];
        } else {
            return filenameWithPath;
        }
    }

    /**
     * Makes a new directory for the item being processed
     * /path/to/input/SimpleArchiveFormat/item_27/
     *
     * @param itemNumber Iterator for the item being processed, Starts from zero.
     * @return Absolute path to the newly created directory
     */
    private String makeNewDirectory(int itemNumber) {
        File newDirectory = new File(input.getPath() + "/" + simpleArchiveFormat + "/item_" + itemNumber);
        newDirectory.mkdir();
        return newDirectory.getAbsolutePath();
    }

    /**
     * Reads a .tar.gz file that would contain the files for the metadata row.
     *
     * @param itemDirectory
     * @param filename
     * @param fileParameters
     * @throws FileSystemException
     */
    @SuppressWarnings("unchecked")
    private void processMetaBodyRowFilegroup(BufferedWriter contentsWriter, String itemDirectory, String filename, String fileParameters) throws FileSystemException {
        ArrayList<FileObject> filesCollection = new ArrayList<FileObject>();

        FileSystemManager fileSystemManager = VFS.getManager();
        FileObject tarGZFile = fileSystemManager.resolveFile("tgz://" + input.getPath() + "/" + filename);
        // List the children of the Jar file
        FileObject[] children = tarGZFile.getChildren();
        for (int i = 0; i < children.length; i++) {
            FileObject[] grandChildren = children[i].getChildren();

            for (int j = 0; j < grandChildren.length; j++) {
                FileObject grandChild = grandChildren[j];
                if (grandChild.getName().getBaseName().equals(".htaccess") || grandChild.getType() != FileType.FILE) {
                    continue;
                }
                filesCollection.add(grandChildren[j]);
            }
        }

        Collections.sort(filesCollection, new FileObjectComparator());

        // Using reverse depend on your stance on which order to sort bitstreams from DS-749
        // TODO allow for custom sorting/ordering/reversing
        Collections.reverse(filesCollection);

        // TODO This method needs to be tested. Processing file groups in general needs to be tested.
        for (FileObject fileObject : filesCollection) {
            addFileObjectToItem(contentsWriter, fileObject, fileSystemManager.resolveFile("file://" + itemDirectory), fileParameters);
        }
    }

    public void processMetaBodyRowCollections(File collectionFile, String collectionsValues) throws IOException {
        collectionsValues = collectionsValues.trim();
        String[] collections = collectionsValues.split(seperatorRegex);

        //TODO Validating collections is alpha, so leave disabled...
        boolean validateCollection = false;

        for(String collection : collections) {
            if(StringUtils.isEmpty(collection)) {
                continue;
            }

            if(validateCollection) {
                validateCollection(collection);
            }

            FileUtils.writeStringToFile(collectionFile, collection, true);
            FileUtils.writeStringToFile(collectionFile, System.getProperty("line.separator"), true);
        }
    }

    static HashSet<String> handleSet;

    public void initializeCollectionsSet() throws IOException{
        String restAPI = "http://localhost:8080/rest/collections";
        handleSet = new HashSet<String>();


        HttpGet request = new HttpGet(restAPI);
        request.setHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");
        //request.addHeader("rest-dspace-token", token);
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(request);

        if(httpResponse.getStatusLine().getStatusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode collNodes = mapper.readValue(httpResponse.getEntity().getContent(), JsonNode.class);
            for(JsonNode collNode : collNodes) {
                String handle = collNode.get("handle").asText();
                handleSet.add(handle);
            }

        } else {
            throw new IOException("REST API not available");
        }
    }

    public void validateCollection(String collectionHandle) throws IOException {
        if(handleSet == null) {
            initializeCollectionsSet();
        }

        if(! handleSet.contains(collectionHandle)) {
            throw new IOException("Handle did not exist in handleset:" + collectionHandle);
        }


    }

    public void generateManifest(String pathToCSV) {
        File csvFile = new File(pathToCSV);
        File directory = csvFile.getParentFile();
        System.out.println("Creating manifest of files in directory:" + directory + " will output results to: " + csvFile);

        //TODO, if CSV doesn't exist, errors get reported
        openCSV(csvFile.getParent(), csvFile.getName());

        CsvWriter csvWriter = new CsvWriter(pathToCSV);
        String[] header = new String[]{"filename", "dc.title", "dc.contributor.author","dc.date.issued","dc.description.abstract", "dc.subject"};
        try {
            csvWriter.writeRecord(header);
            csvWriter.endRecord();

            String[] files = input.list();

            System.out.print("Building manifest:");
            for (int i = 0; i < files.length; i++) {
                //Skip dot files, blanks, and the current CSV file.
                if(StringUtils.isEmpty(files[i]) || files[i].startsWith(".") || files[i].equals(csvFile.getName())) {
                    System.out.print("Skip:[" + files[i]+"]");
                    continue;
                }

                csvWriter.write(files[i]);
                csvWriter.endRecord();
                System.out.print(".");
            }

            System.out.println(" - " + files.length + " files were added to manifest.");

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.err.println(e.getMessage());
        } finally {
            csvWriter.close();
        }
    }

    /**
     * A comparator for the FileObject which does an alphanum sort of the filename (baseName) of the FileObject
     */
    class FileObjectComparator implements Comparator<FileObject> {
        public int compare(FileObject a, FileObject b) {
            AlphanumComparator alphanumComparator = new AlphanumComparator();

            try {

                return alphanumComparator.compare(a.getName().getBaseName(), b.getName().getBaseName());
            } catch (Exception e) {
                System.out.println("ERROR IN COMPARISON");
                return 0;
            }

        }
    }

    /**
     * Move the commons "FileObject" to the item's directory.
     *
     * @param contentsWriter
     * @param destinationDirectory
     */
    private void addFileObjectToItem(BufferedWriter contentsWriter, FileObject fileObject, FileObject destinationDirectory, String fileParameters) {
        try {
            if (fileObject.canRenameTo(destinationDirectory)) {
                fileObject.moveTo(destinationDirectory);
            } else {
                // Can't move the file, have to copy it.
                // Have to create an end-point file which will absorb the contents we are writing.
                FileSystemManager fsManager = VFS.getManager();
                FileObject localDestFile = fsManager.resolveFile(destinationDirectory.getName().getPath() + "/" + fileObject.getName().getBaseName());
                localDestFile.createFile();
                localDestFile.copyFrom(fileObject, Selectors.SELECT_ALL);
            }
            incrementFileHit(fileObject.getName().getBaseName()); //TODO Don't know if this file would exist

            String contentsRow = fileObject.getName().getBaseName();
            if (fileParameters.length() > 0) {
                // BUNDLE:SOMETHING or BUNDLE:SOMETHING__PRIMARY:TRUE or PRIMARY:TRUE
                String[] parameters = fileParameters.split("__");
                for (String parameter : parameters) {
                    contentsRow = contentsRow.concat("\t" + parameter.trim());
                }
            }
            contentsWriter.append(contentsRow);

            contentsWriter.newLine();
        } catch (FileNotFoundException fnf) {
            System.out.println("There is no file named " + fileObject.getName().getBaseName() + " while making " + destinationDirectory.getName().getBaseName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
