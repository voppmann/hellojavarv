package de.voppix.abapformat;

import java.util.ArrayList;


/**
 * The DeclarationFormatter class provides some formatting for
 * abap source code.
 *
 * First thing to do: Reformat declaration blocks (the pretty 
 * printer of the abap work bench does not really pretty print 
 * them except for case conversion according to user preferences).
 * - Make all declarations start on the same column,
 * - Make all typification phrases start in the same column,
 * - Make all types referred to start in the same column.
 *
 * Terms used:
 * - DeclarationStatement
 * - Declaration
 * - DeclarationPart
 *
 * @author: Robert Voppmann
 * @version: 0.1
 *
 * TODO:
 * - handle more than one declaration statements
 * - handle comments
 * - command-line parameters for
 *   - reading input from STDIN
 *   - reading input from String argument
 *   - reading input from clipboard
 *   - reading input from primary selection
 *   - writing output to STDOUT
 *   - writing output to clipboard (does not work in X11)
 *   - writing output to primary selection (does not work in X11)
 * - debugging info should not be written to stdout
 *
 * Notes:
 * - A declaration statement starts with a keyword such as `DATA' or
 *   `FIELD-SYMBOLS:' and ends with a `.'
 * - Declarations within a declaration statement are separated by
 *   comma.
 * - Comments can be included in the declaration statement either as
 *   full-line comments (starting with `*' at the beginning of the 
 *   line and ending at EOL) or
 *   as line-end comments (starting with `"' and ending at EOL
 */
public class DeclarationFormatter{
  enum         declarationParts {VARIABLE, TYPIFICATION, TYPE};
  public static void main (String[] args)
  {
    //
    declarationParts declarationPart;
    final String sep                 = ",";
    Declaration   declaration        = null;
    Declaration[] declarations       = new Declaration[100];
    int           declarationCounter = 0;
    java.util.Scanner scanner;
    // check for input
    if (args.length == 0)
    {
      scanner = new java.util.Scanner(System.in);
      System.out.println("Using System.in for input");
    } else {
      scanner = new java.util.Scanner(args[0]);
      System.out.println("Using args for input: " + args[0]);
    }
    String token = "";
    // first remember the data, types, constants or field-symbols
    // opening token
    String opening = "";
    token = scanner.next();

    if (isDeclarationOpening(token))
    {
      opening = token;
      token   = scanner.next();
    } // is opening
    declarationPart = declarationParts.VARIABLE;
    while (token != null) // read tokens
    {
      //System.out.println(token);
      switch (declarationPart)
      {
        case VARIABLE:
        {
          System.out.println( token );
          declaration = new Declaration();
          declarations[++declarationCounter - 1] = declaration;
          declaration.setVariable(token);
          declarationPart = declarationParts.TYPIFICATION;
          break;
        }
        case TYPIFICATION:
        {
          assert declaration != null: "Typification before variable name?";
          declaration.setTypification(getTypification(scanner));
          System.out.println("declaration.Typificaiton = " + declaration.getTypification());
          declarationPart = declarationParts.TYPE;
          break;
        }
        case TYPE:
        {
          assert declaration != null: "Typification before variable name?";
          declaration.setType(getType(scanner)); // get's the whole rest of the declaration up to the next comma or period
          if (scanner.hasNext())
          {
            token = scanner.next();
            declarationPart = declarationParts.VARIABLE;
          }
          else
          {
            token = null;
          }
          break;
        }// case TYPE
      }// switch (declarationPart)
    }//read tokens 
    
    if (opening.equals("")){
      System.out.println("no opening given");
    }
    else {
      System.out.println("opening = " + opening);
    }
    // calculate the width of the variable column
    int varColWidth = 0;
    for (Declaration d : declarations){
      if (d == null){break;}
      if (d.getVariable().length() > varColWidth){
        varColWidth = d.getVariable().length();
      }
    }
    // Build the printf format string
    // like "%-30s %-15s %-30s\n", 
    String printfFormat = "%s %-" + varColWidth + "s %-13s %s";
    // print out declarations
    Boolean isFirst = true; // first declaration is not preceded with ","
    String prefix = ""; // "  , " or "    "
    java.util.ArrayList<String> lines = new ArrayList<>();
    for (Declaration d: declarations)
    {
      if (d == null) break;
      //how does this work? 
      if (isFirst){ prefix = "   "; isFirst = false; } else { prefix = "  ,";}
      String line = String.format(
          printfFormat, 
          prefix,
          d.getVariable(), 
          d.getTypification(), 
          d.getType()
          );
      lines.add(line);
    } // for each delaration
    if (lines.size() > 0)
    {
      if (opening != "")
      {
        lines.add(0, "  " + opening);     // insert the declaration opening
      }
      lines.add("  ."); // in order to terminate the declaration statement
    }
    for(String line: lines){
      System.out.println(line);
    }
  } // main

