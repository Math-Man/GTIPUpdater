package gtip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.apache.commons.text.similarity.*;

public class GTIPManager 
{
	/**
	 * Generates an excel file from the given matchinfo list.
	 * @param data
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void generateReport(ArrayList<MatchInfo> data) throws FileNotFoundException, IOException 
	{
		HSSFWorkbook workbook = ExcelUtil.CreateExcelWorkbook();
		HSSFSheet worksheet = ExcelUtil.CreateExcelSheet(workbook, "Sheet1");
		
		ExcelUtil.setColNames(new String[] {"RESULT", "CODE IN XLS", "DESCRIPTION IN XLS", "XLS DIRECTORY", "CODE IN XML", "DESCRIPTION IN XML", "XML DIRECTORY"}, workbook, worksheet);
		
		/* Excel has a hard cap on the number of cell styles it can have in a single workbook (around 4000) so it is necessery to create the styles outside the loop*/
		CellStyle[] styles = new CellStyle[] {
		ExcelUtil.genBasicCellStyle(IndexedColors.GREEN, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.LIGHT_ORANGE, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.ORANGE, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.RED, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.MAROON, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.PINK, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.DARK_RED, HSSFColorPredefined.BLACK, workbook),
		ExcelUtil.genBasicCellStyle(IndexedColors.YELLOW, HSSFColorPredefined.BLACK, workbook)
		};
		
		
		for(MatchInfo d : data) 
		{
			if(d.getPair1() == null) {d.setPair1(new Hextuple("","","","","",""));}
			if(d.getPair2() == null) {d.setPair2(new Hextuple("","","","","",""));}
			
			String[] dataArray=null;
			
			if(d.displayParentCodeInstead && d.getPair1().getSixth() != null && ((String[])d.getPair1().getSixth())[0] != null) 
			{
				String parentCode = (String)((String[])d.getPair1().getSixth())[0].replace(".", "");
				dataArray = new String[]{d.getMatchLevel().toString(), "PARENT CODE: "+parentCode, (String)d.getPair1().getSecond(),(String)d.getPair1().getThird(),
						(String)d.getPair2().getFirst(),(String)d.getPair2().getSecond(),(String)d.getPair2().getThird()};
			}
			else 
			{
				dataArray = new String[]{d.getMatchLevel().toString(),(String)d.getPair1().getFirst(),(String)d.getPair1().getSecond(),(String)d.getPair1().getThird(),
						(String)d.getPair2().getFirst(),(String)d.getPair2().getSecond(),(String)d.getPair2().getThird()};
			}

			MatchLevel mLevel = d.getMatchLevel();
			switch(mLevel) 
			{
			case MATCHING_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[0], workbook, worksheet);
				break;
			case MATCHING_CODE_MISMATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[7], workbook, worksheet);
				break;
			case RELATIVE_MISMATCHING_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[1], workbook, worksheet);
				break;
			case RELATIVE_MISMATCHING_CODE_MISMATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[2], workbook, worksheet);
				break;
			case MISMATCHING_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[3], workbook, worksheet);
				break;
			case MISMATCHING_CODE_MISMATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[3], workbook, worksheet);
				break;
			case WRONG_CODE_TREE_MATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[3], workbook, worksheet);
				break;
			case NO_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.AppendAndStylize(dataArray, styles[6], workbook, worksheet);
				break;
			case NEW_ENTRY:
				ExcelUtil.AppendAndStylize(dataArray, styles[4], workbook, worksheet);
				break;
			default:
				ExcelUtil.AppendAndStylize(dataArray, styles[5], workbook, worksheet);
				break;
			}
		}
		
		worksheet.createFreezePane(0, 1);
		ExcelUtil.SaveWorkbook(workbook, System.getProperty("user.dir")+"\\report.xls");
	}		
	
	/**
	 * Compares XLS and XML files and measures the difference by using matchlevel enum
	 * @param xlsData
	 * @param xmlData
	 * @param descriptionClosenessTolerance
	 * @param useLevenshteinDistance
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static ArrayList<MatchInfo> findDifferences(ArrayList<Hextuple> xlsData, ArrayList<Hextuple> xmlData, double descriptionClosenessTolerance, boolean useLevenshteinDistance) throws InterruptedException, ExecutionException 
	{
		int NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors() + 1;
		int NUMBER_OF_ITEMS = xlsData.size();

		ArrayList<MatchInfo> matchList = new ArrayList<MatchInfo>();
		
		ExecutorService exec = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		List<Future<?>> futures = new ArrayList<Future<?>>(NUMBER_OF_ITEMS);
		
		Instant starts = Instant.now();
		
		CopyOnWriteArraySet<Hextuple> xlsSuper = new CopyOnWriteArraySet<Hextuple>(xlsData);
		CopyOnWriteArraySet<Hextuple> xmlSuper = new CopyOnWriteArraySet<Hextuple>(xmlData);
		
		for(Hextuple dxls : xlsSuper) 
		{
			futures.add(exec.submit(new Runnable() {
			
				
				Instant t_s = Instant.now();
				@Override
				public void run() {
					
				MatchLevel bestLevel = MatchLevel.NEW_ENTRY;	//Worst possible state initally
			    Hextuple bestMatch = null;	//Most similar entry from the XMLs
				for(Hextuple dxml : xmlSuper) 
				{
					//Check similarity between the xls entry and the current xml entry
					MatchLevel level = compareEntries(dxls, dxml, descriptionClosenessTolerance, useLevenshteinDistance);
					
					//Best possible outcome is found(perfect match), no need to continue
					if(level.best == level.level || level == level.MATCHING_CODE_MISMATCHING_DESCRIPTION) 
					{
						bestLevel = level;
						bestMatch = dxml;
						//Remove found data to make the iterations faster
						xmlSuper.remove(dxml);
						//xlsSuper.remove(dxls);
						break;
					}
					
					//Some error is found
					if(level.level < level.best) 
					{
						//is the current error is considered better than the previous error
						if(level.level > bestLevel.level) 
						{
							//If so update the best error and best match
							bestLevel = level;
							bestMatch = dxml;
						}
					}	
				}
				MatchInfo minf = new MatchInfo(dxls, bestMatch, bestLevel, false);
				if(bestLevel == MatchLevel.NO_CODE_MATCHING_DESCRIPTION) {minf.displayParentCodeInstead = true;}
				else if(bestLevel == MatchLevel.NEW_ENTRY && ((String)minf.getPair1().getFirst()).isEmpty()) {minf.displayParentCodeInstead = true;}
				
				matchList.add(minf);
				System.out.println("Match added: #" + matchList.size());
				System.out.println("Time elapsed: " + Duration.between(t_s, Instant.now()));
				}}));
			
		}
		
		for(Future<?> f : futures) 
		{
			f.get();
		}
		exec.shutdown();
		System.out.println("All Matches Processed");
		Instant ends = Instant.now();
		System.out.println("Time elapsed: " + Duration.between(starts, ends));
		
		return matchList;
	}
	
	//currentCode, currentDescription, currentFile, currentLevel, currentSelectable
	/**
	 * Finds how relative two entries are to each others
	 * @param p1
	 * @param p2
	 * @param descriptionClosenessTolerance
	 * @return
	 */
	public static MatchLevel compareEntries(Hextuple p1, Hextuple p2, double descriptionClosenessTolerance, boolean useLevenshteinDistance) 
	{
		//Check codes
		String p1Code = (String)p1.getFirst();
		String p2Code = (String)p2.getFirst();
		
		double stringDiff = 0;
		if(useLevenshteinDistance) {LevenshteinDistance dist = new LevenshteinDistance(); stringDiff = stringSimilarity((String)p1.getSecond(), (String)p2.getSecond(), dist);}
		else {stringDiff = LetterPairSimilarity.compareStrings((String)p1.getSecond(), (String)p2.getSecond()); }

		if(p1Code == null  || p2Code == null || p1Code.equals("") || p2Code.equals("")) 
		{
			if(stringDiff > descriptionClosenessTolerance) 
			{
				return MatchLevel.NO_CODE_MATCHING_DESCRIPTION;
			}
			else 
			{
				return MatchLevel.NEW_ENTRY;
			}
		}
		else if(p1Code.equals(p2Code))
		{
			if(stringDiff > descriptionClosenessTolerance) 
			{
				return MatchLevel.MATCHING_CODE_MATCHING_DESCRIPTION;
			}
			else 
			{
				return MatchLevel.MATCHING_CODE_MISMATCHING_DESCRIPTION;
			}
		}
		else if( ((p1Code).startsWith(p2Code) || (p2Code).startsWith(p1Code)) && (((p1Code).length() > 2) && ((p2Code).length() > 2))) 
		{
			if(stringDiff > descriptionClosenessTolerance) 
			{
				return MatchLevel.RELATIVE_MISMATCHING_CODE_MATCHING_DESCRIPTION;
			}
			else 
			{
				return MatchLevel.RELATIVE_MISMATCHING_CODE_MISMATCHING_DESCRIPTION;
			}
		}
		else 
		{
			if(stringDiff > descriptionClosenessTolerance) 
			{
				if( ((p1Code).contains(p2Code) || (p2Code).contains(p1Code)) && (((p1Code).length() > 2) && ((p2Code).length() > 2)))
				{
					return MatchLevel.WRONG_CODE_TREE_MATCHING_DESCRIPTION;
				}
				else 
				{
					return MatchLevel.MISMATCHING_CODE_MATCHING_DESCRIPTION;
				}

			}
			else 
			{
				return MatchLevel.MISMATCHING_CODE_MISMATCHING_DESCRIPTION;
			}
		}
	}
	
	/**
	 * enums with positive values determine finalizing find, a certain outcome (something that will stop the search)
	 * enums with negative values determine an uncertain find, a possible outcome but not final, 
	 * higher the value more likely the outcome
	 * @author Goktug.Kayacan
	 *
	 */
	public enum MatchLevel
	{
		MATCHING_CODE_MATCHING_DESCRIPTION(5),
		MATCHING_CODE_MISMATCHING_DESCRIPTION(4),
		RELATIVE_MISMATCHING_CODE_MATCHING_DESCRIPTION(3),
		RELATIVE_MISMATCHING_CODE_MISMATCHING_DESCRIPTION(-2),
		MISMATCHING_CODE_MATCHING_DESCRIPTION(-1),
		MISMATCHING_CODE_MISMATCHING_DESCRIPTION(-3),
		WRONG_CODE_TREE_MATCHING_DESCRIPTION(2),
		
		NO_CODE_MATCHING_DESCRIPTION(1),
		NEW_ENTRY(-4);
		
		private final int level;
		public final int best = 5;
		public final int worst = -4;
		MatchLevel(int level) { this.level = level; }
	    public int getValue() { return level; }
	    //public int findByValue(int i) {for(MatchLevel value : this.values()) {if(value.getValue() == i) {return value.getValue();}}return Integer.MIN_VALUE;}
	}

	/**
	 * Create a list of rows with data columns converted to strings in a string array
	 * @param dir
	 * @param startingRow
	 * @return
	 * @throws IOException
	 */
	private static ArrayList<String[]> getDataList(String dir, int startingRow) throws IOException 
	{
		HSSFWorkbook workbook = ExcelUtil.OpenExcelWorkbook(dir);
		HSSFSheet worksheet = workbook.getSheetAt(0);
		
		ArrayList<String[]> rowsData = ExcelUtil.ReadAllRows(workbook, worksheet, startingRow, worksheet.getPhysicalNumberOfRows());
		ArrayList<String[]> filteredData = new ArrayList<String[]>();
		
		//bug workaround, check if the code column has a non-numeric character, if so push the column to description and leave the code column blank
		for(String[] s : rowsData) 
		{
			if(s.length > 0) 
			{
				s[0] = s[0].replaceAll("\\s+", "").replaceAll("[()]", "").replace("[", "").replace("]", "");
				if((!s[0].matches("[0-9.]+")) && s[0].length() > 0) 
				{
					s[0] = s[0].replaceAll("-", "");
					ArrayList<String> ar = new ArrayList<>(Arrays.asList(s));
					ar.add(0, "");
					
					s = ar.toArray(new String[ar.size()]);
				}
			}
			filteredData.add(s);
			
		}
		
		rowsData.clear();
		rowsData.addAll(filteredData);
		rowsData.removeIf(s -> (s.length <= 1));
		
		//Filter out the rowsData from empty data lines
		rowsData.removeIf(s -> 
		(s.length <= 1) ||
		(((s[0].isEmpty()) || s[0].equals(" ")) &&
		(s[1].isEmpty() || s[1].equals(" "))));
			
		return rowsData;
	}
	/*
	//returns -1 if data is root If the entry has no code (finds the first entry with one less -)
	private static int findParentIndex(String[] data, ArrayList<String[]> dataList) 
	{
		int baseIndex = -1;
		int dataLevel = org.apache.commons.lang3.StringUtils.countMatches(data[1], "-");
		
		for (int j = dataList.size() - 1; j >= 0; j--) 
		{
			if((baseIndex == -1)) 
			{
				if((Arrays.equals(data, dataList.get(j))))
				{
					baseIndex = j;
				}	
			}			
			else //If baseIndex is found, find the first entry with the less "-" than the one at base index
			{
				int currentLevel = org.apache.commons.lang3.StringUtils.countMatches(dataList.get(j)[1], "-"); 
				if(currentLevel < dataLevel) //If the current level is lower than the original data level, we found the parent
				{
					//Check for multiline description, check rows with lower indexes if there is no code
					int dec = 0;
					while( ((j - dec >= 0))) 
					{
						if( dataList.get(j - dec)[0] == "" || dataList.get(j- dec)[0] == null) //If it has no code, increment dec
						{
							dec++;
						}
						else//If it has a code, return it 
						{
							//currentLevel = org.apache.commons.lang3.StringUtils.countMatches(dataList.get(j-dec)[1], "-"); 
							return j - dec;
						}
						
					}
					
					return j;
				}
			}	
		}
		return -1;		
	}
	*/
	/**
	 * Finds the given data's parent's index in terms of rows
	 * @param data
	 * @param dataList
	 * @return
	 */
	private static int findParentIndex(String[] data, ArrayList<String[]> dataList) 
	{
		int data_index = -1;
		int dataLevel = org.apache.commons.lang3.StringUtils.countMatches(data[1], "-");//Level is indicated by the number of dashes at the start of the descripiton
		
		//Find data index
		for (int j = dataList.size() - 1; j >= 0; j--) 
		{
			if((Arrays.equals(data, dataList.get(j))))
			{
				data_index = j;
				break;
			}	
		}
		
		//If no index is found, something is wrong
		if(data_index == -1) {return -1;}
		
		
		//Find the parent index
		for(int j = data_index; j >= 0; j--) 
		{
			//Check if the current data at j starts with a code (not null not empty and contains atleast one '.')
			if((dataList.get(j)[0] != null) && (!dataList.get(j)[0].isEmpty()) && ((dataList.get(j)[0].contains(".")))) 
			{

				int currentDataLevel = -1;
				String[] substringsofchars = dataList.get(j)[1].split("[a-zA-Z]");
				if(substringsofchars.length > 0) 
				{
					currentDataLevel = org.apache.commons.lang3.StringUtils.countMatches(substringsofchars[0], "-");
				}
				else
				{
					currentDataLevel = 0;
				}

				//if the level is lower than what we started with, we found the parent (lack of code eliminates extended )
				if(currentDataLevel < dataLevel) 
				{
					return (j);
				}
				
			}
			
			
		}
		return -1;
		
		
		
		
	}

	/**
	 * Levenshtein string distance
	 * @param s1
	 * @param s2
	 * @param dist
	 * @return
	 */
	public static double stringSimilarity(String s1, String s2, LevenshteinDistance dist) 
	{
		  String longer = s1, shorter = s2;
		  if (s1.length() < s2.length()) 
		  { 
		    longer = s2; shorter = s1;
		  }
		  int longerLength = longer.length();
		  if (longerLength == 0) { return 1.0;}
		  return (longerLength -  dist.apply(longer, shorter)) / (double) longerLength;
	}
	
	/**
	 * Finds the parent data, given the index of the base
	 * @param rowsData
	 * @param baseindex
	 * @return
	 */
	private static String[] findParent(ArrayList<String[]> rowsData, int baseindex) 
	{
		//Find parent
		String[] parent = null;
	    int parentIndex = findParentIndex(rowsData.get(baseindex), rowsData);
	    if(parentIndex != -1) 
	    {
	    	parent = rowsData.get(parentIndex);
	    	
	    	//Find parent until it has a code
			while( parent != null && (parent[0] == "" || parent[0] == null)) 
			{
				parent = findParent(rowsData, parentIndex);
			}
	    }
	    return parent;
	    
	}
	
	/**
	 * Generates Hextuples from the given XLS GTIP file
	 * @param dir
	 * @param startingrow
	 * @return
	 * @throws IOException
	 */
	static ArrayList<Hextuple> genHextuplesFromSingleXLS(String dir, int startingrow) throws IOException
	{
		ArrayList<String[]> rowsData = getDataList(dir, startingrow);
		ArrayList<Hextuple> outputData = new ArrayList<Hextuple>();
		
		boolean firstLineFound = false;
		
		//for(String[] dataArray : rowsData) 
		for(int i = 0; i < rowsData.size(); i++)
		{
			if((!firstLineFound)) 
			{
				if( (rowsData.get(i)[0].contains("."))) {firstLineFound=true;}//This line attempts to skip excel rows until the first actual line with any code is found
				else { continue; }
			}
		
			
			String code = rowsData.get(i)[0];
			String description = rowsData.get(i)[1];
			int level = org.apache.commons.lang3.StringUtils.countMatches(description, "-");
			int selectable = -1;
			String[] parent = findParent(rowsData, i);	//Find parent of the current item
			
			
			//Check the description to be correct, combine rows if necessery (if the following row descriptions doesnt contains '-')
		    int desct = 1;
		    if( !(rowsData.size() <= i || rowsData.get(i) == null))
		    {
		    	while((i != rowsData.size()-1) && (!(rowsData.get(i + desct)[1].replaceAll(" ", "").replaceAll("'", "").startsWith("-"))) && (rowsData.get(i + desct)[0].isEmpty())) //following row "desct" doesnt contain - and code column is empty
				{
					description += rowsData.get(i + desct)[1];
					desct++;
					
					if(i + desct >= rowsData.size() || rowsData.get(i + desct) == null) {break;}
				}
		    }
			
			//Normalize Description
			if(description.contains("-")) 
			{
				String[] splitDesc = description.split("- ");
				description = splitDesc[splitDesc.length-1];	//Grab the last one
			}
			
			//Normalize Code by removing the dot
			if(code.contains(".")) 
			{
				code = code.replace(".", "");
			}
			
			if(rowsData.get(i)[0].isEmpty()) 
			{
				selectable = 0;
			}
			else 
			{
				selectable = 1;
			}

			Hextuple pip = new Hextuple(code, description, dir, level+"", selectable+"", parent);//parent[1]);
			outputData.add(pip);//currentCode, currentDescription, currentFile, currentLevel, currentSelectable
			
			//Skip desct# - 1 lines, since they are empty
			i += desct - 1;
		}
		return outputData;
	}
	
	
	/**
	 * Generates Hextuples from All files XLS files that match the format from "http://ggm.gtb.gov.tr/mevzuat/turk-gumruk-tarife-cetveli/2018-turk-gumruk-tarife-cetveli"
	 * @param scanDir
	 * @param fileNameContains Common regex that is included in every file's name
	 * @param fileNameEndsWith Format of the file (".xls"...)
	 * @return
	 * @throws IOException 
	 */
	static ArrayList<Hextuple> genHextuplesFromXLS(String scanDir, String fileNameContains, String fileNameEndsWith) throws IOException
	{
		List<String> allFiles = findFiles(scanDir, fileNameContains, fileNameEndsWith);
		ArrayList<Hextuple> Hextuples = new ArrayList<Hextuple>();
		
		for(String file: allFiles) 
		{
			//Some excel files randomly start at a different index.
			int startRow = 5;
			if(file.contains("99")) {startRow = 9;}
			if(file.contains("29")) {startRow = 8;}
			ArrayList<Hextuple> currentFileData = genHextuplesFromSingleXLS(file, startRow);
			Hextuples.addAll(currentFileData);
		}
		
		return Hextuples;
	}
	
	/**
	 * Creates hextuples from the given XML GTIP file
	 * @param scanDir
	 * @param removePeriods
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	static ArrayList<Hextuple> genHextuplesFromXML(String scanDir, boolean removePeriods) throws IOException, ParserConfigurationException, SAXException {
		
		List<String> allFiles = findFiles(scanDir, "gtip", ".xml");
		ArrayList<Hextuple> Tuples = new ArrayList<Hextuple>();

		for (String file : allFiles) {
			
			String currentCode = "";
			String currentDescription = "";
			String currentFile = file;
			String currentLevel = "";
			String currentSelectable = "";

			File inputFile = new File(file);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("GTIP");
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					
					Element eElement = (Element) nNode;
					for (int j = 0; j < eElement.getChildNodes().getLength(); j++) {
						Node currentNode = eElement.getChildNodes().item(j);
						if (!(currentNode instanceof Text)) {
							if (currentNode.getNodeName().equals("CODE")) {currentCode = currentNode.getTextContent(); if(removePeriods) {currentCode = currentCode.replace(".", "");} }
							else if (currentNode.getNodeName().equals("LEVEL_")) {currentLevel = currentNode.getTextContent();}
							else if (currentNode.getNodeName().equals("SELECTABLE")) {currentSelectable = currentNode.getTextContent();}
							else if (currentNode.getNodeName().equals("DESCRIPTIONS")) {
								for (int k = 0; k < currentNode.getChildNodes().getLength(); k++) {
									if (!(currentNode instanceof Text)) {
										
										NodeList altNodes = currentNode.getChildNodes().item(k).getChildNodes();
										for (int t = 0; t < altNodes.getLength(); t++) {
											if (!(currentNode instanceof Text)) {
												if (altNodes.item(t).getNodeName().equals("DESCR")) {
													String str = altNodes.item(t).getTextContent();

													currentDescription = str;
												}
											}
										}
										
									}
								}
							}
						}
					}
					
				}
				//Add all tuples from all files to the list
				Tuples.add(new Hextuple<String, String, String, String, String, String>(currentCode, currentDescription, currentFile, currentLevel, currentSelectable, ""));
			}
		}
		return Tuples;
	}
	
	
	/**
	 * Finds all files under given scandir that contains the given pattern and ends with tail (".xls", ".xml", ...)
	 * @param scanDir
	 * @return List of files
	 * @throws IOException
	 */
	static List<String> findFiles(String scanDir, String pattern, String tail) throws IOException {
		Stream<Path> paths = Files.walk(Paths.get(scanDir));
		try 
		{
			List<String> files = paths.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().toLowerCase().contains(pattern))
					.filter(p -> p.getFileName().toString().toLowerCase().contains(tail)).map(p -> p.toString())
					.collect(Collectors.toList());
			return files;
		} 
		finally 
		{
			if (null != paths)
			{
				paths.close();
			}
		}
		
	}
	

	
	
}
