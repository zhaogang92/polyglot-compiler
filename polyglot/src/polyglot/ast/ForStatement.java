package jltools.ast;

import jltools.types.*;
import jltools.util.*;

import java.util.*;


/**
 * An immutable representation of a Java language <code>for</code>
 * statement.  Contains a statement to be executed and an expression
 * to be tested indicating whether to reexecute the statement.
 */
public class ForStatement extends Statement 
{  
  /** 
   * A list of statements that will be executed once before the loop is
   * executed. 
   */
  private List inits;
  private Expression cond;
  /** 
   * A list of statements that will be executed at the end of each iteration. 
   */
  private List iters;
  private Statement body;

  /**
   * Create a new <code>ForStatement</code>.
   *
   * @pre Each element of <code>inits</code> must be a 
   *  <code>Statement</code>. Each element of <code>iters</code> is a
   *  <code>Statement</code>. 
   */
  public ForStatement( Node ext, List inits, Expression cond, List iters, 
                       Statement body) 
  {
    this.ext = ext;
    this.inits = TypedList.copyAndCheck( inits, Statement.class, true);
    this.cond = cond;
    this.iters = TypedList.copyAndCheck( iters, Statement.class, true);
    this.body = body;    
  }

  public ForStatement( List inits, Expression cond, List iters, 
                       Statement body) {
      this(null, inits, cond, iters, body);
  }


  public ForStatement reconstruct( Node ext, List inits, Expression cond, List iters, 
                                   Statement body)
  {
    if( this.inits.size() != inits.size() || this.ext != ext || this.cond != cond
        || this.iters.size() != iters.size() || this.body != body) {
      ForStatement n = new ForStatement( ext, inits, cond, iters, body);
      n.copyAnnotationsFrom( this);
      return n;
    }
    else {
      for( int i = 0; i < inits.size(); i++) {
        if( this.inits.get( i) != inits.get( i)) {
          ForStatement n = new ForStatement( ext, inits, cond, iters, body);
          n.copyAnnotationsFrom( this);
          return n;
        }
      }
      for( int i = 0; i < iters.size(); i++) {
        if( this.iters.get( i) != iters.get( i)) {
          ForStatement n = new ForStatement( ext, inits, cond, iters, body);
          n.copyAnnotationsFrom( this);
          return n;
        }
      }
      return this;
    }
  }

  public ForStatement reconstruct( List inits, Expression cond, List iters, 
                                   Statement body)
    {
	return reconstruct(this.ext, inits, cond, iters, body);
    }

  
  /**
   * Return an iterator of initializers for this statement.
   *
   * @post Each member of the list is of type <code>Statement</code>.
   */
  public Iterator initializers() 
  {
    return inits.iterator();
  }
 
  /**
   * Returns the condition of this for statement.
   */
  public Expression getCondition() 
  {
    return cond;
  }

  /**
   * Return the list of iterators for this statement.
   *
   * @post Each member of the list is of type <code>Statement</code>.
   */
  public Iterator iterators() 
  {
    return iters.iterator();
  }

  /**
   * Returns the body of this for statement.
   */
  public Statement getBody() 
  {
    return body;
  }

  public void enterScope( LocalContext c)
  {
    c.pushBlock();
  }

  public void leaveScope( LocalContext c)
  {
    c.popBlock();
  }

  /**
   * Visit the children of this node.
   *
   * @pre Requires that the <code>visit</code> method return an object of type
   *  <code>Statement</code> for all initializers and iterators. Also,
   *  <code>cond.visit</code> must return an <code>Expression</code> and
   *  <code>body.visit</code> must return a <code>Statement</code>
   */
  public Node visitChildren( NodeVisitor v) 
  {
    List newInits = new ArrayList( inits.size()),
      newIters = new ArrayList( iters.size());

    for( Iterator iter = initializers(); iter.hasNext(); ) {
      Statement stmt = (Statement)((Statement)iter.next()).visit( v);
      if( stmt != null) {
        newInits.add( stmt);
      }
    }

    Expression newCond = null;
    if( cond != null) {
      newCond = (Expression)cond.visit( v);
    }

    for( Iterator iter = iterators(); iter.hasNext(); ) {
      Statement stmt = (Statement)((Statement)iter.next()).visit( v);
      if( stmt != null) {
        newIters.add( stmt);
      }
    }

    Statement newBody = null;
    if( body != null) {
      newBody = (Statement)body.visit( v);
    }

    return reconstruct( Node.condVisit(this.ext, v),newInits, newCond, newIters, newBody);
  }

  public Node typeCheck( LocalContext c) throws SemanticException
  {
    
    if( cond != null && 
         !cond.getCheckedType().isImplicitCastValid( 
                      c.getTypeSystem().getBoolean())) {
      throw new SemanticException("The condition of a for statement must "
                                  + "evaluate to a boolean expression.",
				  Annotate.getLineNumber(cond));
    }

    /*
     * Type checking of the initializers and the iterators is handled by
     * the standard visitor model traversal.
     */

    return this;
  }

  public void translate_no_override(LocalContext c, CodeWriter w)
  {
    boolean writeSemicolon = true;
    
    w.write( "for (");
    w.begin(0);

    if( inits != null) {
      for( Iterator iter = inits.iterator(); iter.hasNext(); ) {
        Statement stmt = (Statement)iter.next();
	enterScope(c);
        if( stmt instanceof VariableDeclarationStatement) {
          stmt.translate_block (c, w);
          writeSemicolon = false;
	  w.allowBreak(0);
        } 
        else {
          ((ExpressionStatement)stmt).getExpression().translate_block(c, w);
        }

        if( iter.hasNext()) {
          w.write( ",");
	  w.allowBreak(2, " ");
        }
      }   
    }

    /*
     * If the initializer is a single variable declaration statement, then
     * we don't want to write out the semicolon, since the subnode has 
     * already done this.
     */
    if( writeSemicolon) {
      w.write(";"); 
      w.allowBreak(0);
    }
    
    if( cond != null) {
      cond.translate_block(c, w);
    }

    w.write (";"); /* cond is a expr, so write semicolon. */
    w.allowBreak(0);
    
    if( iters != null) {
      for( Iterator iter = iters.iterator(); iter.hasNext(); ) {
        Statement stmt = (Statement)iter.next();
        if( stmt instanceof ExpressionStatement) {
          ((ExpressionStatement)stmt).getExpression().translate_block(c, w);
        }
        else {
          stmt.translate_block(c, w);
        }
        
        if( iter.hasNext()) {
          w.write(",");
	  w.allowBreak(2, " ");
        }
      }
    }

    w.end();
    w.write( ")");
    
    if (body == null) {
      w.write(";");
    }

    enterScope(c);
    body.translate_substmt(c, w);
    leaveScope(c);

    if( inits != null) {
      for( Iterator iter = inits.iterator(); iter.hasNext(); ) {
        Statement stmt = (Statement)iter.next();
	leaveScope(c);
      }
    }
  }

  public String toString() {
    String s = "for (";

    if( inits != null) {
      for( Iterator iter = inits.iterator(); iter.hasNext(); ) {
	s += iter.next();

        if( iter.hasNext()) {
          s += " ";
        }
      }
    }

    s += cond + ";";

    if( iters != null) {
      for( Iterator iter = iters.iterator(); iter.hasNext(); ) {
	s += iter.next();

        if( iter.hasNext()) {
          s += " ";
        }
      }
    }

    s += ") " + body;

    return s;
  }

  public void dump( CodeWriter w)
  {
    w.write( "FOR ");
    dumpNodeInfo( w);
  }
}

