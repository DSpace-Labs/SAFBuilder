package edu.osu.kb.batch;

import com.csvreader.CsvReader;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;


import java.io.*;


/**
 * Created by IntelliJ IDEA.
 * User: peterdietz
 * Date: 11/29/11
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class PDFProvenanceStamper {
    private static CsvReader inputCSV;
    private static String basePDFPath;

    public static void main(String[] args) throws DocumentException {
        if(args.length < 2) {
            System.err.println("USAGE: PDFProvenanceStamper metadataFileWithPath baseDirectoryOfPDFS");
            System.exit(-1);
        }

        String metaFile = args[0];
        String inputDir = args[1];

        basePDFPath = inputDir;

        PDFProvenanceStamper stamper = new PDFProvenanceStamper();
        try {
            stamper.processMetaSheet(inputDir, metaFile);
        } catch (IOException e) {
            System.err.println(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void processMetaSheet(String inputDir, String metafile) throws IOException, DocumentException {
        openCSV(metafile);

        inputCSV.readHeaders();

        while(inputCSV.readRecord()) {
            processBodyRow();
        }
    }

    public void processBodyRow() throws IOException, DocumentException {
        String[] currentLine = inputCSV.getValues();
        //Peter's
        // 0 issue_id, 1 section_name, 2 xml-body, 3 arbitrary number, 4 journal path, 5 xmlFileName, 6 command


        //Henry's
        // 0 xmlBody, 1 filename, 2 command
        final int columnProvenance = 0;
        final int columnLicensing = 1;
        final int columnFileName = 2;

        String provenanceText = currentLine[columnProvenance].trim();
        String provenanceLicense = currentLine[columnLicensing].trim();
        String pathToPDF = basePDFPath + "/" + currentLine[columnFileName];

        //Now add the provenanceText to the footer of the PDF
        PdfReader pdfReader = new PdfReader(pathToPDF);
        PdfStamper pdfStamper = new PdfStamper(pdfReader, new FileOutputStream(pathToPDF+".new.pdf"));
        BaseFont bf_helv = BaseFont.createFont(BaseFont.HELVETICA, "Cp1252", false);


        //Grab/Stamp Only the last page of PDF
        PdfContentByte content = pdfStamper.getOverContent(pdfReader.getNumberOfPages());
        PdfDocument pdfDocument = content.getPdfDocument();
        int fontSize = 7;

        Float characterWidth = (pdfDocument.right() - pdfDocument.left())/fontSize;
        int widthBuffer = 20;
        if(provenanceText.length() > characterWidth + widthBuffer ) {
            fontSize--;
            System.out.println(pathToPDF + " has a long provenance("+provenanceText.length()+"), we are reducing the font size. CW:"+characterWidth);
        }

        content.setFontAndSize(bf_helv, fontSize);
        content.setLineWidth(0f);

        content.moveTo(0, pdfDocument.bottom());

        content.beginText();
        content.showTextAligned(PdfContentByte.ALIGN_LEFT, provenanceText, pdfDocument.leftMargin()-20, pdfDocument.bottom()+8-25, 0);
        content.showTextAligned(PdfContentByte.ALIGN_LEFT, provenanceLicense, pdfDocument.leftMargin()-20, pdfDocument.bottom()-25, 0);
        content.endText();
        pdfStamper.close();
    }

    /**
     * Gets a "handle" on the metadata file
     * @param metaFile Filename of the CSV
     */
    protected void openCSV(String metaFile)
    {
        try {
            inputCSV = new CsvReader(metaFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Opened CSV File:" + metaFile);
    }

    /** Inner class to add a header and a footer. */
    static class HeaderFooter extends PdfPageEventHelper {

        public void onEndPage (PdfWriter writer, Document document) {
            Rectangle rect = writer.getBoxSize("art");
            switch(writer.getPageNumber() % 2) {
            case 0:
                ColumnText.showTextAligned(writer.getDirectContent(),
                        Element.ALIGN_RIGHT, new Phrase("even header"),
                        rect.getRight(), rect.getTop(), 0);
                break;
            case 1:
                ColumnText.showTextAligned(writer.getDirectContent(),
                        Element.ALIGN_LEFT, new Phrase("odd header"),
                        rect.getLeft(), rect.getTop(), 0);
                break;
            }
            ColumnText.showTextAligned(writer.getDirectContent(),
                    Element.ALIGN_CENTER, new Phrase(String.format("page %d", writer.getPageNumber())),
                    (rect.getLeft() + rect.getRight()) / 2, rect.getBottom() - 18, 0);
        }
    }

}
