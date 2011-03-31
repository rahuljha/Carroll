import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;


class TextUtils {
	public static String[] extractWords(String sentence) {
		   Vector<String> words = new Vector<String>();
		   
		   BreakIterator boundary = BreakIterator.getWordInstance();
	       boundary.setText(sentence);
	       
	       int start = boundary.first();
	       for (int end = boundary.next();
	            end != BreakIterator.DONE;
	            start = end, end = boundary.next()) {
	    	   	String word = sentence.substring(start, end);
	    	   	word = word.replaceAll("\\s+", "");
	    	   	if(!word.equals("")) {
	    	   		words.add(word);
	    	   	}
	       }
		   
	       return words.toArray(new String[0]);
	  }
	
	public static List<String> extractSentences(String rawText) {
		   
		   List<String> sentences = new ArrayList<String>();
		   
		   BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
		   bi.setText(rawText);
		   
		   int lastIndex = bi.first();
		   while (lastIndex != BreakIterator.DONE) {
			   int firstIndex = lastIndex;
			   lastIndex = bi.next();

			   if (lastIndex != BreakIterator.DONE) {
				   String sentence = rawText.substring(firstIndex, lastIndex);
				   sentence = sentence.replaceAll("\\n"," ");
				   sentence = sentence.replaceAll("^\\s+","");
				   sentences.add(sentence);
		          }
		      }
		   
		   return sentences;
	   }
}