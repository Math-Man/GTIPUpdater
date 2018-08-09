package gtip;

import org.w3c.dom.Element;

import java.io.File;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class GTIPBuilder {

	
	public boolean forceIndentation = true;
	
	private DocumentBuilderFactory docFac;
	private DocumentBuilder docBuilder;
	private Document document;
	private Element root;
	
	public GTIPBuilder(boolean forceIndentation) throws ParserConfigurationException 
	{
		docFac = DocumentBuilderFactory.newInstance();
		docBuilder = docFac.newDocumentBuilder();
		document = docBuilder.newDocument();
		
		//Create root
		
		root = document.createElement("GTIPCODES");
		document.appendChild(root);
		
		this.forceIndentation = forceIndentation;
		
	}
	
	/**
	 * Creats a gtip entry
	 * @param entry
	 */
	public void createEntry(Hextuple entry) 
	{
		
		int isCodeEmpty = 0;
		if( entry.getFirst() == null || ((String)entry.getFirst()).equals("")) {isCodeEmpty = 1;}
		
		Element newGTIP = document.createElement("GTIP");
		root.appendChild(newGTIP);
		
		if(entry.getSixth() != null)
		{
			String parentCode = ((String[])entry.getSixth())[0];
			if(!forceIndentation) 
			{
				parentCode = parentCode.replace(".", "");
			}
			Element ParentCode = document.createElement("PARENTCODE");
			ParentCode.appendChild(document.createTextNode( parentCode ));
			newGTIP.appendChild(ParentCode);
		}
		else 
		{
			Element ParentCode = document.createElement("PARENTCODE");
			ParentCode.appendChild(document.createTextNode( "" ));
			newGTIP.appendChild(ParentCode);
		}

		if(entry.getFirst() != null)
		{
			String code = (String)entry.getFirst();
			if(forceIndentation) 
			{
				//StringBuilder str = new StringBuilder(code);
				/*for(int i = 2; i < code.length(); i = i+3) 
				{
					str.insert(i, ".");
				}*/
				//code = str.toString();
				code = code.replaceAll("(.{2})", "$1.").replaceFirst(".$", "").replaceFirst("[.]", "");
			}
			Element Code = document.createElement("CODE");
			Code.appendChild(document.createTextNode( code ));
			newGTIP.appendChild(Code);
		}
		else 
		{
			Element Code = document.createElement("CODE");
			Code.appendChild(document.createTextNode( "" ));
			newGTIP.appendChild(Code);
		}
		Element Level = document.createElement("LEVEL_");
		Level.appendChild(document.createTextNode( (String)entry.getFourth() ));
		newGTIP.appendChild(Level);
		
		Element CodeEmpty = document.createElement("CODEEMPTY");
		CodeEmpty.appendChild(document.createTextNode( isCodeEmpty+"" ));
		newGTIP.appendChild(CodeEmpty);
		
		Element Selectable = document.createElement("SELECTABLE");
		Selectable.appendChild(document.createTextNode( (String)entry.getFifth() ));
		newGTIP.appendChild(Selectable);
		
		Element Description = document.createElement("DESCRIPTION");
		newGTIP.appendChild(Description);
		
		Element Index = document.createElement("INDEX_");
		Index.appendChild(document.createTextNode( "1" ));
		Description.appendChild(Index);
		
		Element Descr = document.createElement("DESCR");
		Descr.appendChild(document.createTextNode( ((String)entry.getSecond()).trim() ));
		Description.appendChild(Descr);
		
	}
	
	/**
	 * Creates entries from the given hextuple list
	 * @param c
	 */
	public void createEntriesFromList(Collection<Hextuple> c) 
	{
		for(Hextuple h : c) 
		{
			createEntry(h);
		}
	}
	
	/**
	 * Wipes the memory of the builder object
	 * @throws ParserConfigurationException
	 */
	public void wipeXml() throws ParserConfigurationException 
	{
		docFac = DocumentBuilderFactory.newInstance();
		docBuilder = docFac.newDocumentBuilder();
		document = docBuilder.newDocument();

		root = document.createElement("GTIPCODES");
		document.appendChild(root);
	}
	
	/**
	 * Saves the entries in the current memory to a file
	 * @param dir
	 * @throws TransformerException
	 */
	public void saveXml(String dir) throws TransformerException 
	{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(new File(dir));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);

		transformer.transform(source, result);

		System.out.println("File saved!");
	}
	
	
	
	
}
