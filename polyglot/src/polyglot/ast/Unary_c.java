package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import java.util.*;

/**
 * A <code>Unary</code> represents a Java unary expression, an
 * immutable pair of an expression and an an operator.
 */
public class Unary_c extends Expr_c implements Unary
{
    protected Unary.Operator op;
    protected Expr expr;

    public Unary_c(Del ext, Position pos, Unary.Operator op, Expr expr) {
	super(ext, pos);
	this.op = op;
	this.expr = expr;
    }

    /** Get the precedence of the expression. */
    public Precedence precedence() {
	return Precedence.UNARY;
    }

    /** Get the sub-expression of the expression. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the sub-expression of the expression. */
    public Unary expr(Expr expr) { Unary_c n = (Unary_c) copy(); n.expr = expr;
      return n; }

    /** Get the operator. */
    public Unary.Operator operator() {
	return this.op;
    }

    /** Set the operator. */
    public Unary operator(Unary.Operator op) {
	Unary_c n = (Unary_c) copy();
	n.op = op;
	return n;
    }

    /** Reconstruct the expression. */
    protected Unary_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Unary_c n = (Unary_c) copy();
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(expr);
    }

    protected Node num(NodeFactory nf, long value) {
        // Unary promotion
        IntLit.Kind kind = IntLit.INT;

        if (expr instanceof IntLit) {
            kind = ((IntLit) expr).kind();
        }

        return nf.IntLit(position(), kind, value).type(type());
    }

    protected Node bool(NodeFactory nf, boolean value) {
        return nf.BooleanLit(position(), value).type(type());
    }

    /** Fold constants for the expression. */
    public Node foldConstants(ConstantFolder cf) {
      	NodeFactory nf = cf.nodeFactory();

        if (expr instanceof NumLit) {
	    long x = ((NumLit) expr).longValue();
	    if (op == BIT_NOT) return num(nf, ~x);
	    if (op == NEG) return num(nf, -x);
	    if (op == POS) return num(nf, +x);
	}
	else if (expr instanceof BooleanLit) {
	    boolean x = ((BooleanLit) expr).value();
	    if (op == NOT) return bool(nf, !x);
	}

        return this;
    }

    /** Type check the expression. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

	if (op == POST_INC || op == POST_DEC ||
	    op == PRE_INC || op == PRE_DEC) {

	    if (! expr.type().isNumeric()) {
		throw new SemanticException("Operand of " + op +
		    " operator must be numeric.", expr.position());
	    }

	    return type(expr.type());
	}

	if (op == BIT_NOT) {
	    if (! ts.isImplicitCastValid(expr.type(), ts.Long())) {
		throw new SemanticException("Operand of " + op +
		    " operator must be numeric.", expr.position());
	    }

	    return type(ts.promote(expr.type()));
	}

	if (op == NEG || op == POS) {
	    if (! expr.type().isNumeric()) {
		throw new SemanticException("Operand of " + op +
		    " operator must be numeric.", expr.position());
	    }

	    return type(ts.promote(expr.type()));
	}

	if (op == NOT) {
	    if (! expr.type().isBoolean()) {
		throw new SemanticException("Operand of " + op +
		    " operator must be boolean.", expr.position());
	    }

	    return type(expr.type());
	}

	return this;
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == expr) {
            if (op == POST_INC || op == POST_DEC ||
                op == PRE_INC || op == PRE_DEC) {
                return ts.Double();
            }
            else if (op == NEG || op == POS) {
                return ts.Double();
            }
            else if (op == BIT_NOT) {
                return ts.Long();
            }
            else if (op == NOT) {
                return ts.Boolean();
            }
        }

        return child.type();
    }

    /** Check exceptions thrown by the statement. */
    public String toString() {
        if (op.isPrefix()) {
	    return op.toString() + expr.toString();
	}
	else {
	    return expr.toString() + op.toString();
	}
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (op.isPrefix()) {
	    w.write(op.toString());
	    printSubExpr(expr, w, tr);
	}
	else {
	    printSubExpr(expr, w, tr);
	    w.write(op.toString());
	}
    }

    public Term entry() {
        return expr.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(expr, this);
        return succs;
    }

    public Object constantValue() {
        Object v = expr.constantValue();

        if (v instanceof Boolean) {
            boolean vv = ((Boolean) v).booleanValue();
            if (op == NOT) return new Boolean(!vv);
        }
        if (v instanceof Double) {
            double vv = ((Double) v).doubleValue();
            if (op == POS) return new Double(+vv);
            if (op == NEG) return new Double(-vv);
        }
        if (v instanceof Float) {
            float vv = ((Float) v).floatValue();
            if (op == POS) return new Float(+vv);
            if (op == NEG) return new Float(-vv);
        }
        if (v instanceof Long) {
            long vv = ((Long) v).longValue();
            if (op == BIT_NOT) return new Long(~vv);
            if (op == POS) return new Long(+vv);
            if (op == NEG) return new Long(-vv);
        }
        if (v instanceof Integer) {
            int vv = ((Integer) v).intValue();
            if (op == BIT_NOT) return new Integer(~vv);
            if (op == POS) return new Integer(+vv);
            if (op == NEG) return new Integer(-vv);
        }
        if (v instanceof Character) {
            char vv = ((Character) v).charValue();
            if (op == BIT_NOT) return new Integer(~vv);
            if (op == POS) return new Integer(+vv);
            if (op == NEG) return new Integer(-vv);
        }
        if (v instanceof Short) {
            short vv = ((Short) v).shortValue();
            if (op == BIT_NOT) return new Integer(~vv);
            if (op == POS) return new Integer(+vv);
            if (op == NEG) return new Integer(-vv);
        }
        if (v instanceof Byte) {
            byte vv = ((Byte) v).byteValue();
            if (op == BIT_NOT) return new Integer(~vv);
            if (op == POS) return new Integer(+vv);
            if (op == NEG) return new Integer(-vv);
        }

        // not a constant
        return null;
    }
}
