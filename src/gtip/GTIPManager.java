package gtip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.apache.poi.ss.usermodel.IndexedColors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

//TODO: Ignore description, absolute match description modes

public class GTIPManager 
{


	public static void generateReport(ArrayList<MatchInfo> data) throws FileNotFoundException, IOException 
	{
		HSSFWorkbook workbook = ExcelUtil.CreateExcelWorkbook();
		HSSFSheet worksheet = ExcelUtil.CreateExcelSheet(workbook, "Sheet1");
		
		ExcelUtil.setColNames(new String[] {"RESULT", "CODE IN XLS", "DESCRIPTION IN XLS", "XLS DIRECTORY", "CODE IN XML", "DESCRIPTION IN XML", "XML DIRECTORY"}, workbook, worksheet);
		
		for(MatchInfo d : data) 
		{
			
			
			
			if(d.getPair1() == null) {d.setPair1(new Pentuple("","","","",""));}
			if(d.getPair2() == null) {d.setPair2(new Pentuple("","","","",""));}
			
			String[] dataArray = new String[]{d.getMatchLevel().toString(),(String)d.getPair1().getFirst(),(String)d.getPair1().getSecond(),(String)d.getPair1().getThird(),
					(String)d.getPair2().getFirst(),(String)d.getPair2().getSecond(),(String)d.getPair2().getThird()};
			ExcelUtil.AppendData(dataArray, workbook, worksheet);
			
			MatchLevel mLevel = d.getMatchLevel();
			
			switch(mLevel) 
			{
			case MATCHING_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.GREEN, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case MATCHING_CODE_MISMATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.LIGHT_ORANGE, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case RELATIVE_MISMATCHING_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.LIGHT_ORANGE, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case RELATIVE_MISMATCHING_CODE_MISMATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.ORANGE, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case MISMATCHING_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.ORANGE, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case MISMATCHING_CODE_MISMATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.RED, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case WRONG_CODE_TREE_MATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.RED, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case NO_CODE_MATCHING_DESCRIPTION:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.RED, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			case NEW_ENTRY:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.BROWN, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			default:
				ExcelUtil.markLastRow(ExcelUtil.genBasicCellStyle(IndexedColors.GOLD, HSSFColorPredefined.BLACK, workbook), workbook, worksheet);
				break;
			
			
			}
			
		}
		
		ExcelUtil.SaveWorkbook(workbook, "report.xls");
		
	}		
	
