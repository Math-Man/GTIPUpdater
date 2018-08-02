package gtip;

import gtip.GTIPManager.MatchLevel;

public class MatchInfo
{
	
	private Pentuple pair1; public Pentuple getPair1() {return pair1;} public void setPair1(Pentuple p1) { pair1 = p1;}
	private Pentuple pair2; public Pentuple getPair2() {return pair2;} public void setPair2(Pentuple p2) { pair2 = p2;}
	private MatchLevel matchLevel; public MatchLevel getMatchLevel() {return matchLevel;}
	
	public MatchInfo(Pentuple p1, Pentuple p2, MatchLevel level) 
	{
		this.pair1 = p1;
		this.pair2 = p2;
		this.matchLevel = level;
	}
	
	
	
}
