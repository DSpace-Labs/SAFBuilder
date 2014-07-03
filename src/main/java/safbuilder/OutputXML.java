package safbuilder;

import com.generationjava.io.xml.SimpleXmlWriter;
import java.io.*;

public class OutputXML
{

    private SimpleXmlWriter writer;
    private FileOutputStream FOS;
    private OutputStreamWriter OSW;
    private BufferedWriter BW;
	private String schema;

	public OutputXML(String outputFile)
    {
        //File should not already exist
        //TODO DELETE THIS
        /*File checkFile = new File(outputFile);
        if(checkFile.exists())
        {
        checkFile.delete();
        }*/


        try {
            FOS = new FileOutputStream(outputFile);
            OSW = new OutputStreamWriter(FOS, "UTF8");
            BW = new BufferedWriter(OSW);
            writer = new SimpleXmlWriter(BW);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.out.println(e.getMessage());
            }
        }
    }

	public OutputXML(String outputFile, String schema) {
		this(outputFile);
		this.schema = schema;
	}

	public void start()
    {
        try {
            writer.writeXmlVersion("1.0", "UTF-8");
            writer.writeEntity("dublin_core");
	        if (schema != null && !"dc".contentEquals(schema)) {
		        writer.writeAttribute("schema", schema);
	        }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Accepts only one dublin core value
     * dc.description dc.description.abstract
     * @param dcField Full dublin core field name -- ex: dc.description OR dc.description.abstract
     * @param metaValue value of the metadata. Should not be blank.
     */
    public void writeOneDC(String dcField, String metaValue)
    {
        String element = "";
        String qualifier = "";
        String[] dublinPieces = dcField.split("\\.");
        if (dublinPieces.length > 1) {
            element = dublinPieces[1];
        }
        if (dublinPieces.length > 2) {
            qualifier = dublinPieces[2];
        }


        try {
            writer.writeEntity("dcvalue");
            if (element.length() > 0) {
                writer.writeAttribute("element", element);
            }
            if (qualifier.length() > 0) {
                writer.writeAttribute("qualifier", qualifier);
            }
            writer.writeText(metaValue);
            writer.endEntity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void end()
    {
        try {
            writer.endEntity();
            writer.close();
            BW.close();
            OSW.close();
            FOS.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private boolean validateElement(String element)
    {
        return (!element.equals("") && !element.equals(null));
    }

    private boolean validateMetadata(String metadata)
    {
        return (!metadata.equals("") && !metadata.equals(null));
    }
}
