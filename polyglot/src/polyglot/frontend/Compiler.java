package jltools.frontend;

import jltools.ast.*;
import jltools.parse.*;
import jltools.lex.*;
import jltools.types.*;
import jltools.util.*;
import jltools.visit.*;

import java.io.*;
import java.util.*;

public class Compiler implements TargetTable, ClassCleaner
{
  /* Global constants. */
  public static String OPT_OUTPUT_WIDTH     = "Output Width (Integer)";
  public static String OPT_VERBOSE          = "Verbose (Boolean)";
  public static String OPT_FQCN             = "FQCN (Boolean)";
  public static String OPT_DUMP             = "Dump AST (Boolean)";
  public static String OPT_SCRAMBLE         = "Scramble AST (Boolean)";

  public static int VERSION_MAJOR           = 1;
  public static int VERSION_MINOR           = 0;
  public static int VERSION_PATCHLEVEL      = 0;

  /* Global options and state. */
  private static Map options;
  private static int outputWidth;
  private static boolean useFqcn;
  private static boolean dumpAst;
  private static boolean scrambleAst;
  private static boolean verbose;

  private static boolean initialized = false;

  /* Stage defining constants. */
  public static final int PARSED           = 0x01;
  public static final int READ             = 0x02;
  public static final int CLEANED          = 0x04;
  public static final int DISAMBIGUATED    = 0x08;
  public static final int CHECKED          = 0x10;
  public static final int TRANSLATED       = 0x20;

  /* Global factories. */
  protected static TargetFactory tf;
  protected static ErrorQueueFactory eqf;

  /* What's done and what needs work. */  
  protected static List workList;

  /* Static initializer method. */
  public static void initialize( Map options, TargetFactory tf, 
                                 ErrorQueueFactory eqf)
  {
    Compiler.options = options;
    Compiler.tf = tf;
    Compiler.eqf = eqf;
    Integer width;
    Boolean fqcn;
    Boolean dump;
    Boolean scramble;
    Boolean v;

    /* Read the options. */
    width = (Integer)options.get( OPT_OUTPUT_WIDTH);
    if( width == null) {
      width = new Integer( 72);
    }
    outputWidth = width.intValue();

    fqcn = (Boolean)options.get( OPT_FQCN);
    if( fqcn == null) {
      fqcn = new Boolean( false);
    }
    useFqcn = fqcn.booleanValue();

    dump = (Boolean)options.get( OPT_DUMP);
    if( dump == null) {
      dump = new Boolean( false);
    }
    dumpAst = dump.booleanValue();

    scramble = (Boolean)options.get( OPT_SCRAMBLE);
    if( scramble == null) {
      scramble = new Boolean( false);
    }
    scrambleAst = scramble.booleanValue();

    v = (Boolean)options.get( OPT_VERBOSE);
    if( v == null) {
      v = new Boolean( false);
    }
    verbose = v.booleanValue();
    
    /* Other setup. */
    workList = Collections.synchronizedList( new LinkedList());
    
    initialized = true;
  }

  public static boolean useFullyQualifiedNames()
  {
    return useFqcn;
  }

  public static void verbose( Object o, String s)
  {
    if( verbose) {
      if( o instanceof Class) {
        System.err.println( ((Class)o).getName() + ": " + s);
      }
      else {
        System.err.println( o.getClass().getName() + ": " + s);
      }
    }
  }
 
  /* Instance fields. */
  private TypeSystem ts;

  private CompoundClassResolver systemResolver;
  private CompoundClassResolver parsedResolver;
  private SourceFileClassResolver sourceResolver;
  private LoadedClassResolver loadedResolver;

  /* Public constructor. */
  public Compiler()
  {
    if( !initialized) {
      throw new InternalCompilerError( "Unable to construct compiler "
                       + "instance before static initialization.");
    }
    
    /* Set up the resolvers. */
    systemResolver = new CompoundClassResolver();
        
    parsedResolver = new CompoundClassResolver();
    systemResolver.addClassResolver( parsedResolver);
    
    sourceResolver = new SourceFileClassResolver( tf, this);
    systemResolver.addClassResolver( sourceResolver);
    
    loadedResolver = new LoadedClassResolver();
    systemResolver.addClassResolver( loadedResolver);
    
    ts = new ObjectPrimitiveTypeSystem( systemResolver);
    
    loadedResolver.setTypeSystem( ts);
    
    try
    {
      ts.initializeTypeSystem();
    }
    catch( TypeCheckException e)
    {
      throw new InternalCompilerError( "Unable to initialize compiler. " + 
                                       "Failed to initialize type system: " +
                                       e.getMessage());
    }
  }
 
 
 /* Public Methods. */
  public boolean compileFile( String filename) throws IOException 
  {
    return compile( tf.createFileTarget( filename)) ;
  }

