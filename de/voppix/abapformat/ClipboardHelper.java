package de.voppix.abapformat;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection; // for set
import java.awt.datatransfer.DataFlavor;      // for get

/** ClipboardHelper has two static methods for retrieving a string
 * from the system clipboard and for putting a string in the system
 * clipboard
 *
 * @Method get
 * @Method put
 * 
 * The problem in an X-Windows environment is, that the clipboard
 * contents is lost when the owner program exits (because the
 * clipboard itself just stores a pointer to a memory area of the
 * contents owning program), see
 * https://wiki.ubuntu.com/ClipboardPersistence
 *
 * This can be circumvented by 
 * 
 * A) leaving the program running (e.g.
 * `System.in.read();')
 * 
 * B) using a clipboard manager
 * Maybe the contents can also be retained by some clipboard manager
 * as mentioned on: http://en.wikipedia.org/wiki/Clipboard_manager
 *
 * (as of today (2013-11-27):
 * - parcellite does not work for me
 * - clipit (fork of parcellite) does not work for me
 * - glipper sees what we put in the clipboard if (and only if) we
 *   afterwards ask it for the new contents, if not -- it doesn't even
 *   show up in history. However, the contents is still cleared from
 *   the current clipboard as this program exits -- the contents can
 *   only be retrieved from the clipboard manager's history, quite
 *   strange. Not good.
 * )
 *
 * C) using a clipboard console tool instead (e.g. xclip)
 *
 */
public class ClipboardHelper //implements ClipboardOwner
{
  private static Toolkit toolkit     = Toolkit.getDefaultToolkit();
  private static Clipboard clipboard = toolkit.getSystemClipboard();

  public static void put(String theString) 
  {
    System.out.println("Must put to Clipboard: " + theString);
    StringSelection strSel = new StringSelection(theString);
    clipboard.setContents(strSel, null);
    // now, re-getting seems to help to make glipper aware of
    // the new clipboard contents (-- why?, how is this done
    // correctly?)
    try { String s = get(); } catch (Exception e) {/*ignore*/}
  }

  public static String get() throws Exception
  {
    String result = (String) clipboard.getData(DataFlavor.stringFlavor);
    System.out.println("Got String from Clipboard: " + result);
    return result;
  }
  // Well, that might be nice in the example, but why should we want
  // to do this?
  ///**
  // *    * Empty implementation of the ClipboardOwner interface.     
  // */
  //// @Override -- does not apply here!
  //public void lostOwnership(Clipboard aClipboard, Transferable aContents)
  //{
  //          //do nothing
  //}
  //the test
  public static void main(String[] args)
  {
    try
    {
      String s = get();
      put("I just got " + s + " from the clipboard");
      //System.in.read();
      //s = "";
      //s = get();
      //System.out.println("After putting, now getting back: " + s);
    }
    catch (Exception e)
    {
      //ignore?
      // e.printStackTrace();
      System.out.println("\t" + e);
    }
  }
}
