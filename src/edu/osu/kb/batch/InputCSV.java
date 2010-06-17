package edu.osu.kb.batch;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.util.List;

public class InputCSV {
	
	private CSVReader reader;
	
	public InputCSV(String file) throws IOException
	{
		reader = new CSVReader(new FileReader(file));
	}
	
	public List parse()
	{
		try
		{
			return reader.readAll();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
