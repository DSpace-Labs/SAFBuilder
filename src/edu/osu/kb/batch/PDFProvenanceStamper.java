package edu.osu.kb.batch;

import com.csvreader.CsvReader;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.commons.io.FileUtils;


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
        final int columnPushDown = 0;
        final int columnProvenance = 1;
        final int columnLicensing = 2;
        final int columnFileName = 3;


        String provenanceText = currentLine[columnProvenance].trim();
        String provenanceLicense = currentLine[columnLicensing].trim();
        String pathToPDF = basePDFPath + "/" + currentLine[columnFileName];

        Float pushDownAdditional = Float.parseFloat(currentLine[columnPushDown].trim());

        // By default we push 25 units lower than bottom. We can allow the user to push it down slightly more.
        // pushdownAdditional of 0 does nothing, 1 then down 1, 2 then down 2
        float pushDown = -25f;
        pushDown = pushDown - pushDownAdditional;

        //Now add the provenanceText to the footer of the PDF

        File originalPDF = new File(pathToPDF);
        File archivePDF = new File(pathToPDF+".no-citation-backup.pdf");

        if(originalPDF.exists() && ! archivePDF.exists()) {
            FileUtils.moveFile(originalPDF, archivePDF);
            FileUtils.waitFor(archivePDF, 1);
        }



        PdfReader pdfReader = new PdfReader(pathToPDF+".no-citation-backup.pdf");
        PdfStamper pdfStamper = new PdfStamper(pdfReader, new FileOutputStream(pathToPDF));
        BaseFont bf_helv = BaseFont.createFont(BaseFont.HELVETICA, "Cp1252", false);


        //Grab/Stamp Only the last page of PDF
        PdfContentByte content = pdfStamper.getOverContent(pdfReader.getNumberOfPages());
        PdfDocument pdfDocument = content.getPdfDocument();
        int fontSize = 7;

        int widthBuffer = 10;



        Float characterWidth = (pdfDocument.right() - pdfDocument.left() - widthBuffer)/fontSize;

        if(provenanceText.length() > characterWidth  ) {
            fontSize--;
            //System.out.println(pathToPDF + " has a long provenance("+provenanceText.length()+"), we are reducing the font size. CW:"+characterWidth);
        }

        if(provenanceText.length() > characterWidth + 40 ) {
            fontSize--;
            System.out.println(pathToPDF + " has a long provenance("+provenanceText.length()+"), we are reducing the font size again. CW:"+characterWidth);
        }

        if(provenanceText.length() > characterWidth + 60 ) {
            fontSize--;
            System.out.println(pathToPDF + " has a long provenance("+provenanceText.length()+"), we are reducing the font size again again. CW:"+characterWidth);
        }


        content.setFontAndSize(bf_helv, fontSize);
        content.setLineWidth(0f);

        content.moveTo(0, pdfDocument.bottom());

        content.beginText();

        content.showTextAligned(PdfContentByte.ALIGN_LEFT, provenanceText, pdfDocument.leftMargin()-20, pdfDocument.bottom()+8+pushDown, 0);
        content.showTextAligned(PdfContentByte.ALIGN_LEFT, provenanceLicense, pdfDocument.leftMargin()-20, pdfDocument.bottom()+pushDown, 0);

        /*BarcodeQRCode qrCode = new BarcodeQRCode(provenanceText + " " + provenanceLicense, Math.round(pdfDocument.left()), Math.round(pdfDocument.bottom()), null);
         *Image qrImage = qrCode.getImage();
         *qrImage.setAbsolutePosition(pdfDocument.left(), pdfDocument.bottom());
         *content.addImage(qrImage);
         */


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