  /**
   * Returns true, if the token opens a declaration
   */
  private static boolean isDeclarationOpening(String aToken)
  {
    if ( aToken.equalsIgnoreCase("data") ||
         aToken.equalsIgnoreCase("data:") ||
         aToken.equalsIgnoreCase("type") ||
         aToken.equalsIgnoreCase("types:") ||
         aToken.equalsIgnoreCase("constant") ||
         aToken.equalsIgnoreCase("constants:") ||
         aToken.equalsIgnoreCase("field-symbol") ||
         aToken.equalsIgnoreCase("field-symbols:")
       )
    {return true;}
    else
    {return false;}
  } // isDeclarationOpening()

  /** getTypification returns a typification string from a scanner,
   * such as TYPE REF TO
   */
  private static String getTypification( java.util.Scanner scanner)
  {
    // patterns for analyzing the next token(s)
    java.util.regex.Pattern typificationLineTablePattern = 
      java.util.regex.Pattern.compile(
          "table|line",
          java.util.regex.Pattern.CASE_INSENSITIVE
          );
    java.util.regex.Pattern typificationLineTable2Pattern = 
      java.util.regex.Pattern.compile(
          "of",
          java.util.regex.Pattern.CASE_INSENSITIVE
          );
    java.util.regex.Pattern typificationRefPattern = 
      java.util.regex.Pattern.compile(
          "ref",
          java.util.regex.Pattern.CASE_INSENSITIVE
          );
    java.util.regex.Pattern typificationRef2Pattern = 
      java.util.regex.Pattern.compile(
          "to",
          java.util.regex.Pattern.CASE_INSENSITIVE
          );
    java.util.regex.Pattern simpleTypificationPattern = 
      java.util.regex.Pattern.compile(
          "type|like",
          java.util.regex.Pattern.CASE_INSENSITIVE
          );
    // analysis
    String token = "";
    if (scanner.hasNext(simpleTypificationPattern))
    { 
      token = scanner.next(simpleTypificationPattern);
      System.out.println("Found typification token: " + token);
      if (scanner.hasNext(typificationLineTablePattern))
      {
        token += " " + scanner.next(typificationLineTablePattern);
        if (scanner.hasNext(typificationLineTable2Pattern))
        {
          token += " " + scanner.next(typificationLineTable2Pattern);
        }
        else
        { // type table *of* missing!
          //todo:throw error;
          System.out.println(token + "'of' missing");
        }
      }
      else if (scanner.hasNext(typificationRefPattern))
      {
        token += " " + scanner.next(typificationRefPattern);
        if (scanner.hasNext(typificationRef2Pattern))
        {
          token+= " " + scanner.next(typificationRef2Pattern);
        }
        else
        { //type ref *to* missing!
          //todo: throw error;
          System.out.println(token + "'to' missing");
        }
      }//notificationRefPattern
    }// typificationPattern
    else
    {
      // todo: throw exception
      System.out.println("entered getTypification, but no type or like found!");
    }
    return token;
  } // private static getTypification

  /** Method getType gets everything of the declaration 
   * that comes after the typification
   *
   * i.e. [ls_bla] [like line of] [lt_bla] -- [lt_bla]
   */
  private static String getType(java.util.Scanner scanner)
  {
    boolean finished = false;
    String  result   = "";
    String  token    = "";

    System.out.println("entering getType");

    while (finished == false)
    {
      if (scanner.hasNext(".")||scanner.hasNext(","))
      {
        token = scanner.next(); //throw away
        System.out.println("throwing away " + token);
        finished = true;
      }
      else if (scanner.hasNext())
      {
        result += " " + scanner.next();
        if (result.endsWith(".")||result.endsWith(","))
        {
          result = result.substring(0,result.length() - 1);
          finished = true;
        }
      }
      else
      { 
        //todo throw exception
        System.out.println("Declaration without TYPE part");
        break;
      }
    }//while end of declaration not reached
    return result;
  }//private String getType(java.util.Scanner scanner)

} // class DeclarationFormatter

/////////////////////////////////////////////////////////////////////

/** A Declaration holds the parts of the declaration
 *
 * These are the parts
 * - variable 
 * - typification
 * - type
 * - optional: value_keyword + value
 *   (the value_keyword is always VALUE (case-insensitive),
 *   the value may consist of several tokens if surrounded by single
 *   quotes, or it is just one single token (reference to some
 *   constant).
 * As in: lt_itab TYPE TABLE OF ls_itab,
 * where lt_itab is the declared variable, TYPE TABLE OF is the
 * typification and ls_itab is the type.
 */
class Declaration{
  String variable     = "";
  String typification = "";
  String type         = "";

  Declaration()
  {
    //empty constructor
  }
  Declaration(String variable, String typification, String type)
  {
    this.setVariable(variable);
    this.setTypification(typification);
    this.setType(type);
  }
  void setVariable(String variable)
  {
    this.variable = variable;
  }
  void setTypification(String typification)
  {
    this.typification = typification;
  }
  void setType(String type)
  {
    this.type = type;
  }
  
  String getVariable()
  {
    return this.variable;
  }
  String getTypification()
  {
    return this.typification;
  }
  String getType()
  {
    return this.type;
  }
  public String toString(){
    return this.variable + this.typification + this.type;
  }


}// class Declaration
