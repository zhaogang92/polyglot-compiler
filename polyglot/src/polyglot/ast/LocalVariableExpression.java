package jltools.ast;

import jltools.types.*;
import jltools.util.*;


/** 
 * A <code>LocalVariableExpression</code> corresponds to an immutable reference
 * to a local variable (not a field of a class) in an expression.
 */
public class LocalVariableExpression extends Expression 
{
  protected final String name;
  private LocalInstance li;
    
  /** 
   * Creates a new local variable reference.
   */
  public LocalVariableExpression( Node ext, String name) 
  {
    this.ext = ext;
    this.name = name;
  }

    public LocalVariableExpression( String name) {
	this(null, name);
    }

  public LocalVariableExpression reconstruct( Node ext, String name)
  {
    if( this.ext == ext && this.name.equals( name)) {
      return this;
    }
    else {
      LocalVariableExpression n = new LocalVariableExpression( ext, name);
      n.copyAnnotationsFrom( this);
      n.li = li;
      return n;
    }
  }

  public LocalVariableExpression reconstruct( String name) {
      return reconstruct(this.ext, name);
  }

  /** 
   * Returns the name of the variable referenced by this node.
   */
  public String getName() 
  {
    return name;
  }

  public LocalInstance getLocalInstance()
  {
    return li;
  }


  public Node visitChildren( NodeVisitor v) 
  {
      return reconstruct(Node.condVisit(ext, v), name);
  }

  public Node typeCheck( LocalContext c) throws SemanticException
  {
    /**
     * FIXME:
     * This was previously not here (ala spoons) since we weren't sure whether it
     * was necessary.  The switchStatement uses it to determine the labels
     * so that it can resolve final names to case lables. However, this brings up
     * the ugly point of whether we should reconstruct this or not. I am not 
     * reconstructing here, but perhaps we should?
     */
    li = c.getLocal( name);
    setCheckedType( li.getType());

    return this;
  }

  public void translate_no_override( LocalContext c, CodeWriter w)
  {
    w.write( name);
  }

  public void dump( CodeWriter w)
  {
    w.write( "LOCAL");
    w.write( " < " + name + " > ");
    dumpNodeInfo( w);
  }

  public int getPrecedence()
  {
    return PRECEDENCE_OTHER;
  }
  
  public String toString() {
	  return name;
  }
  
  public boolean equals(Object obj) {
	  if (!(obj instanceof LocalVariableExpression)) return false;
	  return name.equals(((LocalVariableExpression)obj).name);
  }
  
  public int hashCode() {
	  return name.hashCode();
  }
}
