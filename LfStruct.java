import java.util.Map;
import java.util.HashMap;


import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;

class LfStruct {
	
	public enum LfType {NA, VERB, COPULA};
	
	LfType type;
	String mainLexUnit;
	Map<GrammaticalRelation, String> depLexUnits;
	Map<String, Map<GrammaticalRelation, String>> lexicalChains;
	
	Counter counter;
	
	LfStruct(String lexUnit) {
		this.type = LfType.NA;
		this.counter = new Counter();
		this.mainLexUnit = lexUnit;
		
		depLexUnits = new HashMap<GrammaticalRelation, String>();
		lexicalChains = new HashMap<String, Map<GrammaticalRelation, String>>();
	}
	
	void addDepLexUnit(GrammaticalRelation rel, String lexUnit) {
		depLexUnits.put(rel, lexUnit);
	}
	
	void addLexChainItem(String lex, GrammaticalRelation rel, String val) {
		
		Map<GrammaticalRelation, String> lexChainItem;
		if(lexicalChains.containsKey(lex)) {
			lexChainItem = lexicalChains.get(lex);
		} else {
			lexChainItem = new HashMap<GrammaticalRelation, String>();
			lexicalChains.put(lex, lexChainItem);
		}
		
		lexChainItem.put(rel, val); 

	}
	
	void setType(LfType type) {
		this.type = type;
	}
	
	Boolean containsRel(GrammaticalRelation rel) {
		return depLexUnits.containsKey(rel);
	}
	
	public String generateLf() {
		
		String lf = "";
		
		if(this.type.equals(LfType.VERB)) {
			
			if(depLexUnits.containsKey(EnglishGrammaticalRelations.NOMINAL_SUBJECT) &&
				depLexUnits.containsKey(EnglishGrammaticalRelations.DIRECT_OBJECT)) {
				String nsubj = depLexUnits.get(EnglishGrammaticalRelations.NOMINAL_SUBJECT);
				String dobj = depLexUnits.get(EnglishGrammaticalRelations.DIRECT_OBJECT);
				String verbId = counter.getNextEid();
				String subjId = counter.getNextXid();
				String objId = counter.getNextXid();
			
				lf += nsubj + ": (" + subjId + ")";
				lf += " "+dobj + ": (" + objId + ")";
				
				Map<String, String> optionalDeps = new HashMap<String, String>();
				
				if(depLexUnits.containsKey(EnglishGrammaticalRelations.INDIRECT_OBJECT)) {
					String iobj = depLexUnits.get(EnglishGrammaticalRelations.INDIRECT_OBJECT);
					String iobjId = counter.getNextXid();
					optionalDeps.put(iobj, iobjId);
				} if(depLexUnits.containsKey(EnglishGrammaticalRelations.TEMPORAL_MODIFIER)) {
					String tmod = depLexUnits.get(EnglishGrammaticalRelations.TEMPORAL_MODIFIER);
					String tmodId = counter.getNextXid();
					optionalDeps.put(tmod, tmodId);
					
				}
				
				String mainPredStr = " " + mainLexUnit + ": ("+verbId + ", " + subjId + ", " + objId;
				for(String optDep: optionalDeps.keySet()) {
					String optDepId = optionalDeps.get(optDep);
					lf += " " + optDep+": ("+optDepId+")";
					mainPredStr += ", " + optDepId;
				}
				
				mainPredStr += ")";
				lf += mainPredStr;
				
				if(lexicalChains.containsKey(dobj)) {
						
						Map<GrammaticalRelation, String> currLexChain = lexicalChains.get(dobj);
						
						for(GrammaticalRelation chainItemKey: currLexChain.keySet()) {
							if(chainItemKey.toString().startsWith("prep")) {
								String actualRel = chainItemKey.toString().split("_")[1];
								String prep = lexicalChains.get(dobj).get(chainItemKey);
								String prepId = counter.getNextXid();
								lf += " "+ actualRel + ": (" + objId +", "+prepId+")";
								lf += " " + prep + ": (" + prepId + ")";
							} 
							
							else if(chainItemKey.equals(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER)) {
								String adj = lexicalChains.get(dobj).get(chainItemKey);
								lf += " " + adj + ": (" + objId + ")";
							}
						}
				}
				
			} 
		} else if(this.type.equals(LfType.COPULA)) {
			if(depLexUnits.containsKey(EnglishGrammaticalRelations.NOMINAL_SUBJECT) &&
				depLexUnits.containsKey(EnglishGrammaticalRelations.COPULA)) {
				String nsubj = depLexUnits.get(EnglishGrammaticalRelations.NOMINAL_SUBJECT);
				String copula = depLexUnits.get(EnglishGrammaticalRelations.COPULA);
				
				String copulaId = counter.getNextEid();
				String subjId = counter.getNextXid();
				String objId = counter.getNextXid();
				
				lf += copula + ": ("+copulaId + "," + subjId + "," + objId + ")";
				lf += " " + nsubj + ": (" + subjId + ")";
				lf += " " + mainLexUnit + ": (" + objId + ")";
				
				if(depLexUnits.containsKey(EnglishGrammaticalRelations.NUMERIC_MODIFIER)) {
					String num = depLexUnits.get(EnglishGrammaticalRelations.NUMERIC_MODIFIER);
					lf += " " + num + ": (" + subjId + ")";
				}
				
				if(depLexUnits.containsKey(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER)) {
					String adj = depLexUnits.get(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER);
					lf += " " + adj + ": (" + subjId + ")";
				}
				
			}
		}
		
		return lf;
	}
	
}

class Counter {
	int eCount;
	int xCount;
	
	Counter() {
		eCount = 1;
		xCount = 1;
	}
	
	String getNextEid() {
			return "e"+(eCount++);
	};
	
	String getNextXid() {
		return "x"+(xCount++);
	}
}