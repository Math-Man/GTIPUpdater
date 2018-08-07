package gtip;

import gtip.GTIPManager.MatchLevel;

public class MatchInfo
{
	
	private Hextuple pair1; public Hextuple getPair1() {return pair1;} public void setPair1(Hextuple p1) { pair1 = p1;}
	private Hextuple pair2; public Hextuple getPair2() {return pair2;} public void setPair2(Hextuple p2) { pair2 = p2;}
	private MatchLevel matchLevel; public MatchLevel getMatchLevel() {return matchLevel;}
	
	public boolean displayParentCodeInstead;
	
	public MatchInfo(Hextuple p1, Hextuple p2, MatchLevel level, boolean displayParentCode) 
	{
		this.pair1 = p1;
		this.pair2 = p2;
		this.matchLevel = level;
		this.displayParentCodeInstead = displayParentCode;
	}
	
	
	
}
