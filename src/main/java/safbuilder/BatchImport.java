package safbuilder;

import java.io.*;

@Deprecated
public class BatchImport {

    public BatchImport() {
    }

    public void runBatch(String eperson, String collectionId, String source) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec("/dspace/bin/dsrun org.dspace.app.itemimport.ItemImport --add --eperson=" + eperson + " --collection=" + collectionId + " --source=" + source + " --mapfile=" + collectionId + "map");
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
