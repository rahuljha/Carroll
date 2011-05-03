
class PredicateCounter {
	int eCount;
	int xCount;
	
	PredicateCounter() {
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