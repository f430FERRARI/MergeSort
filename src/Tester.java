


public class Tester {

	public static void main(String[] args) {
		LinkedList<Integer> testList = new LinkedList<Integer>(); 
		testList.append(2); 
		testList.append(1); 
		testList.append(6); 
		testList.append(4); 
		testList.append(3); 
		
		LinkedList.sort(testList);
		
		testList.print();
	} 
	
	

}
