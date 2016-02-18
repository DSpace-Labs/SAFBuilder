# SAFBuilder - Item Packager from CSV

A tool that turns content files and a metadata spreadsheet into a Simple Archive Format package, which easily allows for batch import to DSpace, an Institutional Repository.

See also: [Wiki entry on Simple Archive Format Packager](https://wiki.duraspace.org/display/DSPACE/Simple+Archive+Format+Packager "Simple Archive Format Package wiki entry")

To Install and generate an ItemImport package:

```bash
git clone https://github.com/DSpace-Labs/SAFBuilder.git
cd SAFBuilder
./safbuilder.sh -c src/sample_data/AAA_batch-metadata.csv -z
```

Prerequisites:

 * Command line / terminal
 * Java JDK
 * Git
 * Maven

Help Usage (i.e. ./safbuilder.sh --help):

```
usage: SAFBuilder   
 -c,--csv <arg>   Filename with path of the CSV spreadsheet. This must be
                  in the same directory as the content files
 -h,--help        Display the Help
 -z,--zip         (optional) ZIP the output
 ```


Input
-----
A spreadsheet (.csv) with the following columns:
* filename for the bitstream/file
* metadata with namespace.element.(qualifer). Examples would be: dc.description or dc.contributor.author
![Image of a sample input spreadsheet with metadata](https://wiki.duraspace.org/download/attachments/20809267/metadata-spreadsheet.png?version=1&modificationDate=1276806916424 "sample spreadsheet with metadata")


Output
------
The output is a directory "SimpleArchiveFormat" in the same directory as the CSV. If you specify to have a ZIP file created, it is in the same directory as the CSV, and will be named SimpleArchiveFormat.zip
```
SimpleArchiveFormat/
  item_000/
      dublin_core.xml         -- qualified Dublin Core metadata for metadata fields belonging to the dc schema
      metadata_[prefix].xml   -- metadata in another schema, the [prefix] is the short name of the schema as registered with the metadata registry
      contents                -- text file containing one line per filename
      file_1.doc              -- files to be added as bitstreams to the item
      file_2.pdf
  item_001/
      dublin_core.xml
      contents
      file_1.png
  item_...
```

You can then import the SimpleArchiveFormat directory into DSpace as-is (see https://wiki.duraspace.org/display/DSDOC5x/Importing+and+Exporting+Items+via+Simple+Archive+Format for further information). Or you can import the ZIP file into portions of DSpace that enable Batch Import from Zip files.


Other Things
-----

Author: Peter Dietz - [Longsight, DSpace Service & Hosting Provider](http://longsight.com)

Version History:

* Current - Use -c to specify csv, and -z to indicate to zip the contents
* v3, v2, v1 - ./safbuilder.sh /path/to/parentDirectory file.csv

Older versions of this tool required a space between the parent directory file path, and the filename of the CSV. The current version combines the path and the filename. It also allows the ability to ZIP the contents.
