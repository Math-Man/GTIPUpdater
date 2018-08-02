package gtip;
//https://stackoverflow.com/a/6010861
public class Pentuple<T, U, V, W, Z> 
{
	
	private final T first;
    private final U second;
    private final V third;
    private final W fourth;
    private final Z fifth;
    

    public Pentuple(T first, U second, V third, W fourth, Z fifth) 
    {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
    }

    public T getFirst() { return first; }
    public U getSecond() { return second; }
    public V getThird() { return third; }
    public W getFourth() { return fourth; }
    public Z getFifth() { return fifth; }
}
