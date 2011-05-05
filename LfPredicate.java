
import java.util.List;
import java.util.ArrayList;

import edu.northwestern.at.utils.corpuslinguistics.lemmatizer.*;

class LfPredicate {
	public enum predicateType {EVENT, ENTITY};
	
	String lex;
	String headVar;
	List<String> argVars;
	
	LfPredicate(String lex, predicateType type) {
		
		if(type.equals(predicateType.EVENT)) {
			Lemmatizer lemmatizer = null;
			try {
				lemmatizer = new DefaultLemmatizer();
				this.lex = lemmatizer.lemmatize(lex);
			} catch (Exception e) {
				System.err.println("Error in lemmatization"+e.getMessage());
				this.lex = lex;
			}
		} else {
			this.lex = lex;
		}
		
		argVars = new ArrayList<String>();
	}
	
	public void setHeadVar(String varName) {
		this.headVar = varName;
	}
	
	public void addArgVar(String varName) {
		this.argVars.add(varName);
	}
	
	public void clearArgVars() {
		this.argVars = new ArrayList<String>();
	}
	
	public void setLex(String lex) {
		this.lex = lex;
	}
	
	public String getHeadVar() {
		return this.headVar;
	}
	
	public String[] getArgVars() {
		return (String[]) argVars.toArray(new String[] {});
	}
	
	public String toString() {
		String predStr = "";
		
		predStr += this.lex;
		predStr += " (";
		predStr += headVar+", ";
		for(String argVar:argVars) {
			predStr += argVar+", ";
		}
		
		predStr = predStr.substring(0, predStr.length()-2);
		predStr += ")";
		
		return predStr;
	}

	public String getLex() {
		return this.lex;
	}


	
}

