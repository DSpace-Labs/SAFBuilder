package edu.osu.kb.batch;

import java.io.IOException;

public class BatchProcess
{

    public static void main(String[] args) throws IOException
    {
        if(args.length != 2)
        {
            System.out.println("USAGE: BatchProcess /path/to/directory metadatafilename.csv");
            System.out.println("Hint -- directory: Use absolute path and no trailing slashes");
            System.out.println("Hint -- metadatafilename: needs to be in the directory, as do the content files");
            System.exit(-1);
        }

        String inputDir = args[0];
        String metaFile = args[1];
        
        SAFPackage safPackageInstance = new SAFPackage();
        safPackageInstance.processMetaPack(inputDir, metaFile);
    }
}