  public boolean compileClass( String classname) throws IOException
  {
    return compile( tf.createClassTarget( classname));
  }

  public ClassResolver getResolver( Target t) throws IOException
  {
    Job job = lookupJob( t);
    boolean success = compile( job, READ);

    if( success) {
      return job.cr;
    }
    else {
      return null;
    }
  }

  public boolean cleanClass( ClassType clazz) throws IOException
  {
    Job job = lookupJob( clazz);

    return compile( job, CLEANED);
  } 

  public boolean compile( Target t) throws IOException
  {
    return compile( t, TRANSLATED);
  }
 
  public boolean cleanup( Collection completed) throws IOException
  {
    boolean okay = true, success;
    
    for( int i = 0; i < workList.size(); i++)
    {
      Job job = (Job)workList.get( i);
      success = compile( job, TRANSLATED);
      if( success) {
        completed.add( job.t);
      }
      else {
        okay = false;
      }
    }
    return okay;
  }


  /* Protected methods. */
  protected boolean compile( Target t, int goal) throws IOException
  {
    Job job = lookupJob( t);
    return compile( job, goal);
  }

  protected boolean compile( Job job, int goal) throws IOException
  {
    if( hasErrors( job)) {
      return false;
    }

    try
    {
      /* PARSE. */
      if( (job.status & PARSED) == 0) {

        verbose( this, "parsing " + job.t.getName() + "...");
        job.ast = parse( job.t, job.eq);

        if( hasErrors( job)) {
          return false;
        }

        job.status |= PARSED;
      }
      
      if( dumpAst) { dump( job.ast); }
      if( goal == PARSED) { return true; }

      /* READ. */
      if( (job.status & READ) == 0) {
        verbose( this, "reading " + job.t.getName() + "...");
        job.cr = new TableClassResolver( this);
        parsedResolver.addClassResolver( job.cr);
        job.it = readSymbols( job.ast, job.cr, job.eq);

        job.status |= READ;
      }

      if( goal == READ) { return true; }

      /* CLEAN. */
      if( (job.status & CLEANED) == 0) {
        verbose( this, "cleaning " + job.t.getName() + "...");
        job.cr.cleanupSignatures( ts, job.it, job.eq);
        if( hasErrors( job)) {
          return false;
        }

        job.status |= CLEANED;
      }

      if( goal == CLEANED) { return true; }

      /* DISAMBIGUATE. */
      if( (job.status & DISAMBIGUATED) == 0) {
        verbose( this, "disambiguating " + job.t.getName() + "...");
        job.ast = removeAmbiguities( job.ast, job.cr, job.it, job.eq);
        if( hasErrors( job)) {
          return false;
        }

        job.status |= DISAMBIGUATED;
      }

      if( dumpAst) { dump( job.ast); }
      if( goal == DISAMBIGUATED) { return true; }


      /* Okay. Before we can type check, we need to make sure that 
       * else in the worklist is at least CLEAN. */
      boolean okay = true; 
      for( int i = 0; i < workList.size(); i++) {
        okay &= compile( (Job)workList.get( i), CLEANED);
      }
      if( !okay) {
        job.eq.enqueue( ErrorInfo.SEMANTIC_ERROR, "Unable to continue " 
                    + "because of errors in dependencies.");
        return false;
      }
      
      if( (job.status & CHECKED) == 0 && scrambleAst) {
        dump( job.ast);

        NodeScrambler ns = new NodeScrambler();
        job.ast = job.ast.visit( ns.fp);
        job.ast = job.ast.visit( ns);
      
        dump( job.ast);
      }

      /* CHECK. */
      if( (job.status & CHECKED) == 0) {
        verbose( this, "type checking " + job.t.getName() + "...");
        typeCheck( job.ast, job.it, job.eq);    
        if( hasErrors( job)) {
          return false;
        }
        
        job.status |= CHECKED;
      }

      if( dumpAst) { dump( job.ast); }
      if( goal == CHECKED) { return true; }

      ObjectPrimitiveCastRewriter op = new ObjectPrimitiveCastRewriter( ts);
      job.ast = job.ast.visit( op);

      /* TRANSLATE. */
      if( (job.status & TRANSLATED) == 0) {
        verbose( this, "translating " + job.t.getName() + "...");
        translate( job.t, job.it, job.ast);

        job.status |= TRANSLATED;
      }
    }
    catch( IOException e)
    {
      job.eq.enqueue( ErrorInfo.IO_ERROR, 
                      "Encountered an I/O error while compiling.");
      job.eq.flush();
      throw e;
    }
    catch( RuntimeException rte)
    {
      CodeWriter cw = new CodeWriter( new UnicodeWriter( 
                                        new FileWriter( "ast.dump")), 
                                      outputWidth);
      DumpAst d = new DumpAst( cw);
      job.ast.visit( d);
      cw.flush();

      throw rte;
    }

    job.eq.flush();

    if( job.eq.hasErrors()) {
      return false;
    }
    else {
      return true;
    }
  }
 
  
  
