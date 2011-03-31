import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;

import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

class LfExtractor {
	
	LexicalizedParser lp;
	
	LfExtractor() {
		lp = new LexicalizedParser("/Users/rahuljha/Research/tools/stanford-parser-2010-11-30/englishPCFG.ser.gz");
	    lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
	}
	
	public void parseString(String str) {
		
		String[] words = TextUtils.extractWords(str); 
		Tree parse = (Tree) lp.apply(Arrays.asList(words));
			
	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);

	    Collection<TypedDependency> tdl =  gs.typedDependenciesCollapsed();
	    
	    System.out.println(tdl);
	    
		Map<String, LfStruct> lfStructs = new HashMap<String, LfStruct>();
		Map<String, String> waitingObjs = new HashMap<String, String>();
		
	    for(TypedDependency t: tdl) {
	    		    	
	    	if(!lfStructs.containsKey(t.gov().value())) {
    			lfStructs.put(t.gov().value(), new LfStruct(t.gov().value()));
    		}
	    	
	    	lfStructs.get(t.gov().value()).addDepLexUnit(t.reln(), t.dep().value());
	    	
	    	if(waitingObjs.containsKey(t.gov().value())) {
	    		String parentKey = waitingObjs.get(t.gov().value());
	    		lfStructs.get(parentKey).addLexChainItem(t.gov().value(), t.reln(), t.dep().value());
	    	}
	    	
	    	if(t.reln().equals(EnglishGrammaticalRelations.DIRECT_OBJECT)) {
	    		lfStructs.get(t.gov().value()).setType(LfStruct.LfType.VERB);
	    		waitingObjs.put(t.dep().value(), t.gov().value());
	    		
	    	} else if(t.reln().equals(EnglishGrammaticalRelations.COPULA)) {
	    		lfStructs.get(t.gov().value()).setType(LfStruct.LfType.COPULA);
	    	} 
	    }
	    	

	    for(String k: lfStructs.keySet()) {
	    	
	    	String lfStr = lfStructs.get(k).generateLf();
	    	if(lfStr != "") {
	    		System.out.println(lfStr);
	    	}
	    }

	}
	

	
}