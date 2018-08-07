package gtip;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

//https://stackoverflow.com/a/6010861
public class Hextuple<T, U, V, W, Z, F> 
{
	
	private final T first;
    private final U second;
    private final V third;
    private final W fourth;
    private final Z fifth;
    private final F sixth;
    

    public Hextuple(T first, U second, V third, W fourth, Z fifth, F sixth) 
    {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
        this.sixth = sixth;
    }

    public T getFirst() { return first; }
    public U getSecond() { return second; }
    public V getThird() { return third; }
    public W getFourth() { return fourth; }
    public Z getFifth() { return fifth; }
    public F getSixth() {return sixth;}
    
    public String[] toStringArray() 
    {
    	return new String[] {(String) first, (String) second, (String) third, (String) fourth, (String) fifth, (String) sixth};
    }
    
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            // if deriving: appendSuper(super.hashCode()).
            append(first).
            append(second).
            append(third).
            append(fourth).
            append(fifth).
            append(sixth).
            toHashCode();
    }
    
    
//    @Override
//    public boolean equals(Object obj) 
//    {
//    	
//    	if (!(obj instanceof Hextuple))
//            return false;
//        if (obj == this)
//            return true;
//
//        Hextuple rhs = (Hextuple) obj;
//        return new EqualsBuilder().
//            // if deriving: appendSuper(super.equals(obj)).
//            append(first, rhs.first).
//            append(second, rhs.second).
//            append(third, rhs.third).
//            append(fourth, rhs.fourth).
//            append(fifth, rhs.fifth).
//            append(sixth, rhs.sixth).
//            isEquals();
//        
//    	
//    }
    
    
    public boolean compare(Hextuple p) 
    {
    	if(this.first.equals(p.getFirst()) && this.second.equals(p.getSecond()) && this.third.equals(this.third) && this.fourth.equals(this.fourth) && this.fifth.equals(p.getFifth()) && this.sixth.equals(p.getSixth()) )
    	{
    		return true;
    	}
    	else return false;
    }
    
}
