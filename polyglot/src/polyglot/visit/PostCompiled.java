/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006-2008 IBM Corporation
 * 
 */

package polyglot.visit;

import java.io.InputStreamReader;
import java.util.Iterator;

import polyglot.frontend.*;
import polyglot.frontend.Compiler;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.util.*;

/** The post compiler pass runs after all jobs complete.  It invokes the post-compiler on the output files stored in compiler.outputFiles(). */
public class PostCompiled extends AllBarrierGoal
{
    ExtensionInfo ext;

    /**
     * Create a Translator.  The output of the visitor is a collection of files
     * whose names are added to the collection <code>outputFiles</code>.
     */
    public PostCompiled(ExtensionInfo ext) {
	super("PostCompile",ext.scheduler());
	this.ext = ext;
    }

    public Goal prereqForJob(Job job) {
	if (scheduler.shouldCompile(job)) {
	    return scheduler.End(job);
	}
	else {
	    return new SourceGoal_c("DummyEnd", job) {
		public boolean runTask() { return true; }
	    }.intern(scheduler);
	}
    }

    public final static String postcompile = "postcompile";

    public boolean runTask() {
	Compiler compiler = ext.compiler();

	if (Report.should_report(postcompile, 2))
	    Report.report(2, "Output files: " + compiler.outputFiles());

	long start_time = System.currentTimeMillis();

	try {
	    return invokePostCompiler(ext.getOptions(), compiler, compiler.errorQueue());
	}
	finally {
	    if (Report.should_report("time", 1))
		Report.report(1, "Finished compiling Java output files. time=" +
		              (System.currentTimeMillis() - start_time));
	}

    }

    protected boolean invokePostCompiler(Options options,
                                      Compiler compiler,
                                      ErrorQueue eq) {
        if (options.post_compiler != null && !options.output_stdout) {
            Runtime runtime = Runtime.getRuntime();
            QuotedStringTokenizer st = new QuotedStringTokenizer(options.post_compiler);
            int pc_size = st.countTokens();
            String[] javacCmd = new String[pc_size+2+compiler.outputFiles().size()];
            int j = 0;
            for (int i = 0; i < pc_size; i++) {
                javacCmd[j++] = st.nextToken();
            }
            javacCmd[j++] = "-classpath";
            javacCmd[j++] = options.constructPostCompilerClasspath();

            Iterator iter = compiler.outputFiles().iterator();
            for (; iter.hasNext(); j++) {
                javacCmd[j] = (String) iter.next();
            }

            if (Report.should_report(postcompile, 1)) {
                StringBuffer cmdStr = new StringBuffer();
                for (int i = 0; i < javacCmd.length; i++)
                    cmdStr.append(javacCmd[i]+" ");
                Report.report(1, "Executing post-compiler " + cmdStr);
            }

            try {
                Process proc = runtime.exec(javacCmd);

                InputStreamReader err = new InputStreamReader(proc.getErrorStream());

                try {
                    char[] c = new char[72];
                    int len;
                    StringBuffer sb = new StringBuffer();
                    while((len = err.read(c)) > 0) {
                        sb.append(String.valueOf(c, 0, len));
                    }

                    if (sb.length() != 0) {
                        eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, sb.toString());
                    }
                }
                finally {
                    err.close();
                }

                proc.waitFor();

                if (!options.keep_output_files) {
                  String[] rmCmd = new String[1+compiler.outputFiles().size()];
                  rmCmd[0] = "rm";
                  iter = compiler.outputFiles().iterator();
                  for (int i = 1; iter.hasNext(); i++)
                    rmCmd[i] = (String) iter.next();
                  runtime.exec(rmCmd);
                }

                if (proc.exitValue() > 0) {
                  eq.enqueue(ErrorInfo.POST_COMPILER_ERROR,
                                   "Non-zero return code: " + proc.exitValue());
                  return false;
                }
            }
            catch(Exception e) {
                eq.enqueue(ErrorInfo.POST_COMPILER_ERROR, e.getMessage());
                return false;
            }
        }
        return true;
    }

}
