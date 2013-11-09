package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.Radios;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.PopupTestContext;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.Positioner;

class FontPicker {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  private JFrame parentFrame;
  private Fail fail;
  private JDialog win;
  private Popups popupMgr;
  private FontOptions fontOptions;
  private boolean ok=false;
  private Map<String,Font> goodFonts=new HashMap<>(),
                           badFonts =new HashMap<>();
  private Color selectedForeground, selectedBackground, selectedCaret;

  private DefaultListModel<String> fontNameData=new DefaultListModel<>();
  private DefaultListModel<Integer> fontSizeData=new DefaultListModel<>();
  private JList<String> jlFonts;
  private JList<Integer> jlFontSize;
  private JScrollPane  jspFonts, jspFontSize;
  private JButton btnOK, btnCancel;
  private JColorChooser colorChooser;
  private MyTextArea mta;
  private JScrollPane jspMTA;
  private JRadioButton jrbForeground, jrbBackground, jrbCaret;
  
  
  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public FontPicker(JFrame parentFrame, Fail fail, Popups popupMgr) {
    this.parentFrame=parentFrame;
    this.fail=fail;
    this.popupMgr=popupMgr;
    create();
    layout(); 
    listen();
  }
  public boolean show(FontOptions fontOptions) {
    prepare(fontOptions);
    while (true){
      ok=false;
      win.setVisible(true);
      win.toFront();
      if (!ok)
        return false;
      if (jlFonts.isSelectionEmpty())
        popupMgr.alert("No font selected");
      else
      if (jlFontSize.isSelectionEmpty()) 
        popupMgr.alert("No font size selected");
      else {
        fontOptions.setFontName(jlFonts.getSelectedValue());
        fontOptions.setFontSize(jlFontSize.getSelectedValue());
        fontOptions.setColor(selectedForeground);
        fontOptions.setBackgroundColor(selectedBackground);
        fontOptions.setCaretColor(selectedCaret);
        return true;
      }
    }
  }
  
  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////

