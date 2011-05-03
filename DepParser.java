import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;


public class DepParser {
	
	LexicalizedParser lp;
	
	DepParser(String pcfgFilePath) {
		lp = new LexicalizedParser(pcfgFilePath);
		lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
	}

	public Collection<TypedDependency> parse(String sentence) {
		
		String[] words = TextUtils.extractWords(sentence);
		Tree parse = (Tree) lp.apply(Arrays.asList(words));
			
	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    
	    Collection<TypedDependency> tdl =  gs.typedDependenciesCollapsedTree();
	    
	    return tdl;

	}

}
