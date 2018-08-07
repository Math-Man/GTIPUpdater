package gtip;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

public class Main {

	public static void main(String[] args) throws IOException, AWTException, ParserConfigurationException, SAXException, InterruptedException, ExecutionException, TransformerException {
		// TODO Auto-generated method stub
		
		ArrayList<Hextuple> ott = GTIPManager.genHextuplesFromSingleXLS("2018012.xls", 5);
		
		
		
		GTIPBuilder builder = new GTIPBuilder(true);
		//builder.createEntry(ott.get(0));
		builder.createEntriesFromList(ott);
		builder.saveXml("test.xml");
		
		
		List<String> allFiles = GTIPManager.findFiles("D:\\Projects\\GTIPW\\GTIPUpdater\\XLSFiles", "fasıl", ".xls");
		
		int indx = 0;
		for(String file : allFiles) 
		{
			int startRow = 5;
			if(file.contains("99")) {startRow = 9;}
			if(file.contains("29")) {startRow = 8;}
			
			ArrayList<Hextuple> ouchie = GTIPManager.genHextuplesFromSingleXLS(file, startRow);
			builder.createEntriesFromList(ouchie);
			builder.saveXml("D:\\Projects\\GTIPW\\GTIPUpdater\\generatedGTIP\\GTIP" + indx + ".xml");
			builder.wipeXml();
			indx++;
		}
		
		
		ArrayList<Hextuple> dat = GTIPManager.genHextuplesFromXLS("D:\\Projects\\GTIPW\\GTIPUpdater\\XLSFiles", "fasıl", ".xls");
		
		ArrayList<Hextuple> tot = GTIPManager.genHextuplesFromXML("D:\\Projects\\jguar_GIT_Set\\jprod\\UnityServer\\WebContent\\resources\\TRTR",true);
		
		GTIPManager.compareEntries(dat.get(0), tot.get(121), 0.7, true);
		
		
		ArrayList<MatchInfo> diffs = GTIPManager.findDifferences(dat, tot, 0.85, true);
		
		GTIPManager.generateReport(diffs);
		
		tot.clear();
		ott.clear();
		/*
		HSSFWorkbook workbook = ExcelUtil.OpenExcelWorkbook("2018012.xls");
		HSSFSheet worksheet = workbook.getSheet("Sheet1");
		
		ArrayList<String[]> rowsData = ExcelUtil.ReadAllRows(workbook, worksheet, 5, worksheet.getPhysicalNumberOfRows());
		
		rowsData.removeIf(s -> 
						((s[0].isEmpty()) || s[0].equals(" ")) &&
						 (s[1].isEmpty() || s[1].equals(" ")));
		
		
		
		findParentIndex(rowsData.get(12), rowsData);
		
		for(int i = 0; i < rowsData.size(); i++)
		{
		String description = rowsData.get(i)[1];
		//Check the description to be correct, combine rows if necessery (if the following row descriptions doesnt contains '-')
		int desct = 1;
		while( (!rowsData.get(i + desct)[1].replace(" ", "").startsWith("-")) && (rowsData.get(i + desct)[0].isEmpty())) //following row "desct" doesnt contain - and code column is empty
		{
			description += rowsData.get(i + desct)[1];
			desct++;
		}
		
		if(description.contains("-")) 
		{
			String[] splitDesc = description.split("- ");
			description = splitDesc[splitDesc.length-1];	//Grab the last one
		}
		
		i += desct - 1;
		}
		//skip desct number of rows
		
		*/
		
		
		
		//org.apache.commons.io.FileUtils.copyURLToFile(new URL("https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=4&ved=2ahUKEwiK18mGmMncAhVLUlAKHdPlDHkQFjADegQIBBAC&url=https%3A%2F%2Fol.baker.edu%2Fwebapps%2Fdur-browserCheck-bb_bb60%2Fsamples%2Fsample.xlsx&usg=AOvVaw3kB6F7vtKidr35vh64gzJb"), new File("file2.xls"));
		
		//Document doc = Jsoup.connect("https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=4&ved=2ahUKEwiK18mGmMncAhVLUlAKHdPlDHkQFjADegQIBBAC&url=https%3A%2F%2Fol.baker.edu%2Fwebapps%2Fdur-browserCheck-bb_bb60%2Fsamples%2Fsample.xlsx&usg=AOvVaw3kB6F7vtKidr35vh64gzJb").get();
//		Response resultImageResponse = Jsoup
//                .connect("https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=4&ved=2ahUKEwiK18mGmMncAhVLUlAKHdPlDHkQFjADegQIBBAC&url=https%3A%2F%2Fol.baker.edu%2Fwebapps%2Fdur-browserCheck-bb_bb60%2Fsamples%2Fsample.xlsx&usg=AOvVaw3kB6F7vtKidr35vh64gzJb")
//                .ignoreContentType(true)
//                .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
//                .execute();
//
//		// output here
//		FileOutputStream out = (new FileOutputStream(new java.io.File("" + "oof.xls")));
//		out.write(resultImageResponse.bodyAsBytes());           
//		out.close();
//		System.out.println("done");
		

		//String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.122 Safari/534.30";
		//Jsoup.connect("http://example.com").userAgent(ua).get().html();
		
		/*
		//System.setProperty("http.agent", "");
		System.setProperty("http.agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		//URL url = new URL("https://www.sample-videos.com/xls/Sample-Spreadsheet-10-rows.xls");
		URL url = new URL("http://ggm.gtb.gov.tr/data/5a4e13beddee7d1fa8b6bfab/01 fasıl 2018.xls");
		InputStream in = url.openStream();
		OutputStream out = new BufferedOutputStream(new FileOutputStream("ouchie.xls"));
		final int BUFFER_SIZE = 1024 * 4;
		byte[] buffer = new byte[BUFFER_SIZE];
		BufferedInputStream bis = new BufferedInputStream(in);
		int length;
		while ( (length = bis.read(buffer)) > 0 ) {
		    out.write(buffer, 0, length);
		} 
		out.close();
		in.close();
		
	    System.out.println("done");
	    */
	    
/*

PointerInfo a = MouseInfo.getPointerInfo();
Point b = a.getLocation();
int x = (int) b.getX();
int y = (int) b.getY();
System.out.print(y + "jjjjjjjjj");
System.out.print(x);
Robot r = new Robot();
r.mouseMove(x, y - 50);
r.keyPress(KeyEvent.VK_TAB);						

*/
	    
	}
	
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

	
	
}