	public static ArrayList<MatchInfo> findDifferences(ArrayList<Pentuple> xlsData, ArrayList<Pentuple> xmlData, double descriptionClosenessTolerance) throws InterruptedException, ExecutionException 
	{
		int NUMBER_OF_THREADS = 8;
		int NUMBER_OF_ITEMS = xlsData.size();
		//TODO:Check starts with for code when finding matching descriptions, if the code has nothing to do with the description put a warning or something idfk 
		
		ArrayList<MatchInfo> matchList = new ArrayList<MatchInfo>();
		
		ExecutorService exec = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		List<Future<?>> futures = new ArrayList<Future<?>>(NUMBER_OF_ITEMS);
		
		
		
		for(Pentuple dxls : xlsData) 
		{
			futures.add(exec.submit(new Runnable() {

				@Override
				public void run() {

				MatchLevel bestLevel = MatchLevel.NEW_ENTRY;	//Worst possible state initally
			    Pentuple bestMatch = null;	//Most similar entry from the XMLs
				for(Pentuple dxml : xmlData) 
				{
					//Check similarity between the xls entry and the current xml entry
					MatchLevel level = compareEntries(dxls, dxml, descriptionClosenessTolerance);
				
					//Best possible outcome is found(perfect match), no need to continue
					if(level.best == level.level) 
					{
						bestLevel = level;
						bestMatch = dxml;
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
				matchList.add(new MatchInfo(dxls, bestMatch, bestLevel));
				System.out.println("Match added: #" + matchList.size());
			
				}}));
			
		}
		
		for(Future<?> f : futures) 
		{
			f.get();
		}
		exec.shutdown();
		System.out.println("All Matches Processed");
		
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
	public static MatchLevel compareEntries(Pentuple p1, Pentuple p2, double descriptionClosenessTolerance) 
	{
		
		if(p1.getFirst().equals("") || p2.getFirst().equals("")) 
		{
			if(LetterPairSimilarity.compareStrings((String)p1.getSecond(), (String)p2.getSecond()) > descriptionClosenessTolerance) 
			{
				return MatchLevel.NO_CODE_MATCHING_DESCRIPTION;
			}
			else 
			{
				return MatchLevel.NEW_ENTRY;
			}
		}
		
		//Check codes
		String p1Code = (String)p1.getFirst();
		String p2Code = (String)p2.getFirst();
		
		if(p1Code.equals(p2Code))
		{
			if(LetterPairSimilarity.compareStrings((String)p1.getSecond(), (String)p2.getSecond()) > descriptionClosenessTolerance) 
			{
				return MatchLevel.MATCHING_CODE_MATCHING_DESCRIPTION;
			}
			else 
			{
				return MatchLevel.MATCHING_CODE_MISMATCHING_DESCRIPTION;
			}
		}
		else if( (((String)p1.getFirst()).startsWith((String)p2.getFirst()) || ((String)p2.getFirst()).startsWith((String)p1.getFirst())) && ((((String)p1.getFirst()).length() > 2) && (((String)p2.getFirst()).length() > 2))) 
		{
			if((LetterPairSimilarity.compareStrings((String)p1.getSecond(), (String)p2.getSecond()) > descriptionClosenessTolerance)) // || ((String)p1.getSecond()).toLowerCase().contains(((String)p2.getSecond()).replace(":", "").toLowerCase()) || ((String)p2.getSecond()).toLowerCase().contains(((String)p1.getSecond()).replace(":", "").toLowerCase())
			{
				//Check tree here "WRONG_CODE_TREE_MATCHING_DESCRIPTION"
				return MatchLevel.RELATIVE_MISMATCHING_CODE_MATCHING_DESCRIPTION;
			}
			else 
			{
				return MatchLevel.RELATIVE_MISMATCHING_CODE_MISMATCHING_DESCRIPTION;
			}
		}
		else 
		{
			if(LetterPairSimilarity.compareStrings((String)p1.getSecond(), (String)p2.getSecond()) > descriptionClosenessTolerance) 
			{
				//Wrong tree check here
				if( (((String)p1.getFirst()).contains((String)p2.getFirst()) || ((String)p2.getFirst()).contains((String)p1.getFirst())) && ((((String)p1.getFirst()).length() >= 2) && (((String)p2.getFirst()).length() >= 2)))
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

	//Create a list of rows with data columns converted to strings in a string array
	private static ArrayList<String[]> getDataList(String dir) throws IOException 
	{
		HSSFWorkbook workbook = ExcelUtil.OpenExcelWorkbook(dir);
		HSSFSheet worksheet = workbook.getSheet("Sheet1");
		
		ArrayList<String[]> rowsData = ExcelUtil.ReadAllRows(workbook, worksheet, 5, worksheet.getPhysicalNumberOfRows());
		
		rowsData.removeIf(s -> (s.length <= 1));
		
		//Filter out the rowsData from empty data lines
		rowsData.removeIf(s -> 
		(s.length <= 1) ||
		(((s[0].isEmpty()) || s[0].equals(" ")) &&
		 (s[1].isEmpty() || s[1].equals(" "))));
			
		return rowsData;
	}
	
	//returns -1 if data is root If the entry has no code, use this to find its parent (finds the first entry with one less -)
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
					return j;
				}
			}	
		}
		return -1;		
	}

	
	private static String[] findParent(ArrayList<String[]> rowsData, int baseindex) 
	{
		//Find parent
		String[] parent = null;
	    int parentIndex = findParentIndex(rowsData.get(baseindex), rowsData);
	    if(parentIndex != -1) 
	    {
	    	parent = rowsData.get(parentIndex);
	    }
	    
	    return parent;
	    
	}
	
	
	static ArrayList<Pentuple> genPentuplesFromSingleXLS(String dir) throws IOException
	{
		ArrayList<String[]> rowsData = getDataList(dir);
		ArrayList<Pentuple> outputData = new ArrayList<Pentuple>();
		
		int lastRootIndex = -1;
		
		//for(String[] dataArray : rowsData) 
		for(int i = 0; i < rowsData.size(); i++)
		{
			
			String code = rowsData.get(i)[0];
			String description = rowsData.get(i)[1];
			int level = org.apache.commons.lang3.StringUtils.countMatches(description, "-");
			int selectable = -1;
			int booter = rowsData.size();
			String[] parent = findParent(rowsData, i);
		    	
			//Check the description to be correct, combine rows if necessery (if the following row descriptions doesnt contains '-')
		    int desct = 1;
		    if( !(rowsData.size() <= i || rowsData.get(i) == null))
		    {
		    	while((i != rowsData.size()-1) && (!rowsData.get(i + desct)[1].replace(" ", "").startsWith("-")) && (rowsData.get(i + desct)[0].isEmpty())) //following row "desct" doesnt contain - and code column is empty
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

			outputData.add(new Pentuple(code, description, dir, level+"", selectable+""));//currentCode, currentDescription, currentFile, currentLevel, currentSelectable
			System.out.println(code + " " + description + " " + dir + " " + level + " " + selectable + " " + i);
			//Skip desct# - 1 lines, since they are empty
			i += desct - 1;
			
		}
		
		return outputData;
	}
	
	
	/**
	 * Generates Pentuples from All files XLS files that match the format from "http://ggm.gtb.gov.tr/mevzuat/turk-gumruk-tarife-cetveli/2018-turk-gumruk-tarife-cetveli"
	 * @param scanDir
	 * @param fileNameContains Common regex that is included in every file's name
	 * @param fileNameEndsWith Format of the file (".xls"...)
	 * @return
	 * @throws IOException 
	 */
	static ArrayList<Pentuple> genPentuplesFromXLS(String scanDir, String fileNameContains, String fileNameEndsWith) throws IOException
	{
		List<String> allFiles = findFiles(scanDir, fileNameContains, fileNameEndsWith);
		ArrayList<Pentuple> pentuples = new ArrayList<Pentuple>();
		
		for(String file: allFiles) 
		{
			ArrayList<Pentuple> currentFileData = genPentuplesFromSingleXLS(file);
			pentuples.addAll(currentFileData);
		}
		
		return pentuples;
	}
	
	
	static ArrayList<Pentuple> genPentuplesFromXML(String scanDir, boolean removePeriods) throws IOException, ParserConfigurationException, SAXException {
		
		List<String> allFiles = findFiles(scanDir, "gtip", ".xml");
		ArrayList<Pentuple> Tuples = new ArrayList<Pentuple>();

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
				Tuples.add(new Pentuple<String, String, String, String, String>(currentCode, currentDescription, currentFile, currentLevel, currentSelectable));
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
