package org.tmotte.common.text;
import java.util.List;
import java.util.LinkedList;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import java.io.IOException;
/**
 *  Simple class for building a String that is delimited by commas or brackets or whatever. Maintains an internal list
 *  to which objects are added, then concatenates everything in toString() and appendTo().
 *  Note that objects added to a DelimitedString are not toString()'d themselves until DelimitedString.toString() is called.
 */
public class DelimitedString implements Appender {

  private List<Object> listData=new LinkedList<>();
  private String initial,between,last, beforeEach, afterEach;

  /**
   * Same as new <code>DelimitedString("", between, "", "", "");</code>
   *  @param between Will go between items.
   */
  public DelimitedString(String between){
    this("",between,"");
  }

  /**
   * Same as new <code>DelimitedString(initial, between, "", "", "");</code>
   *  @param initial Will go before first item.
   *  @param between Will go after each additional item except last.
   */
  public DelimitedString(String initial, String between){
    this(initial,between,"");
  }
  /**
   * Same as new <code>DelimitedString(initial, between, last, "", "");</code>
   *  @param initial Will go before first item.
   *  @param between   Will go after each additional item except last.
   *  @param last   Will go after last item.
   */
  public DelimitedString(String initial, String between, String last){
    this(initial, between, last, "", "");
  }
  /**
   * Sets all the possible delimiters. When printed, the DelimitedString will
   * insert these delimiters around list members as they are concatenated together. Null parameters
   * for this constructor will be treated as blanks ("").
   * <br>
   * For example, the two-item list "a" "b" will be formatted as:<pre>

      [initial][beforeEach]a[afterEach][between][beforeEach]b[afterEach][last]
      </pre>
   * As another example, to generate a comma-separated-values (CSV) record, the constructor would be invoked as:<br>
   * <pre>

      new DelimitedString(null, ",", null, "\"", "\"");
      </pre>
   *  @param initial Goes before first item.
   *  @param between Is only used if there is more than one
   *                 item. Goes after each item except the last one.
   *  @param last    Goes after last item.
   *  @param beforeEach This goes immediately before each item; for the first item,
   *                    goes after <code>initial</code> but before the item.
   *  @param afterEach This goes immediately after each item; for the last item,
   *                    goes after the item, but before <code>last</code>.
   */
  public DelimitedString(String initial, String between, String last, String beforeEach, String afterEach){
    this.initial=initial==null ?"" :initial;
    this.between=between==null ?"" :between;
    this.last=last==null ?"" :last;
    this.beforeEach=beforeEach==null ?"" :beforeEach;
    this.afterEach=afterEach==null ?"" :afterEach;
  }

  /**
   * Obtains the size of the internal list.
   */
  public int size(){
    return listData.size();
  }
  /**
   * Clears the internal list of data. Does not affect any of the delimiters.
   */
  public void clear(){
    listData.clear();
  }


  //////////
  // ADD: //
  //////////

  /**
   * Adds <code>object</code> to the internal list.
   */
  public final DelimitedString add(Object object){
    if (object==null)
      throw new IllegalArgumentException("Null parameter");
    listData.add(object);
    return this;
  }
  /** Adds each member of <code>list</code> individually. */
  public DelimitedString addEach(Enumeration<?> list){
    while (list.hasMoreElements())
      add(list.nextElement());
    return this;
  }
  /** Adds each member of <code>list</code> individually. */
  public DelimitedString addEach(Iterator<?> list){
    while (list.hasNext())
      add(list.next());
    return this;
  }
  /** Adds each member of <code>list</code> individually. */
  public DelimitedString addEach(List<?> list){
    for (Object o: list)
      this.add(o);
    return this;
  }
  /** Adds each member of <code>list</code> individually. */
  public DelimitedString addEach(Object... list){
    for (Object o: list)
      add(o);
    return this;
  }

  /////////////////
  // Generation: //
  /////////////////

  private void doString(Appendable a, String before1, String before2, Object middle, String after) throws IOException {
    if (middle instanceof DelimitedString && ((DelimitedString)middle).size()==0)
      return;
    a.append(before1);
    a.append(before2);
    if (middle instanceof Appender)
      ((Appender)middle).appendTo(a);
    else
      a.append(middle.toString());
    a.append(after);
  }
  /**
   * Fulfills the Appender interface. Similar to toString(), but is more
   * efficient to use when you know what you want to write the DelimitedString
   * to, since it eliminates the need for an internal text buffer (e.g. StringBuilder/StringBuffer).
   */
  public void appendTo(Appendable a){
    if (listData.size()>0)
      try {
        doString(a, initial, beforeEach, listData.get(0), afterEach);
        for (int i=1; i<listData.size(); i++)
          doString(a, between, beforeEach, listData.get(i), afterEach);
        a.append(last);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
  }
  /**
   * Converts the internal list to a String by toString()'ing list members and concatenating them
   * together in order, delimited by the various delimiters passed to the constructor.
   * @return "" if nothing has been added.
   */
  public String toString() {
    StringBuilder result=new StringBuilder();
    appendTo(result);
    return result.toString();
  }

  ///////////
  // TEST: //
  ///////////

  /** Used for unit test. */
  public static void main(String[] args){
    DelimitedString string=new DelimitedString("select * from x where (", " and ", ")", "", "=?");
    System.out.println(string.add("fee").add("fo").add("fum"));
    DelimitedString ds=new DelimitedString(null, ",", null, null, null);
    List<String> c=new LinkedList<>();
    c.add("a");
    c.add("b");
    c.add("c");
    ds.add(c);
    System.out.println(ds);
  }

}

