package jltools.frontend;

import jltools.ast.Node;
import jltools.visit.NodeVisitor;
import jltools.util.*;

/** A pass which runs a visitor. */
public class VisitorPass extends AbstractPass
{
    Job job;
    NodeVisitor v;

    public VisitorPass(Pass.ID id, Job job) {
	this(id, job, null);
    }

    public VisitorPass(Pass.ID id, Job job, NodeVisitor v) {
        super(id);
	this.job = job;
	this.v = v;
    }

    public void visitor(NodeVisitor v) {
	this.v = v;
    }

    public NodeVisitor visitor() {
	return v;
    }

    public boolean run() {
	Node ast = job.ast();

	if (ast == null) {
	    throw new InternalCompilerError("Null AST: did the parser run?");
	}

        if (v.begin()) {
	    ErrorQueue q = job.compiler().errorQueue();
	    int nErrsBefore = q.errorCount();

            ast = ast.visit(v);
            v.finish();

            int nErrsAfter = q.errorCount();

            job.ast(ast);

            return (nErrsBefore == nErrsAfter);
            // because, if they're equal, no new errors occured,
            // so the run was successful.
        }

        return false;
    }

    public String toString() {
	return id.toString();
    }
}
