package to.etc.util;

import javax.annotation.*;

/**
 * Typed pair of two nullable integers, with proper equals and hash code.
 * Useful for all kind of integer open range models. 
 * 
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijić</a>
 * Created on Aug 10, 2016
 */
@DefaultNonNull
public class PairInteger extends Pair<Integer, Integer>{

	public PairInteger(@Nullable Integer one, @Nullable Integer two) {
		super(one, two);
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null)
			return false;
		if(getClass() != o.getClass())
			return false;
		PairInteger v = (PairInteger) o;
		Integer i1 = get1();
		Integer o1 = v.get1(); 
		if (!isEqual(i1, o1)){
			return false;
		}
		
		Integer i2 = get2(); 
		Integer o2 = v.get2();
		
		return isEqual(i2,  o2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		Integer i1 = get1();
		Integer i2 = get2(); 
		
		result = prime * result + (i1 == null ? 0 : i1.hashCode());
		result = prime * result + (i2 == null ? 0 : i2.hashCode());
		return result;
	}
	
	private static boolean isEqual(@Nullable Integer i1, @Nullable Integer i2){
		if (null == i1){
			return null == i2;
		}
		if (null == i2){
			return null == i1;
		}
		return i1.equals(i2); 
	}
	
	public String toString() {
		return get1() + "-" + get2(); 
	}

}
