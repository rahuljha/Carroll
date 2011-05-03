import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;


class PredicateExtractor {
	
	Map<String, LfPredicate> predicateMap;
	Map<String, String> inversePredicateMap;
	
	Map<String, LfPredicate> waitingForParentOf;
	
	PredicateCounter predicateCounter;
	
	public Collection<LfPredicate> extractPredicates(Collection<TypedDependency> typedDependencies) {
		
		predicateMap = new HashMap<String, LfPredicate>();
		inversePredicateMap = new HashMap<String, String>();
		waitingForParentOf = new HashMap<String, LfPredicate>();
		
		predicateCounter = new PredicateCounter();
		
		for(TypedDependency td: typedDependencies) {
			String handlerName = td.reln().getShortName()+"Handler";
			
			try {
				Method handler = this.getClass().getDeclaredMethod(handlerName, new Class[]{td.getClass()});
				handler.invoke(this, td);
			}
			catch (NoSuchMethodException e) {
				System.err.println("No method called "+handlerName);
				continue; //we don't worry about methods that don't exist
			}
			catch (SecurityException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}
		
		normalizeConjs();
		
		return predicateMap.values();
	}
	
	void eventRelHandler(TypedDependency td, LfPredicate.predicateType secondPredType) {
		LfPredicate eventPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate entityPred = getPred(td.dep(), secondPredType);
		
		eventPred.addArgVar(entityPred.getHeadVar());
		inversePredicateMap.put(td.dep().toString(), td.gov().toString());
		
	}
	
	/* Here begin the handlers for the supported typed dependencies*/
	
	void advmodHandler(TypedDependency td) {
		LfPredicate verbPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate advPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		advPred.setHeadVar(verbPred.headVar);
		predicateMap.put(td.dep().toString(), advPred);
	} 
	
	void amodHandler(TypedDependency td) {
		LfPredicate nounPred = getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate adjPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		adjPred.setHeadVar(nounPred.headVar);
		predicateMap.put(td.dep().toString(), adjPred);
	} 
	
	void auxHandler(TypedDependency td) {		
		String auxLex = td.dep().value();
		
		LfPredicate auxPred = new LfPredicate(auxLex);
		
		LfPredicate secondPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		auxPred.addArgVar(secondPred.headVar);
		
		if(inversePredicateMap.containsKey(td.gov().toString())) {
			String parentKey = inversePredicateMap.get(td.gov().toString());
			auxPred.setHeadVar(predicateMap.get(parentKey).getHeadVar());
		} else {
			waitingForParentOf.put(td.gov().toString(), auxPred);
		}
		
		predicateMap.put(auxLex+"_"+secondPred, auxPred);
		
	}
	
	void conjHandler(TypedDependency td) {
		String conjLex = td.reln().toString().split("_")[1];
		
		LfPredicate conjPred = new LfPredicate(conjLex);
		
		LfPredicate firstPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate secondPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		
		conjPred.addArgVar(firstPred.headVar);
		conjPred.addArgVar(secondPred.headVar);
		
		predicateMap.put(firstPred.headVar+"_"+secondPred.headVar, conjPred);
	}
	
	void copHandler(TypedDependency td) {
		LfPredicate copPred = getPred(td.dep(), LfPredicate.predicateType.EVENT);
				
		if(predicateMap.containsKey(td.gov().toString())) {
			
			String objArg = "";
			if(predicateMap.get(td.gov().toString()).getArgVars().length > 0) {
				objArg = predicateMap.get(td.gov().toString()).getArgVars()[0];
			}
			
			predicateMap.remove(td.gov().toString());
			copPred.addArgVar(objArg);
			copPred.addArgVar(getPred(td.gov(), LfPredicate.predicateType.ENTITY).headVar);
		} 
	}
	
	void dobjHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY);
	}
	
	void nsubjHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY);
	}
	
	void possHandler(TypedDependency td) {
		LfPredicate possessed = getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate possPred = getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		possPred.setHeadVar(possessed.headVar);
		
		predicateMap.put(td.dep().value(), possPred);
	}

	void prepHandler(TypedDependency td) {
		
		String prepLex;
		if(td.reln().toString().split("_").length > 1) {
			prepLex = td.reln().toString().split("_")[1];
		} else {
			prepLex = "";
		}
		
		LfPredicate prepPred = new LfPredicate(prepLex);
		
		LfPredicate firstPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate secondPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		
		prepPred.setHeadVar(firstPred.headVar);
		prepPred.addArgVar(secondPred.headVar);
		
		firstPred.addArgVar(secondPred.headVar);
		
		predicateMap.put(firstPred.headVar+"_"+secondPred.headVar, prepPred);
		
	}
	
	void tmodHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY);
	}
	
	void xcompHandler(TypedDependency td) {

		LfPredicate firstPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate secondPred = getPred(td.dep(), LfPredicate.predicateType.EVENT);
		
		if(firstPred.getArgVars().length>0) {
			secondPred.addArgVar(firstPred.getArgVars()[0]);
		}
		
		firstPred.addArgVar(secondPred.getHeadVar());
		
		inversePredicateMap.put(td.dep().toString(), td.gov().toString());
		
		if(waitingForParentOf.containsKey(td.dep().toString())) {
			LfPredicate waitingPred = waitingForParentOf.get(td.dep().toString());
			waitingPred.setHeadVar(firstPred.getHeadVar());
			waitingForParentOf.remove(td.dep().toString());
		}
	}
	
	

	LfPredicate getPred(TreeGraphNode tgNode, LfPredicate.predicateType predType) {
		if(predicateMap.containsKey(tgNode.toString())) {
			return predicateMap.get(tgNode.toString());
		} else {
			LfPredicate newPred = new LfPredicate(tgNode.value());
			
			String predId = predType.equals(LfPredicate.predicateType.EVENT) ? predicateCounter.getNextEid() :
																			   predicateCounter.getNextXid();
			newPred.setHeadVar(predId);
			predicateMap.put(tgNode.toString(), newPred);
			return newPred;
		}
	}
	
	void normalizeConjs() {
		Map<String, String> inverseConjArgs = new HashMap<String, String>();  
		
		List<String> removeKeys = new ArrayList<String>();
		
		for(Map.Entry<String, LfPredicate> entry : predicateMap.entrySet()) {
			
			LfPredicate predicate = entry.getValue();
			
			if(predicate.lex.equals("and") ||
				predicate.lex.equals("or")) {
					
				Boolean found = false;
				String conjArg = "";
				for(String arg: predicate.argVars) {
					if(inverseConjArgs.containsKey(arg)) {
						found = true;
						conjArg = inverseConjArgs.get(arg);
					}
				}
				
				if(!found) {
					conjArg = predicateCounter.getNextXid()+"_"+predicate.lex;
				}
				
				for(String arg: predicate.argVars) {
					inverseConjArgs.put(arg, conjArg);
				}
				
				removeKeys.add(entry.getKey());
				
			}

		}
		
		for(String remkey: removeKeys) {
			predicateMap.remove(remkey);
		}
		
		Map<String, List<String>> andPreds = new HashMap<String, List<String>>();
		
		for(Map.Entry<String, String> inverseConjEntry: inverseConjArgs.entrySet()) {
			String andArg = inverseConjEntry.getValue();
			String depArg = inverseConjEntry.getKey();
			
			if(!andPreds.containsKey(andArg)) {
				andPreds.put(andArg, new ArrayList<String>());
			}
			
			andPreds.get(andArg).add(depArg);
		}

		
		
		for(Map.Entry<String, List<String>> entry : andPreds.entrySet()) {
			String andArgConj = entry.getKey();
			String andArg = andArgConj.split("_")[0];
			String andConj = andArgConj.split("_")[1];
			
			List<String> depArgs = entry.getValue();
			
			String predMapKey = "";
			for(String depArg: depArgs) {
				predMapKey += depArg+"_";
			}
			predMapKey = predMapKey.substring(0, predMapKey.length()-1);
			
			LfPredicate andPredicate = new LfPredicate(andConj);
			andPredicate.setHeadVar(andArg);
			
			for(String depArg: depArgs) {
				andPredicate.addArgVar(depArg);	
			}
			
			predicateMap.put(predMapKey, andPredicate);
			
		}
		
	}
	
	
}

