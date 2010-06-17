package edu.osu.kb.batch;

public class BatchProcess {
    public static void main(String[] args) {
        // TODO - Accept input from command line
        String inputDir = "/home/peter/NetBeansProjects/SimpleArchiveFormat_Builder/src/edu/osu/kb/sample_data";   // absolute path, no trailing slash
        String metaFile = "AAA_batch-metadata.csv";

        SAFPackage safPackageInstance = new SAFPackage();
        safPackageInstance.processMetaPack(inputDir, metaFile);
    }
}