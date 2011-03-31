import java.util.*;
import java.io.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


class Driver {
	
	static String helpMessage = "Carroll -f <filename> | -s <string>";
	
	public static void main(String[] args) throws IOException, ParseException {
		
		Options options = new Options();
		
		options.addOption("s", true, "Read text from the given string");
		options.addOption("f", true, "Read text from the given filename");
		
		
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse( options, args);
		
		String inputText = "";
		
		if(cmd.hasOption("s")) {
		    // read from string
			String inputString = cmd.getOptionValue("s");
			if(inputString == null) {
				System.out.println(helpMessage);
				System.exit(0);
			}
			
			inputText = inputString;
		}
		else if(cmd.hasOption("f")){
		    // read from file
			String inputFileName = cmd.getOptionValue("f");
			if(inputFileName == null) {
				System.out.println(helpMessage);
				System.exit(0);
					
			}
			File inputFile = new File(inputFileName);
			  
			if(!inputFile.exists()) {
				System.out.println("File doesn't exist...");
				System.exit(0);
			}
			  
			inputText = readFileAsString(inputFileName);
		} else {
			System.out.println(helpMessage);
			System.exit(0);
		}
	  
	  
	  
	  List<String> sentences = TextUtils.extractSentences(inputText);
	  
	  LfExtractor lf = new LfExtractor();
	  for(String sentence: sentences) {
		  sentence = sentence.replaceAll("\\[\\*\\*[^\\d]*(\\d+)\\*\\*\\]", "X$1");
		  lf.parseString(sentence);
	  }
	  

	  
  }
  
  /** @param filePath the name of the file to open. Not sure if it can accept URLs or just filenames. Path handling could be better, and buffer sizes are hardcoded
   */ 
   private static String readFileAsString(String filePath) throws java.io.IOException{
       StringBuffer fileData = new StringBuffer(1000);
       BufferedReader reader = new BufferedReader(
               new FileReader(filePath));
       char[] buf = new char[1024];
       int numRead=0;
       while((numRead=reader.read(buf)) != -1){
           String readData = String.valueOf(buf, 0, numRead);
           fileData.append(readData);
           buf = new char[1024];
       }
       reader.close();
       return fileData.toString();
   }
   
}

