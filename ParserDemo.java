import java.util.*;
import java.io.*;

class ParserDemo {
	
	public static void main(String[] args) throws IOException {
	  
	  if(args.length < 1) {
		  System.out.println("Please provide an input file...");
		  System.exit(0);
	  } 
	  
	  String inputFileName = args[0];
	  File inputFile = new File(inputFileName);
	  
	  if(!inputFile.exists()) {
		  System.out.println("File doesn't exist...");
		  System.exit(0);
	  }
	  
	  String fileData = readFileAsString(inputFileName);
	  List<String> sentences = TextUtils.extractSentences(fileData);
	  
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

