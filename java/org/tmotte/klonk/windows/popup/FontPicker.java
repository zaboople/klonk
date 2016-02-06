package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.MinimumFont;
import org.tmotte.common.swang.Radios;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.Positioner;

public class FontPicker {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////


  // DI:
  private PopupInfo pInfo;
  private Setter<String> alerter;
  private FontOptions fontOptions;

  // State:
  private boolean ok=false;
  private Map<String,Font> goodFonts, badFonts;
  private Color selectedForeground, selectedBackground, selectedCaret;
  private boolean initialized=false;
  private DefaultListModel<String> fontNameData;
  private DefaultListModel<Integer> fontSizeData;
  private JList<String> jlFonts;
  private JList<Integer> jlFontSize;

  // Controls:
  private JDialog win;
  private JScrollPane  jspFonts, jspFontSize;
  private JButton btnOK, btnCancel;
  private JColorChooser colorChooser;
  private MyTextArea mta;
  private JScrollPane jspMTA;
  private JRadioButton jrbForeground, jrbBackground, jrbCaret;
  private JCheckBox jcbBold, jcbItalic;
  private JSpinner jspControlSize;
  private JLabel jlCFO, jlEFO;


  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public FontPicker(PopupInfo pInfo, Setter<String> alerter) {
    this.pInfo=pInfo;
    this.alerter=alerter;
  }
  public boolean show(FontOptions fontOptions) {
    init();
    prepare(fontOptions);
    while (true){
      ok=false;
      win.setVisible(true);
      win.toFront();
      if (!ok)
        return false;
      if (jlFonts.isSelectionEmpty())
        alerter.set("No font selected");
      else
      if (jlFontSize.isSelectionEmpty())
        alerter.set("No font size selected");
      else {
        fontOptions.setFontName(jlFonts.getSelectedValue());
        fontOptions.setFontSize(jlFontSize.getSelectedValue());
        fontOptions.setFontStyle(getSelectedStyle());
        fontOptions.setControlsFont(Integer.parseInt(jspControlSize.getValue().toString()));
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

    //General fonts:
    fontOptions.getControlsFont().set(win);
    jspControlSize.setValue(fontOptions.getControlsFont().getSize());

    //Set font name, size:
    {
      final int i=fontNameData.indexOf(fontOptions.getFont().getFamily());
      if (i!=-1) {
        jlFonts.setSelectedIndex(i);
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            jlFonts.ensureIndexIsVisible(i);
          }
        });
      }
    }
    {
      final int i=fontSizeData.indexOf(fontOptions.getFontSize());
      if (i!=-1) {
        jlFontSize.setSelectedIndex(i);
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            jlFontSize.ensureIndexIsVisible(i);
          }
        });
      }
    }


    //Set up color:
    selectedForeground=fontOptions.getColor();
    selectedBackground=fontOptions.getBackgroundColor();
    selectedCaret     =fontOptions.getCaretColor();
    setColorChooserColor();

    //Set up style:
    jcbBold.setSelected((fontOptions.getFontStyle() & Font.BOLD) > 0);
    jcbItalic.setSelected((fontOptions.getFontStyle() & Font.ITALIC) > 0);

    //Set up text area:
    mta.setFont(fontOptions.getFont());
    mta.setRows(4);
    mta.setForeground(fontOptions.getColor());
    mta.setBackground(fontOptions.getBackgroundColor());
    mta.setCaretColor(fontOptions.getCaretColor());

    Positioner.set(pInfo.parentFrame, win, false);
  }
  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    ok=action;
    win.setVisible(false);
  }

  private void changeFont() {
    int size=Integer.parseInt(jspControlSize.getValue().toString());
    new MinimumFont(size).set(win);
    new MinimumFont(size + 3).set(jlCFO, jlEFO);

    if (mta==null || jlFonts==null || jlFontSize==null ||
        jlFonts.isSelectionEmpty() || jlFontSize.isSelectionEmpty())
      return;
    mta.setFont(
      new Font(
        jlFonts.getSelectedValue(),
        getSelectedStyle(),
        jlFontSize.getSelectedValue())
      );
  }

  /////////////
  // CREATE: //
  /////////////

  private void init() {
    if (!initialized) {
      create();
      layout();
      listen();
      initialized=true;
    }
  }

  private void create(){
    win=new JDialog(pInfo.parentFrame, true);
    win.setTitle("Font Options");

    jlCFO=new JLabel("General Font Options:");
    jlCFO.setFont(jlCFO.getFont().deriveFont(Font.BOLD));
    jspControlSize=new JSpinner(new SpinnerNumberModel(1,1,99,1));

    jlEFO=new JLabel("Editor Font Options:");
    jlEFO.setFont(jlEFO.getFont().deriveFont(Font.BOLD));

    goodFonts=new HashMap<>();
    badFonts =new HashMap<>();
    fontNameData=new DefaultListModel<>();
    fontSizeData=new DefaultListModel<>();

    jlFonts=new JList<>(fontNameData);
    jlFonts.setVisibleRowCount(-1);
    jlFonts.setCellRenderer(new MyFontRenderer(jlFonts.getFont()));
    jspFonts = new JScrollPane(jlFonts);
    for (Font f: GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()){
      String name=f.getFamily();
      if (goodFonts.get(name)==null && f.canDisplayUpTo("abcdefgh")==-1){
        fontNameData.addElement(name);
        f=f.deriveFont(0, 14);
        goodFonts.put(name, f);
      }
    }

    jlFontSize=new JList<>(fontSizeData);
    jlFontSize.setVisibleRowCount(6);
    jspFontSize = new JScrollPane(jlFontSize);
    for (int i=5; i<40; i++)
      fontSizeData.addElement(i);

    jcbBold=new JCheckBox("Bold");
    jcbBold.setFont(jcbBold.getFont().deriveFont(Font.BOLD));
    jcbBold.setMnemonic(KeyEvent.VK_B);
    jcbItalic=new JCheckBox("Italic");
    jcbItalic.setFont(jcbItalic.getFont().deriveFont(Font.ITALIC));
    jcbItalic.setMnemonic(KeyEvent.VK_I);

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

    mta=new MyTextArea(pInfo.currentOS);
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
      this.setFont(font);
      setText(value);
      return this;
    }
  }

  /////////////
  // LAYOUT: //
  /////////////

  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridXY(0);
    gb.weightXY(0);
    gb.setInsets(10, 5, 0, 5);
    gb.fill=gb.BOTH;
    gb.anchor=gb.NORTHWEST;

    gb.addY(layoutTop());

    makeSeparator(gb);

    gb.fill=gb.BOTH;
    gb.weightXY(1);
    gb.addY(layoutBottom());
    win.pack();
  }

  private void makeSeparator(GridBug gb) {
    int il=gb.insets.left, ir=gb.insets.right;
    gb.insets.left=5; gb.insets.right=5;
    JSeparator j=new JSeparator(JSeparator.HORIZONTAL);
    gb.addY(j);
    gb.insets.left=il; gb.insets.right=ir;
  }

  private JPanel layoutTop() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.gridXY(0);
    gb.weightXY(0);
    gb.fill=gb.BOTH;
    gb.anchor=gb.NORTHWEST;
    gb.setInsets(5);

    // Title:
    gb.gridwidth=2;
    gb.setX(0);
    gb.addX(jlCFO);

    gb.gridwidth=1;

    // Spinner:
    gb.insets.left=10;
    gb.setX(0).setY(1);
    gb.gridXY(0, 2);
    gb.weightXY(0, 1);
    gb.fill=gb.NONE;
    gb.add(jspControlSize);

    // Spinner label;
    gb.insets.left=4;
    gb.fill=gb.VERTICAL;
    gb.weightXY(1, 0);
    gb.addX(new JLabel("Minimum font size for various controls"));
    return jp;
  }

  private JPanel layoutBottom() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.gridXY(0);
    gb.weightXY(0);
    gb.anchor=gb.NORTHWEST;

    // Label:
    gb.insets.left=5;
    gb.addY(jlEFO);

    // Font name, size< style:
    gb.fill=gb.BOTH;
    gb.weightXY(1);
    gb.addY(getEditorFontSelectionPanel());

    // Colors:
    gb.insets.top=5;
    gb.weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(getColorPanel());

    // Preview:
    gb.insets.top=0;
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.addY(getPreviewPanel());

    // Buttons:
    gb.weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(getButtonPanel());

    return jp;
  }
  private JPanel getEditorFontSelectionPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0.6, 1);
    gb.fill=gb.BOTH;
    gb.add(getFontListPanel());

    gb.weightx=0.2;
    gb.addX(getFontSizePanel());

    gb.weightx=0;
    gb.fill=gb.VERTICAL;
    gb.addX(getFontStylePanel());
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
    label.setDisplayedMnemonic(KeyEvent.VK_Z);
    label.setLabelFor(jlFontSize);
    gb.insets.top=10;
    gb.add(label);

    gb.weightXY(1);
    gb.insets.top=0;
    gb.fill=gb.BOTH;
    gb.addY(jspFontSize);

    return panel;
  }
  private JPanel getFontStylePanel(){
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.NORTHWEST;
    gb.insets.left=5;
    gb.insets.right=5;

    gb.weightXY(1,0);
    JLabel label=new JLabel("Font Style:");
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    gb.insets.top=10;
    gb.add(label);

    gb.insets.top=0;
    gb.fill=gb.NONE;
    gb.addY(jcbBold);
    gb.weightXY(1,1);
    gb.addY(jcbItalic);

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

  /////////////
  // LISTEN: //
  /////////////

  private void listen() {

    //Controls font size change:
    {
      ChangeListener styleListen=new ChangeListener(){
        public void stateChanged(ChangeEvent ce) {
          new MinimumFont(Integer.parseInt(jspControlSize.getValue().toString()))
            ;
          changeFont();
        }
      };
      jspControlSize.addChangeListener(styleListen);
    }

    //Font name & size change:
    {
      ListSelectionListener lsl=new ListSelectionListener(){
        public void valueChanged(ListSelectionEvent lse) {
          changeFont();
        }
      };
      jlFonts.addListSelectionListener(lsl);
      jlFontSize.addListSelectionListener(lsl);
    }

    //Font style change:
    {
      ChangeListener styleListen=new ChangeListener(){
        public void stateChanged(ChangeEvent ce) {
          changeFont();
        }
      };
      jcbBold.addChangeListener(styleListen);
      jcbItalic.addChangeListener(styleListen);
    }

    //Color chooser radio button change:
    {
      ActionListener clisten=new ActionListener(){
        public void actionPerformed(ActionEvent ce) {
          setColorChooserColor();
        }
      };
      jrbForeground.addActionListener(clisten);
      jrbBackground.addActionListener(clisten);
      jrbCaret.addActionListener(clisten);
    }

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

    // This means clicking enter anywhere activates ok
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));

    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.easyCancel(btnCancel, cancelAction);
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
  private int getSelectedStyle() {
    return Font.PLAIN | (jcbBold.isSelected() ?Font.BOLD :0) | (jcbItalic.isSelected() ?Font.ITALIC :0);
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
      PopupTestContext ptc=new PopupTestContext();
      public void run() {
        try {
          FontOptions fo=new FontOptions();
          System.out.println("Before: "+fo);
          new FontPicker(
            ptc.getPopupInfo(),
            new Setter<String>(){
              public void set(String s) {
                System.out.println("!!!!\n"+s+"\n!!!!");
              }
            }
          ).show(fo);
          System.out.println("After:  "+fo);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

}