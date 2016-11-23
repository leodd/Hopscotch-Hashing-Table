import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class HopscotchHashTable<E> implements Set<E> {

	private E[] arr;
	private int[] hop;
	
	private int occupied;
	
	private final static int INITIAL_SIZE = 13;
	private final static int LARGEST_OFFSET = 4;
	
	public HopscotchHashTable() {
		this(INITIAL_SIZE);
	}
	
	public HopscotchHashTable(int size) {
		arrayAllocation(size);
	}
	
	/**
	 * A method for testing, it will return the hash key of the given object
	 * @param e the object
	 * @return hash key of the given object
	 */
	public int hashCode(E e) {
		return hash(e);
	}
	
	/**
	 * print the whole hash table, including index, object, and the hop
	 */
	public void printTable() {
		for(int i = 0; i < arr.length; i++) {
			String string;
			
			if(arr[i] == null) {
				string = "------";
			}
			else {
				string = arr[i].toString();
			}
			
			System.out.println(i + " " + string + " " + Integer.toBinaryString(hop[i]));
		}
	}
	
	@Override
	public boolean add(E e) {
		if(e == null || contains(e)) {
			return false;
		}
		
		insert(e);
		
		return true;
	}
	
	/**
	 * insert an object into the hash table
	 * @param e the object
	 */
	private void insert(E e) {
		if(e == null) {
			return;
		}
		
		int hashKey = hash(e);
		
		int offsetIndex = findHole(hashKey);
		if(offsetIndex < 0) {
			rehash();
			insert(e);
			return;
		}
		
		while(calculateOffset(hashKey, offsetIndex) > LARGEST_OFFSET) {
			offsetIndex = moveHole(offsetIndex);
			
			if(offsetIndex < 0) {
				rehash();
				insert(e);
				return;
			}
		}
		
		arr[offsetIndex] = e;
		hop[hashKey] |= 1 << calculateOffset(hashKey, offsetIndex);
		
		occupied++;
		if(occupied >= arr.length) {
			rehash();
		}
	}
	
	/**
	 * give it the hash key of an object, it will find the nearest "hole"
	 * if there's no hole at all, return -1
	 * @param index the hash key of the object
	 * @return index of the nearest "hole"
	 */
	private int findHole(int index) {
		int res;
		
		for(int i = 0; i < arr.length; i++) {
			res = (i + index) % arr.length;
			if(arr[res] == null) {
				return res;
			}
		}
		
		return -1;
	}
	
	/**
	 * move the "hole" until it can be reach by the hop
	 * @param index the current index of "hole"
	 * @return the index of "hole" after being moved
	 */
	private int moveHole(int index) {
		int beginIndex = index - LARGEST_OFFSET;
		
		if(beginIndex < 0) {
			beginIndex += arr.length;
		}
		
		int scanIndex;
		int moveIndex;
		
		for(int i = 0; i < LARGEST_OFFSET; i++) {
			scanIndex = (beginIndex + i) % arr.length;
			
			moveIndex = findAvailableMove(scanIndex, index);
			
			if(moveIndex >= 0) {
				arr[index] = arr[moveIndex];
				arr[moveIndex] = null;
				
				int offsetMove = moveIndex - scanIndex;
				
				if(offsetMove < 0) {
					offsetMove += arr.length;
				}
				
				int offsetHole = index - scanIndex;
				
				if(offsetHole < 0) {
					offsetHole += arr.length;
				}
				
				hop[scanIndex] ^= 1 << offsetMove;
				hop[scanIndex] ^= 1 << offsetHole;
				
				return moveIndex;
			}
		}
		
		return -1;
	}
	
	/**
	 * check the hop, and find if there's element that can be moved
	 * and return the index of this element
	 * if no element can be moved, return -1
	 * @param index the index of the hop which you want to check
	 * @param holeIndex the index of the "hole"
	 * @return the index of the element which can be moved
	 */
	private int findAvailableMove(int index, int holeIndex) {
		if(hop[index] == 0) {
			return -1;
		}

		int offsetHole = holeIndex - index;
		
		if(offsetHole < 0) {
			offsetHole += arr.length;
		}
		
		for(int i = 0; i < offsetHole; i++) {
			if((hop[index] & (1 << i)) != 0) {
				return (index + i) % arr.length;
			}
		}
		
		return -1;
	}
	
	/**
	 * compute the distant between two position
	 * @param index index of lower position
	 * @param offsetIndex index of higher position
	 * @return the distant between two position
	 */
	private int calculateOffset(int index, int offsetIndex) {
		int res = offsetIndex - index;
		
		if(res < 0) {
			res += arr.length;
		}
		
		return res;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean flag = false;
		
		for(Object o : c) {
			if(add((E)o)) {
				flag = true;
			}
		}
		
		return flag;
	}

	@Override
	public void clear() {
		for(int i = 0; i < arr.length; i++) {
			arr[i] = null;
			hop[i] = 0;
		}
		
		occupied = 0;
	}

	@Override
	public boolean contains(Object o) {
		return find(o) >= 0;
	}
	
	/**
	 * find the position of the given object
	 * if it fail to find the object, return -1
	 * @param o the object
	 * @return the index of the object
	 */
	private int find(Object o) {
		int hashKey = hash(o);
		
		if(hop[hashKey] == 0) {
			return -1;
		}
		
		for(int i = 0; i <= LARGEST_OFFSET; i++) {
			int index = (hashKey + i) % arr.length;
			if(arr[index] != null && arr[index].equals(o)) {
				return index;
			}
		}
		
		return -1;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c) {
			if(!contains(o)) {return false;}
		}
		
		return true;
	}

	@Override
	public boolean isEmpty() {
		return occupied == 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new HopsctchHashTableIterator();
	}

	@Override
	public boolean remove(Object o) {
		int index = find(o);
		
		if(index < 0) {
			return false;
		}
		
		int hashKey = hash(o);
		
		arr[index] = null;
		
		int offset = index - hashKey;
		
		if(offset < 0) {
			offset += arr.length;
		}
		
		hop[hashKey] ^= 1 << offset;
		
		occupied--;
		
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean flag = false;
		
		for(Object o : c) {
			if(remove(o)) {
				flag = true;
			}
		}
		
		return flag;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean flag = false;
		
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] != null && !c.contains(arr[i])) {
				remove(arr[i]);
				flag = true;
			}
		}
		
		return flag;
	}

	@Override
	public int size() {
		return occupied;
	}

	@Override
	public Object[] toArray() {
		Object[] arr = new Object[occupied];
		
		int pointer = 0;
		
		for(Object o : this) {
			arr[pointer++] = o;
		}
		
		return arr;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if(a.length < occupied) {
			a = (T[]) new Object[occupied];
		}
		
		int pointer = 0;
		
		for(Object o : this) {
			a[pointer++] = (T) o;
		}
		
		return a;
	}
	
	/**
	 * compute the hash key of the object
	 * @param o the object
	 * @return the hash key
	 */
	private int hash(Object o) {
		int hashVal = o.hashCode() % arr.length;
		
		if(hashVal < 0) {
			hashVal += arr.length;
		}
		
		return hashVal;
	}
	
	/**
	 * rehash the table
	 */
	private void rehash() {
		E[] oldArr = arr;
		
		arrayAllocation(oldArr.length * 2);
		
		for(int i = 0; i < oldArr.length; i++) {
			insert(oldArr[i]);
		}
	}
	
	/**
	 * find the nearest prime number of the given number
	 * @param n the number
	 * @return the prime number
	 */
	private static int nextPrime( int n )
    {
        if( n % 2 == 0 )
            n++;

        for( ; !isPrime( n ); n += 2 )
            ;

        return n;
    }
	
	/**
	 * check if a number is prime
	 * @param n the number
	 * @return return true if it is prime
	 */
	private static boolean isPrime( int n )
    {
        if( n == 2 || n == 3 )
            return true;

        if( n == 1 || n % 2 == 0 )
            return false;

        for( int i = 3; i * i <= n; i += 2 )
            if( n % i == 0 )
                return false;

        return true;
    }
	
	/**
	 * allocate a new space for the hash table
	 * @param size the required size of the hash table
	 */
	private void arrayAllocation(int size) {
		int newSize = nextPrime(size);
		arr = (E[]) new Object[newSize];
		hop = new int[newSize];
		occupied = 0;
	}
	
	private class HopsctchHashTableIterator implements Iterator<E> {
		
		int remaining;
		int pointer;
		
		public HopsctchHashTableIterator() {
			remaining = occupied;
			pointer = 0;
		}

		@Override
		public boolean hasNext() {
			return remaining != 0;
		}

		@Override
		public E next() {
			if(remaining == 0) {
				return null;
			}
			
			while(arr[pointer] == null) {
				pointer++;
			}
			
			remaining--;
			
			return arr[pointer++];
		}
		
	}
}