  /* Protected Methods. */
  protected Job lookupJob( ClassType clazz) throws IOException
  {
    Target t = tf.createClassTarget( clazz.getFullName());

    return lookupJob( t);
  }

  protected Job lookupJob( Target t) throws IOException
  {
    Job job = new Job( t, eqf.createQueue( t.getName(), t.getSourceReader()));
    
    if( workList.contains( job)) {
      job = (Job)workList.get( workList.indexOf( job));
      if( job.eq.hasErrors()) {
        return null;
      }
    }
    else {
      workList.add( job);
    }

    return job;
  }

  protected boolean hasErrors( Job job)
  {
    if( job.eq.hasErrors()) {
      job.eq.flush();
      return true;
    }
    else {
      return false;
    }
  }

  protected Node parse( Target t, ErrorQueue eq) throws IOException
  {
    Lexer lexer;
    Grm grm;
    java_cup.runtime.Symbol sym = null;

    lexer = new Lexer( t.getSourceReader(), eq);
    grm = new Grm( lexer, ts, eq);
               
    try
    {
      sym = grm.parse();
    }
    catch( IOException e)
    {
      eq.enqueue( ErrorInfo.IO_ERROR, e.getMessage());
      throw e;
    }
    catch( Exception e)
    {
      e.printStackTrace();
      eq.enqueue( ErrorInfo.INTERNAL_ERROR, e.getMessage());
      return null;
    }

    /* Try and figure out whether or not the parser was successful. */
    if( sym == null) {
      eq.enqueue( ErrorInfo.SYNTAX_ERROR, "Unable to parse source file.");
      return null;
    }

    if( sym.value instanceof SourceFileNode) {
      ((SourceFileNode)sym.value).setFilename( t.getName());
    }

    if( !(sym.value instanceof Node)) {
      eq.enqueue( ErrorInfo.SYNTAX_ERROR, "Unable to parse source file.");
      return null;
    }
    else {
      return (Node)sym.value; 
    }
  }

  protected ImportTable readSymbols( Node ast, TableClassResolver cr,
                                     ErrorQueue eq)
  {
    SymbolReader sr = new SymbolReader( systemResolver, cr, ts, eq);
    ast.visit( sr);

    return sr.getImportTable();
  }

  protected Node removeAmbiguities( Node ast, TableClassResolver cr,
                                    ImportTable it, ErrorQueue eq)
  {
    AmbiguityRemover ar = new AmbiguityRemover( ts, it, eq);
    return ast.visit( ar);
  }

  protected Node typeCheck( Node ast, ImportTable it, ErrorQueue eq)
  {
    TypeChecker tc = new TypeChecker( ts, it, eq);
    return ast.visit( tc);
  }

  protected void translate( Target t, ImportTable it, Node ast) throws IOException
  {
    SourceFileNode sfn = (SourceFileNode)ast;
    CodeWriter cw = new CodeWriter( t.getOutputWriter( sfn.getPackageName()), 
                                    outputWidth);
    
    ast.translate( new LocalContext(it, ts),  cw);
    
    cw.flush();
    System.out.flush();
  }

  public void dump( Node ast) throws IOException
  {
    CodeWriter cw = new CodeWriter( new UnicodeWriter( 
                                       new PrintWriter( System.out)), 
                                    outputWidth);
    DumpAst d = new DumpAst( cw);
    
    ast.visit( d);
    
    cw.flush();
  }
  
  
  static class Job
  {
    Target t;
    ErrorQueue eq;
    Node ast;
    ImportTable it;
    TableClassResolver cr;

    int status;

    public Job( Target t, ErrorQueue eq) 
    {
      this.t = t;
      this.eq = eq;
      
      ast = null;
      it = null;
      cr = null;

      status = 0;
    }
    
    public boolean equals( Object o) {
      if( o instanceof Job) {
        return t.equals( ((Job)o).t);
      }
      else {
        return false;
      }
    }
  }
  
}
