package gtip;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import javax.swing.JProgressBar;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JCheckBox;
import java.awt.event.MouseMotionAdapter;

public class Form1 {

	private JFrame frame;
	private JTextField tfXLSBar;
	private JTextField tfXMLBar;
	private ArrayList<Hextuple> xlsData;
	private ArrayList<Hextuple> xmlData;
	private List<String> xlsFiles;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Form1 window = new Form1();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Form1() {
		initialize();
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 450, 229);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		
		JLabel lbtolerance = new JLabel("0.85");
		lbtolerance.setBounds(331, 113, 83, 26);
		frame.getContentPane().add(lbtolerance);
		
		JSlider toleranceSlider = new JSlider();
		toleranceSlider.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent arg0) {
				lbtolerance.setText((((double)toleranceSlider.getValue())/100)+"");
			}
		});
		toleranceSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				lbtolerance.setText((((double)toleranceSlider.getValue())/100)+"");
			}
		});
		toleranceSlider.setMinimum(1);
		toleranceSlider.setValue(85);
		toleranceSlider.setBounds(10, 113, 154, 26);
		frame.getContentPane().add(toleranceSlider);
		
		JLabel lblNewLabel = new JLabel("String Similarity Tolerance");
		lblNewLabel.setBounds(171, 113, 150, 26);
		frame.getContentPane().add(lblNewLabel);
		
		JCheckBox chcbUseAdvancedComparer = new JCheckBox("Use Simple String comparer");
		chcbUseAdvancedComparer.setSelected(true);
		chcbUseAdvancedComparer.setBounds(10, 150, 208, 23);
		frame.getContentPane().add(chcbUseAdvancedComparer);
		
		JCheckBox chckbForceDots = new JCheckBox("Force Dots on GTIP Files");
		chckbForceDots.setSelected(true);
		chckbForceDots.setBounds(220, 150, 218, 23);
		frame.getContentPane().add(chckbForceDots);
		
		
		JLabel lbInfo = new JLabel("");
		lbInfo.setBounds(10, 180, 404, 14);
		frame.getContentPane().add(lbInfo);
		
		JButton btnGenerateReport = new JButton("Generate Report");
		btnGenerateReport.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				lbInfo.setText("Working...");
				if(xlsData == null || xmlData == null) {lbInfo.setText("Scan for entries first!");}
				else 
				{
					
					ArrayList<MatchInfo> diffs = null;
					try {
						diffs = GTIPManager.findDifferences(xlsData, xmlData, ( (((double)toleranceSlider.getValue()))/100), chcbUseAdvancedComparer.isSelected());
					} catch (InterruptedException | ExecutionException e1) {
						e1.printStackTrace();
					}
					
					try {
						GTIPManager.generateReport(diffs);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					lbInfo.setText("Report generated, saved on root directory as 'report.xls'!");
				}
			}
		});
		btnGenerateReport.setBounds(10, 79, 215, 23);
		frame.getContentPane().add(btnGenerateReport);
		
		JButton btnCreateNewGtip = new JButton("Create New GTIP Files");
		btnCreateNewGtip.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				if(xlsFiles == null) {lbInfo.setText("Scan for XLS files first");}
				else
				{	
					final JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(Paths.get(tfXMLBar.getText()).toFile());
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					//fc.showOpenDialog(frmGtip);

					if (fc.showDialog(frame, "GTIP Save directory") == JFileChooser.APPROVE_OPTION) { 
						 
						String selection = fc.getSelectedFile()+"";
					
						GTIPBuilder builder = null;
						try {
							builder = new GTIPBuilder(chckbForceDots.isSelected());
						} catch (ParserConfigurationException e2) {
							e2.printStackTrace();
						}
		
						int indx = 0;
						for(String file : xlsFiles) 
						{
							int startRow = 5;
							if(file.contains("99")) {startRow = 9;}
							if(file.contains("29")) {startRow = 8;}
							
							ArrayList<Hextuple> entrylist = null;
							try {
								entrylist = GTIPManager.genHextuplesFromSingleXLS(file, startRow);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							builder.createEntriesFromList(entrylist);
							try {
								builder.saveXml(selection + "\\GTIP" + indx + ".xml");
							} catch (TransformerException e1) {
								e1.printStackTrace();
							}
							try {
								builder.wipeXml();
							} catch (ParserConfigurationException e1) {
								e1.printStackTrace();
							}
							indx++;
						}
						lbInfo.setText("Completed, created " + indx + " files under " + selection);
					}
				}
			}
		});
		btnCreateNewGtip.setBounds(232, 79, 192, 23);
		frame.getContentPane().add(btnCreateNewGtip);
		
		JButton btnFindXlsFiles = new JButton("Find XLS Files");
		btnFindXlsFiles.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(Paths.get(tfXLSBar.getText()).toFile());
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				//fc.showOpenDialog(frmGtip);

				if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) { 
					 
					String selection = fc.getSelectedFile()+"";
					tfXLSBar.setText(selection);
					 
					 try 
					 {
						 xlsData = GTIPManager.genHextuplesFromXLS(selection, "fasıl", ".xls");
						 xlsFiles = GTIPManager.findFiles(selection, "fasıl", ".xls");
					 } 
					 catch (IOException e) {
						lbInfo.setText("Invalid Directory or no xls files");
						e.printStackTrace();
					 }
					 
				 }
				 else {
				      //System.out.println("No Selection ");
				 }	
				
			}
		});
		btnFindXlsFiles.setBounds(10, 11, 142, 23);
		frame.getContentPane().add(btnFindXlsFiles);
		
		JButton btnFindXmlFiles = new JButton("Find XML Files");
		btnFindXmlFiles.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(Paths.get(tfXMLBar.getText()).toFile());
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				//fc.showOpenDialog(frmGtip);

				if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) { 
					 
					String selection = fc.getSelectedFile()+"";
					tfXMLBar.setText(selection);
					try {
						xmlData = GTIPManager.genHextuplesFromXML(selection ,true);
					} catch (IOException | ParserConfigurationException | SAXException e1) {
						lbInfo.setText("Invalid Directory or no xml files");
						e1.printStackTrace();
					}
				}
				else 
				{
					
				}
			}
		});
		btnFindXmlFiles.setBounds(10, 45, 142, 23);
		frame.getContentPane().add(btnFindXmlFiles);
		
		tfXLSBar = new JTextField();
		tfXLSBar.setBounds(162, 12, 262, 20);
		frame.getContentPane().add(tfXLSBar);
		tfXLSBar.setColumns(10);
		
		tfXMLBar = new JTextField();
		tfXMLBar.setText("D:\\Projects\\jguar_GIT_Set\\jprod\\UnityServer\\WebContent\\resources\\TRTR");
		tfXMLBar.setBounds(162, 46, 262, 20);
		frame.getContentPane().add(tfXMLBar);
		tfXMLBar.setColumns(10);
		
		
		
	}
}
