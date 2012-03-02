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
	Map<String, LfPredicate> waitingMark;
	Map<String, LfPredicate> waitingAdvcl;
	Map<String, String> passiveSubst;
	
	Map<String, String> inverseNnMap;
	
	PredicateCounter predicateCounter;
	
	public Collection<LfPredicate> extractPredicates(Collection<TypedDependency> typedDependencies) {
		
		predicateMap = new HashMap<String, LfPredicate>();
		inversePredicateMap = new HashMap<String, String>();
		waitingForParentOf = new HashMap<String, LfPredicate>();
		waitingMark = new HashMap<String, LfPredicate>();
		waitingAdvcl = new HashMap<String, LfPredicate>();
		inverseNnMap = new HashMap<String, String>();
		
		predicateCounter = new PredicateCounter();
		passiveSubst = new HashMap<String, String>();
		
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
		generateNnPreds();
		clearNoHeadPreds();
		return predicateMap.values();
	}
	
	void eventRelHandler(TypedDependency td, LfPredicate.predicateType secondPredType, Boolean isMainArg) {
		LfPredicate eventPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate entityPred = getPred(td.dep(), secondPredType);
		
		if(isMainArg) {
			eventPred.addArgVarMain(entityPred.getHeadVar());
		} else {
			eventPred.addArgVar(entityPred.getHeadVar());
		}
		inversePredicateMap.put(td.dep().toString(), td.gov().toString());
		
	}
	
	/* Here begin the handlers for the supported typed dependencies*/
	
	void acompHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY, false);
	}
	
	void advclHandler(TypedDependency td) {

		LfPredicate mainVerb = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate clauseVerb = getPred(td.dep(), LfPredicate.predicateType.EVENT);
		
		LfPredicate advclPredicate = new LfPredicate("", LfPredicate.predicateType.EVENT);
		
		if(waitingMark.containsKey(td.dep().toString())) {
			advclPredicate.setLex(waitingMark.get(td.dep().toString()).getLex());
			waitingMark.remove(td.dep().toString());
			
		} else {
			waitingAdvcl.put(td.dep().toString(), advclPredicate);
			
		}
		
		advclPredicate.setHeadVar(predicateCounter.getNextEid());
		advclPredicate.addArgVar(mainVerb.getHeadVar());
		advclPredicate.addArgVar(clauseVerb.getHeadVar());
		
		predicateMap.put(mainVerb.getLex()+"_"+clauseVerb.getLex(), advclPredicate);
		
	}
	
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
	
	void apposHandler(TypedDependency td) {
		LfPredicate nounPred = getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate apposPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		
		apposPred.setHeadVar(nounPred.headVar);
		predicateMap.put(td.dep().toString(), apposPred);
		
	}
	
	void auxHandler(TypedDependency td) {		
		String auxLex = td.dep().value();
		
		LfPredicate auxPred = new LfPredicate(auxLex, LfPredicate.predicateType.EVENT);
		
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
	
	void auxpassHandler(TypedDependency td) {
		LfPredicate verbPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate auxPred = getPred(td.dep(), LfPredicate.predicateType.EVENT);
		
		verbPred.setHeadVar(predicateCounter.getNextXid());
		verbPred.setLex(td.gov().value());
		
		auxPred.addArgVar(verbPred.getHeadVar());
		
		if(verbPred.getArgVars().length > 0) {
			for(String argVar: verbPred.getArgVars()) {
				auxPred.addArgVar(argVar);
			}
			verbPred.clearArgVars();
		}
		
		for(Map.Entry<String, LfPredicate> entry: predicateMap.entrySet()) {
			
			LfPredicate pred = entry.getValue();
			if(pred.headVar!=null && pred.headVar.equals(verbPred.getLex())) {
				pred.setHeadVar(auxPred.getHeadVar());
			}
		}
		passiveSubst.put(verbPred.getHeadVar(), auxPred.getHeadVar());
		
		predicateMap.put(td.dep().toString(), auxPred);
	}
	
	void conjHandler(TypedDependency td) {
		String conjLex = td.reln().toString().split("_")[1];
		
		LfPredicate conjPred = new LfPredicate(conjLex, LfPredicate.predicateType.ENTITY);
		
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
		eventRelHandler(td, LfPredicate.predicateType.ENTITY, false);
	}
	
	void iobjHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY, false);
	}
	
	void markHandler(TypedDependency td) {
		LfPredicate markPred = new LfPredicate(td.dep().value(), LfPredicate.predicateType.EVENT);
		
		if(waitingAdvcl.containsKey(td.gov().toString())) {
			waitingAdvcl.get(td.dep().toString()).setLex(td.dep().value());
			waitingAdvcl.remove(td.dep().toString());
		} else {
			waitingMark.put(td.gov().toString(), markPred);
		}
	}
	
	void nnHandler(TypedDependency td) {
		LfPredicate govPred = getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate depPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		
		if(inverseNnMap.containsKey(govPred.getHeadVar())) {
			inverseNnMap.put(depPred.getHeadVar(), inverseNnMap.get(govPred.getHeadVar()));
		} else if(inverseNnMap.containsKey(depPred.getHeadVar())) {
			inverseNnMap.put(govPred.getHeadVar(), inverseNnMap.get(depPred.getHeadVar()));
		} else {
			String newNnArg = predicateCounter.getNextXid();
			inverseNnMap.put(govPred.getHeadVar(), newNnArg);
			inverseNnMap.put(depPred.getHeadVar(), newNnArg);
		}
	}
	
	void nsubjHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY, true);
	}
	
	void nsubjpassHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY, true);
	}
	
	void possHandler(TypedDependency td) {
		LfPredicate possessed = getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate possPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		possPred.setHeadVar(possessed.headVar);
	}
	
	void prepHandler(TypedDependency td) {
		
		String prepLex;
		if(td.reln().toString().split("_").length > 1) {
			prepLex = td.reln().toString().split("_")[1];
		} else {
			prepLex = "";
		}
		
		LfPredicate prepPred = new LfPredicate(prepLex, LfPredicate.predicateType.ENTITY);
		
		LfPredicate firstPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate secondPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		
		String headVarToUse = passiveSubst.containsKey(firstPred.getHeadVar()) ? 
										passiveSubst.get(firstPred.getHeadVar()) :
										firstPred.getHeadVar();
		
		prepPred.setHeadVar(headVarToUse);
		prepPred.addArgVar(secondPred.headVar);
		
		//firstPred.addArgVar(secondPred.headVar);

		
		predicateMap.put(headVarToUse+"_"+secondPred.headVar, prepPred);
		
	}
	
	void tmodHandler(TypedDependency td) {
		eventRelHandler(td, LfPredicate.predicateType.ENTITY, false);
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
	
	void detHandler(TypedDependency td)
	{
		LfPredicate nounPred =getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate detPred= getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		detPred.setHeadVar(nounPred.headVar);
		predicateMap.put(td.dep().toString(), detPred);
	}


void prtHandler(TypedDependency td)
	{
		LfPredicate verbPred =getPred(td.gov(), LfPredicate.predicateType.ENTITY);
		LfPredicate prtPred= getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		prtPred.setHeadVar(verbPred.headVar);
		predicateMap.put(td.dep().toString(), prtPred);
	}


void csubjHandler(TypedDependency td)
	{
		eventRelHandler(td, LfPredicate.predicateType.EVENT, false);
	}

void prepcHandler(TypedDependency td)
	{
		String prepLex;
		if(td.reln().toString().split("_").length > 1) {
			prepLex = td.reln().toString().split("_")[1];
		} else {
			prepLex = "";
		}
		LfPredicate prepcPred = new LfPredicate(prepLex, LfPredicate.predicateType.ENTITY);
		
		LfPredicate firstPred = getPred(td.gov(), LfPredicate.predicateType.EVENT);
		LfPredicate secondPred = getPred(td.dep(), LfPredicate.predicateType.ENTITY);
		
		String headVarToUse = passiveSubst.containsKey(firstPred.getHeadVar()) ? 
				passiveSubst.get(firstPred.getHeadVar()) :
				firstPred.getHeadVar();

				prepcPred.setHeadVar(headVarToUse);

		prepcPred.addArgVar(secondPred.headVar);
		
		predicateMap.put(headVarToUse+"_"+secondPred.headVar, prepcPred);

	}

	LfPredicate getPred(TreeGraphNode tgNode, LfPredicate.predicateType predType) {
		if(predicateMap.containsKey(tgNode.toString())) {
			return predicateMap.get(tgNode.toString());
		} else {
			LfPredicate newPred = new LfPredicate(tgNode.value(), predType);
			
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
			
			if(predicate.getLex().equals("and") ||
				predicate.getLex().equals("or")) {
					
				Boolean found = false;
				String conjArg = "";
				for(String arg: predicate.argVars) {
					if(inverseConjArgs.containsKey(arg)) {
						found = true;
						conjArg = inverseConjArgs.get(arg);
					}
				}
				
				if(!found) {
					conjArg = predicateCounter.getNextXid()+"_"+predicate.getLex();
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
					
			LfPredicate andPredicate = new LfPredicate(andConj, LfPredicate.predicateType.ENTITY);
			andPredicate.setHeadVar(andArg);
			
			String predMapKey = "";
			for(String depArg: depArgs) {
				andPredicate.addArgVar(depArg);	
				predMapKey += depArg+"_";
			}
			predMapKey = predMapKey.substring(0, predMapKey.length()-1);
			
			predicateMap.put(predMapKey, andPredicate);
			
		}
		
	}
	
	void generateNnPreds() {
		
		Map<String, List<String>> nnMap = new HashMap<String, List<String>>();
		
		for(Map.Entry<String, String> inverseNnEntry: inverseNnMap.entrySet()) {
			if(!nnMap.containsKey(inverseNnEntry.getValue())) {
				nnMap.put(inverseNnEntry.getValue(), new ArrayList<String>());
			}
			nnMap.get(inverseNnEntry.getValue()).add(inverseNnEntry.getKey());
		}
		
		for(Map.Entry<String, List<String>> nnEntry: nnMap.entrySet()) {
			LfPredicate nnPred = new LfPredicate("nn", LfPredicate.predicateType.ENTITY);
			
			nnPred.setHeadVar(nnEntry.getKey());
			
			String predMapKey = "";
			for(String nnVal: nnEntry.getValue()) {
				nnPred.addArgVar(nnVal);
				predMapKey += nnVal+"_";
			}
			
			predMapKey.substring(0, predMapKey.length()-1);
			
			predicateMap.put(predMapKey, nnPred);
		}
	}
	
	void clearNoHeadPreds() {
		List<String> predToDelete = new ArrayList<String>();
		for(Map.Entry<String, LfPredicate> entry: predicateMap.entrySet()) {
			if(entry.getValue().getHeadVar() == null) {
				predToDelete.add(entry.getKey());
			}
		}
		
		for(String predKey: predToDelete) {
			predicateMap.remove(predKey);
		}
	}
	
}

