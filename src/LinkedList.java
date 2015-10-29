
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import doNOTmodify.Pair;

public class LinkedList<T> implements Iterable<T> {

	public void print() {
		Node<T> currentNode = head;
		if (head == null) {
			throw new NullPointerException();
		} else {
			while (currentNode != null) {
				System.out.println(currentNode.data);
				currentNode = currentNode.next;
			}
		}
	}

	// ####################
	// # Static Functions #
	// ####################

	// We do not want the testers to have the freedom of specifying the
	// comparison function
	// Thus, we will create wrappers for them that they can use and inside these
	// wrappers
	// we will have the comparison function predefined
	// These two static wrappers, will simply call the sort method in the list
	// passed as parameter,
	// and they pass the comparison function as well

	public static <T extends Comparable<T>> void par_sort(LinkedList<T> list) {
		list.par_sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		});
	}

	public static <T extends Comparable<T>> void sort(LinkedList<T> list) {
		list.sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		});
	}

	// ############
	// # LinkList #
	// ############

	// Variables (attributes)
	// Head
	// Tail
	// Size (not required)
	// Critical Section

	private Node<T> head;
	private Node<T> tail; 
	private Lock lock;

	// Constructor
	public LinkedList() {
		// Set head and tail to null
		head = tail = null;
		// Create new instance for the critical section 
		lock = new ReentrantLock();
	}

	// Returns the size of the list
	public synchronized int size() { 
		lock.lock();
		
		if (head == null) {
			lock.unlock();
			return 0;
		} else {
			int size = 1;
			Node<T> currentNode = head;
			while (currentNode.next != null) {
				size++;
				currentNode = currentNode.next;
			} 
			lock.unlock();
			return size;
		}
	}

	// Checks if the list is empty
	public boolean isEmpty() {
		lock.lock();
		
		Boolean empty = size() == 0; 
		
		lock.unlock();
		return empty;
	}

	// Deletes all the nodes in the list
	public void clear() {
		lock.lock();
		
		head = tail = null;

		lock.unlock();
		// What if the merge sort is running now in a thread
		// I should not be able to delete the nodes (and vice versa)
		// Thus run this and everything else in a critical section
	}

	// Adds a new node to the list at the end (tail)
	public LinkedList<T> append(T t) {
		lock.lock();
		
		if (t == null) {
			throw new NullPointerException("Cannot insert null element.");
		}

		Node<T> toInsert = new Node<T>(t, null);
		if (tail != null) {
			tail.next = toInsert;
			tail = toInsert;
		} else {
			head = toInsert;
			tail = toInsert;
		}

		lock.unlock();
		return this;
	}

	// Gets a node's value at a specific index
	public T get(int index) {
		lock.lock();
		
		Node<T> found = head;
		for (int i = 0; i < size(); i++) {
			if (found == null) {
				throw new IndexOutOfBoundsException();
			}
			found = found.next;
		} 
		
		lock.unlock();
		return found.data;
	}

	@Override
	public Iterator<T> iterator() { // TODO: Does this need to be synchronized?
		Iterator<T> it = new Iterator<T>() {
			private Node<T> currentNode = head;

			@Override
			public boolean hasNext() {
				if (head == null) {
					return false;
				}
				return (currentNode.next != null);
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				currentNode = currentNode.next; // Watch this
				return currentNode.data; // This returns next T not next
											// node
			}
		};
		return it;
	}

	// The next two functions, are being called by the static functions at the
	// top of this page
	// These functions are just wrappers to prevent the static function from
	// deciding which
	// sorting algorithm it should use.
	// This function will decide which sorting algorithm it should use
	// (we only have merge sort in this assignment)

	// Sorts the link list in serial
	private void sort(Comparator<T> comp) { // TODO: Does this need to be
											// synchronized?

		new MergeSort<T>(comp).sort(this); // Run this within the critical
											// section (as discussed before)

		// It might not allow you to use this inside critical
		// Create a final pointer = this then use that pointer
	}

	// Sorts the link list in parallel (using multiple threads)
	private void par_sort(Comparator<T> comp) { // TODO: Does this need to be
												// synchronized?
		new MergeSort<T>(comp).parallel_sort(this); // Run this within the
													// critical section (as
													// discussed before)
	}

	// Merge sort
	static class MergeSort<T> {

		// Variables (attributes)
		// ExecutorService
		// Depth limit //TODO: How do you get the depth limit?

		ExecutorService eService;

		// Comparison function
		final Comparator<T> comp;

		// Constructor
		public MergeSort(Comparator<T> comp) {
			this.comp = comp;
		}

		// #####################
		// # Sorting functions #
		// #####################
		// The next two functions will simply call the correct function
		// to merge sort the link list and then they will fix its
		// attributes (head and tail pointers)

		public void sort(LinkedList<T> list) {
			LinkedList<T> result = mergeSort(list);
			list.head = result.head;
		}

		public void parallel_sort(LinkedList<T> list) { 
			int threadCount = 4;
			int depth = (int) Math.floor(Math.log10(threadCount)/Math.log10(2));
			
			try {
				eService = Executors.newFixedThreadPool(4); 
				LinkedList<T> result = parallel_mergesort(list, depth);
				list.head = result.head;
				eService.shutdown(); // TODO: This necessary?
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		// #########
		// # Steps #
		// #########

		// The main merge sort function (parrallel_msort and msort)
		// Split the list to two parts
		// Merge sort each part
		// Merge the two sorted parts together

		public LinkedList<T> parallel_mergesort(LinkedList<T> list, int depth)
				throws InterruptedException, ExecutionException {
			if (depth == 0) {
				return mergeSort(list);
			}

			Pair<LinkedList<T>, LinkedList<T>> splitLists = split(list);
			LinkedList<T> firstPart = splitLists.fst();
			LinkedList<T> secondPart = splitLists.snd();

			Future<LinkedList<T>> futurePartOne = eService.submit(new Callable<LinkedList<T>>() {
				public LinkedList<T> call() throws Exception {
					return parallel_mergesort(firstPart, depth - 1); 
				}
			});

			Future<LinkedList<T>> futurePartTwo = eService.submit(new Callable<LinkedList<T>>() {
				public LinkedList<T> call() throws Exception {
					return parallel_mergesort(secondPart, depth - 1); 
				}
			});

			LinkedList<T> sortedFirst = futurePartOne.get();
			LinkedList<T> sortedSecond = futurePartTwo.get();
			return merge(sortedFirst, sortedSecond);
		}

		public LinkedList<T> mergeSort(LinkedList<T> list) {
			if (list.head == null || list.head.next == null) {
				return list;
			}
			Pair<LinkedList<T>, LinkedList<T>> splitLists = split(list);
			LinkedList<T> firstHalf = splitLists.fst();
			LinkedList<T> secondHalf = splitLists.snd();

			return merge(mergeSort(firstHalf), mergeSort(secondHalf));
		}

		// Splitting function
		// Run two pointers and find the middle of the a specific list
		// Create two new lists (and break the link between them)
		// It should return pair (the two new lists)

		public Pair<LinkedList<T>, LinkedList<T>> split(LinkedList<T> list) {
			// No null checker
			Node<T> pointer1, pointer2;
			pointer1 = list.head;
			pointer2 = pointer1.next;
			while (pointer2 != null && pointer2.next != null) {
				pointer1 = pointer1.next;
				pointer2 = pointer2.next.next;
			}

			LinkedList<T> secondPart = new LinkedList<>();
			secondPart.head = pointer1.next; // Creates new list  
			//secondPart.tail = list.tail;
			//list.tail = pointer1; 	//Sets tail for first list
			pointer1.next = null; // Cuts off backend of front list

			return new Pair<LinkedList<T>, LinkedList<T>>(list, secondPart);
		}

		// Merging function
		// 1- Keep comparing the head of the two link lists
		// 2- Move the smallest node to the new merged link list
		// 3- Move the head on the list that lost this node

		// 4- Once one of the two lists is done, append the rest of the
		// second list to the tail of the new merged link list

		public LinkedList<T> merge(LinkedList<T> firstList, LinkedList<T> secondList) {

			Node<T> temporaryHead, currentNode, firstNode, secondNode;
			temporaryHead = new Node<T>(null, null);
			currentNode = temporaryHead;
			firstNode = firstList.head;
			secondNode = secondList.head;

			while (firstNode != null && secondNode != null) {
				if ((comp.compare(firstNode.data, secondNode.data)) == -1) {
					currentNode.next = firstNode;
					firstNode = firstNode.next;
				} else {
					currentNode.next = secondNode;
					secondNode = secondNode.next;
				}
				currentNode = currentNode.next;
			}
			
			currentNode.next = (firstNode == null) ? secondNode : firstNode;
			LinkedList<T> sortedList = new LinkedList<>();
			
			/*if (firstNode == null) { 
				currentNode.next = secondNode; 
				while (secondNode.next != null) { 
					secondNode = secondNode.next;
				} 
				sortedList.tail = secondNode; 
			} else { 
			
				currentNode.next = firstNode; 
				while (firstNode.next != null) { 
					firstNode = firstNode.next;
				} 
				sortedList.tail = firstNode; 
			}*/
			
			sortedList.head = temporaryHead.next;
			
			return sortedList;
		}
	}

	// #########
	// # Node #
	// #########
	private static class Node<T> {
		T data;
		Node<T> next;

		public Node(T data, Node<T> next) {
			this.data = data;
			this.next = next;
		}
	}

}
