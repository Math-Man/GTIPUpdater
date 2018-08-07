package gtip;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class LetterPairSimilarity {
	  
	//http://www.catalysoft.com/articles/StrikeAMatch.html
	private static String[] letterPairs(String str) {
		       int numPairs = str.length()-1;  
		       
		       //This line prevents crashing when \n \r or " " follows in a sequence.
		       if(numPairs <= 0){return new String[0];}
		       
		       String[] pairs = new String[numPairs];
		       for (int i=0; i<numPairs; i++) {
		           pairs[i] = str.substring(i,i+2);
		       }
		       return pairs;
		   }
	
	/** @return an ArrayList of 2-character Strings. */
	   private static ArrayList wordLetterPairs(String str) {
	       ArrayList allPairs = new ArrayList();
	       // Tokenize the string and put the tokens/words into an array
	       String[] words = str.split("\\s");
	       // For each word
	       for (int w=0; w < words.length; w++) {
	           // Find the pairs of characters
	           String[] pairsInWord = letterPairs(words[w]);
	           for (int p=0; p < pairsInWord.length; p++) {
	               allPairs.add(pairsInWord[p]);
	           }
	       }
	       return allPairs;
	   }
	   
	   /** @return lexical similarity value in the range [0,1] */
	      public static double compareStrings(String str1, String str2) {
	    	  
	    	  //Instant st= Instant.now();
	    	  
	          ArrayList pairs1 = wordLetterPairs(str1.toUpperCase());
	          ArrayList pairs2 = wordLetterPairs(str2.toUpperCase());
	          int intersection = 0;
	          int union = pairs1.size() + pairs2.size();
	          for (int i=0; i<pairs1.size(); i++) {
	              Object pair1=pairs1.get(i);
	              for(int j=0; j<pairs2.size(); j++) {
	                  Object pair2=pairs2.get(j);
	                  if (pair1.equals(pair2)) {
	                      intersection++;
	                      pairs2.remove(j);
	                      break;
	                  }
	              }
	          }
	          //System.out.println("Compare time: " + Duration.between(st, Instant.now()));
	          return (2.0*intersection)/union;
	      }
	
}