  private void prepare(FontOptions fontOptions) {
  
    //Basic initialization:
    this.fontOptions=fontOptions;

    //Set font name, size:
    {
      int i=fontNameData.indexOf(fontOptions.getFontName());
      if (i!=-1) {
        jlFonts.setSelectedIndex(i);
        jlFonts.ensureIndexIsVisible(i);
      }
    }  
    {
      int i=fontSizeData.indexOf(fontOptions.getFontSize());
      if (i!=-1) {
        jlFontSize.setSelectedIndex(i);
        jlFontSize.ensureIndexIsVisible(i);
      }
    }  
    
    //Set up color:
    selectedForeground=fontOptions.getColor();
    selectedBackground=fontOptions.getBackgroundColor();
    selectedCaret     =fontOptions.getCaretColor();
    setColorChooserColor();
    
    //Set up text area:
    mta.setFont(fontOptions.getFont());
    mta.setRows(4);
    mta.setForeground(fontOptions.getColor());
    mta.setBackground(fontOptions.getBackgroundColor());
    mta.setCaretColor(fontOptions.getCaretColor());
  
    Positioner.set(parentFrame, win, false);
  }
  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    ok=action;
    win.setVisible(false);  
  }

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //  
  ///////////////////////////

  private void create(){
    win=new JDialog(parentFrame, true);
    win.setTitle("Font Options");
        
    jlFonts=new JList<>(fontNameData);
    jlFonts.setVisibleRowCount(-1);
    jlFonts.setCellRenderer(new MyFontRenderer(jlFonts.getFont())); 
    jspFonts = new JScrollPane(jlFonts);
    for (Font f: GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
      String name=f.getName();
      fontNameData.addElement(name);
      if (f.canDisplayUpTo("abcdefgh")!=-1)
        badFonts.put(name, f);
      else{
        f=f.deriveFont(0, 14);
        goodFonts.put(name, f);
      }
    }
    
    jlFontSize=new JList<>(fontSizeData);
    jlFontSize.setVisibleRowCount(6);
    jspFontSize = new JScrollPane(jlFontSize);
    for (int i=5; i<40; i++)
      fontSizeData.addElement(i);

    jrbForeground=new JRadioButton("Foreground");
    jrbForeground.setMnemonic(KeyEvent.VK_O);
    jrbForeground.setSelected(true);
    jrbBackground=new JRadioButton("Background");
    jrbBackground.setMnemonic(KeyEvent.VK_A);
    jrbCaret=new JRadioButton("Caret");
    jrbCaret.setMnemonic(KeyEvent.VK_T);
    Radios.doLeftRightArrows(Radios.create(jrbForeground, jrbBackground, jrbCaret));

    colorChooser=new JColorChooser();
    //This eliminates the preview so we can customize:
    colorChooser.setPreviewPanel(new JPanel());
    
    mta=new MyTextArea(); 
    mta.setRows(4);//This doesn't work right because we set the font different.
    mta.setLineWrap(false);
    mta.setWrapStyleWord(false);
    jspMTA=mta.makeVerticalScrollable();
    mta.setText("The quick brown fox jumped over the lazy dog.\n  Fourscore and seven years ago..."
      +"\n    One day Chicken Little was walking in the woods when -"
      +"\n1234567890 < > ? ! / \\ | , . : ; { } [ ] ( ) - + = @ # $ % ^ & * ~ `"    
    );
      
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);

  }
  private class MyFontRenderer extends JLabel implements ListCellRenderer<String> {
    private Font defaultFont;
    Color badColor=new Color(230, 100, 100);
    public MyFontRenderer(Font defaultFont) {
      this.defaultFont=defaultFont;
      setOpaque(true);
    }
    public Component getListCellRendererComponent(
        JList<? extends String> list,
        String value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
      )  {
      Font font=badFonts.get(value);
      boolean isBad=font!=null;
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(isBad ?badColor :list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(isBad ?badColor :list.getForeground());
      }      

      font=null;
      if (!isBad)
        font=goodFonts.get(value);
      if (font==null)
        font=defaultFont;
      setFont(font);
      setText(value);
      return this;
    }
  }


  
  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridXY(0);
    
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.anchor=gb.NORTHWEST;
    gb.add(getTopPanel());
    
    gb.insets.top=5;
    gb.weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(getColorPanel());

    gb.insets.top=0;
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.addY(getPreviewPanel());

    gb.weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(getButtonPanel());
    
    win.pack();
  }
  private JPanel getTopPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0.7, 1);
    gb.fill=gb.BOTH;
    gb.add(getFontListPanel());

    gb.weightx=0.3;
    gb.addX(getFontSizePanel());
    return jp;
  }
  private JPanel getFontListPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    JLabel label=new JLabel("Font:");
    label.setDisplayedMnemonic(KeyEvent.VK_F);
    label.setLabelFor(jlFonts);

    label.setFont(label.getFont().deriveFont(Font.BOLD));
    gb.insets.top=10;
    gb.insets.left=5;
    gb.insets.right=5;
    gb.add(label);
    
    gb.weightXY(1);
    gb.insets.top=0;
    gb.fill=gb.BOTH;
    gb.addY(jspFonts);

    return jp;
  }
  private JPanel getFontSizePanel(){
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.NORTHWEST;
    gb.insets.left=5;
    gb.insets.right=5;

    gb.weightXY(1,0);
    JLabel label=new JLabel("Font Size:");
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.setDisplayedMnemonic(KeyEvent.VK_O);
    label.setLabelFor(jlFontSize);
    gb.insets.top=10;
    gb.add(label);

    gb.weightXY(1);
    gb.insets.top=0;
    gb.fill=gb.BOTH;
    gb.addY(jspFontSize);
    
    return panel;
  }
  private JPanel getColorPanel() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.NORTHWEST;
    gb.insets.left=5;
    gb.insets.right=5;

    gb.weightXY(1, 0);
    gb.insets.top=5;
    gb.fill=gb.HORIZONTAL;
    gb.anchor=gb.WEST;
    gb.add(getColorTopPanel());

    gb.insets.top=0;
    gb.addY(colorChooser);
    return panel;
  }
  private JPanel getColorTopPanel() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    JLabel label=new JLabel("Color:");
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    gb.add(label);
    gb.insets.left=5;
    gb.addX(jrbForeground);
    gb.addX(jrbBackground);
    gb.weightx=1;
    gb.addX(jrbCaret);

    return panel;
  }
  private JPanel getPreviewPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    JLabel label=new JLabel("Preview:");
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.setDisplayedMnemonic(KeyEvent.VK_P);
    label.setLabelFor(jspMTA);
    gb.insets.top=10;
    gb.insets.left=5;
    gb.insets.right=5;
    gb.add(label);
    
    gb.weightXY(1);
    gb.insets.top=0;
    gb.fill=gb.BOTH;
    gb.addY(jspMTA);

    return jp;
  }
  private JPanel getButtonPanel() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    Insets insets=gb.insets;
    insets.top=5;
    insets.bottom=5;
    insets.left=5;
    insets.right=5;

    gb.gridx=0;
    gb.add(btnOK);
    gb.addX(btnCancel);
    return panel;
  }
  
  
  private void listen() {
  
    //Font name & size change:
    jlFonts.addListSelectionListener(new ListSelectionListener(){
      public void valueChanged(ListSelectionEvent lse) {
        changeFont();
      }
    });
    jlFontSize.addListSelectionListener(new ListSelectionListener(){
      public void valueChanged(ListSelectionEvent lse) {
        changeFont();
      }
    });

    //Radio button change:
    ActionListener clisten=new ActionListener(){
      public void actionPerformed(ActionEvent ce) {
        setColorChooserColor();
      }
    };
    jrbForeground.addActionListener(clisten);
    jrbBackground.addActionListener(clisten);
    jrbCaret.addActionListener(clisten);

    //Color change:
    colorChooser.getSelectionModel().addChangeListener(new ChangeListener(){
      public void stateChanged(ChangeEvent ce) {
        if (jrbForeground==null || jrbBackground==null || jrbCaret==null || mta==null)
          return;
        if (jrbForeground.isSelected()){
          selectedForeground=colorChooser.getColor();
          mta.setForeground(selectedForeground);
        }
        else 
        if (jrbBackground.isSelected()) {
          selectedBackground=colorChooser.getColor();
          mta.setBackground(selectedBackground);
        }
        else 
        if (jrbCaret.isSelected()) {
          selectedCaret=colorChooser.getColor();
          mta.setCaretColor(selectedCaret);
        }
      }
    });
    mta.addKeyListener(textAreaListener);

    //Button clicks:
    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(true);}
    };
    btnOK.addActionListener(okAction);
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));
    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.accel(btnCancel, cancelAction, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btnCancel, cancelAction, KeyMapper.key(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
  }
  
  private void changeFont() {
    if (mta==null || jlFonts==null || jlFontSize==null || 
        jlFonts.isSelectionEmpty() || jlFontSize.isSelectionEmpty())
      return;
    mta.setFont(new Font(jlFonts.getSelectedValue(), 0, jlFontSize.getSelectedValue()));
  }
  private void setColorChooserColor() {
    if (colorChooser==null || jrbForeground==null || jrbBackground==null || jrbCaret==null)
      return;
    if (jrbForeground.isSelected())
      colorChooser.setColor(selectedForeground);
    else
    if (jrbBackground.isSelected())
      colorChooser.setColor(selectedBackground);
    else
    if (jrbCaret.isSelected())
      colorChooser.setColor(selectedCaret);
  }
  
  /** Listening to the textareas for tab & enter keys: */
  private KeyAdapter textAreaListener=new KeyAdapter() {
    public void keyPressed(KeyEvent e){
      final int code=e.getKeyCode();
      if (code==e.VK_TAB) {
        int mods=e.getModifiersEx();
        if (KeyMapper.shiftPressed(mods))
          colorChooser.requestFocusInWindow();
        else
          btnOK.requestFocusInWindow();
        e.consume();
      }
    }
  };
  
  
  /////////////
  /// TEST: ///
  /////////////
  
  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          PopupTestContext ptc=new PopupTestContext(args);
          FontOptions fo=ptc.getPersist().getFontAndColors();
          ptc.getPopups().doFontAndColors(fo);
          ptc.getPersist().setFontAndColors(fo);
          ptc.getPersist().save();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }
  
}