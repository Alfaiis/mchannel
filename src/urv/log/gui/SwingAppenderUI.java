package urv.log.gui;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;

import urv.log.TextPaneAppender;



/** Creates a UI to display log messages from a SwingAppender
 * @author pshah
 *
 */
public class SwingAppenderUI extends javax.swing.JPanel {

	private static final long serialVersionUID = 4632204272328194437L;
	
	// Variables declaration - do not modify                     
    private javax.swing.JTabbedPane LogsTabs;
    // End of variables declaration                   

    /** Creates new form SwingAppenderUI */
    public SwingAppenderUI() {
        initComponents();
    }
    
    public void addTab(Level level){
    	this.addTab(level.toString(), level);
    }
    
    public void addTab(String title, Level level){
    	
    	Logger log = Logger.getRootLogger();
    
    	JPanel PanelLog = new javax.swing.JPanel();
    	JScrollPane ScrollLog = new javax.swing.JScrollPane();
        JTextPane TextLog = new javax.swing.JTextPane();
        
        JPanel PanelControl = new JPanel();
        JToggleButton FreezeButton = new JToggleButton("Freeze");
        FreezeButton.setName(Integer.toString(level.toInt(),10));
        JButton CleanButton = new JButton("Clean");
        CleanButton.setName(Integer.toString(level.toInt(),10));
        FreezeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Logger log = Logger.getRootLogger();
				TextPaneAppender tpa = ((TextPaneAppender) log.getAppender("textPaneAppender"));
				StringBuffer sb = tpa.getTextPaneOutStream().getTextBuffer(Integer.parseInt(((JToggleButton)e.getSource()).getName()));
				synchronized(sb){
					tpa.getTextPaneOutStream().setBufferFlushable(sb,(tpa.getTextPaneOutStream().isBufferFlushable(sb))?false:true);
				}
	        }		        	
		});
        CleanButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Logger log = Logger.getRootLogger();
				TextPaneAppender tpa = ((TextPaneAppender) log.getAppender("textPaneAppender"));
				JTextPane jtp = tpa.getTextPaneOutStream().getTextPane(Integer.parseInt(((JButton)e.getSource()).getName()));
				Document doc = jtp.getDocument();
				synchronized(doc){
					try {
						doc.remove(0, doc.getLength());
					} catch (BadLocationException e1) {
						LogLog.warn(e1.getMessage());
					}
				}
			}		        	
		});
        
        PanelControl.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        PanelControl.add(FreezeButton);
        PanelControl.add(CleanButton);
        
        PanelLog.setLayout(new java.awt.BorderLayout());
        ScrollLog.setViewportView(TextLog);

        PanelLog.add(ScrollLog, java.awt.BorderLayout.CENTER);
        PanelLog.add(PanelControl, java.awt.BorderLayout.SOUTH);
    	
    	TextPaneAppender tpa = ((TextPaneAppender) log.getAppender("textPaneAppender"));
    	
    	tpa.getTextPaneOutStream().addTextPane(TextLog,level);
    	LogsTabs.addTab("  " +title + "  ", PanelLog);
    	log.log(level, "testing " + level.toString() + " severity messages loging");
    }
    
    public void createTabsPerLevel(){
    	Level[] levels = new Level[]{Level.ALL,
    								 Level.FATAL,
    								 Level.ERROR,
    								 Level.WARN,
    								 Level.INFO,
    								 Level.DEBUG,
    								 Level.TRACE};
    	for (Level l : levels){
    		this.addTab(l);
    	}
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        
        
        LogsTabs = new javax.swing.JTabbedPane();

        

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        LogsTabs.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        LogsTabs.setName("LogsTabs"); // NOI18N
        
        add(LogsTabs, java.awt.BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(100,100));
    }// </editor-fold>

}