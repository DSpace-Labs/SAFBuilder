package safbuilder;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit test for simple App.
 */
public class SAFPackageTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SAFPackageTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(SAFPackageTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public void testInitDefault() {
        Path outputDirectory = Paths.get("src", "sample_data", "SimpleArchiveFormat");
        SAFPackage safPackage = new SAFPackage();
        generateSampleAndAssertCreated(safPackage, outputDirectory);
    }

    public void testInitWithOutputPath() {
        String randomPath =  "__test_output__" + System.currentTimeMillis();
        Path outputDirectory = Paths.get("src", "sample_data", randomPath);
        SAFPackage safPackage = new SAFPackage(randomPath);
        generateSampleAndAssertCreated(safPackage, outputDirectory);
    }

    private void generateSampleAndAssertCreated(SAFPackage safPackageInstance,  Path outputDirectory) {
        String sampleCSV = Paths.get("src", "sample_data", "AAA_batch-metadata.csv").toAbsolutePath().toString();
        try {
            FileUtils.deleteDirectory(outputDirectory.toFile()); // ensure that it doesn't exist
            safPackageInstance.processMetaPack(sampleCSV, false);
            assert Files.exists(outputDirectory);
            FileUtils.deleteDirectory(outputDirectory.toFile());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            assert false;
        }
    }
}
