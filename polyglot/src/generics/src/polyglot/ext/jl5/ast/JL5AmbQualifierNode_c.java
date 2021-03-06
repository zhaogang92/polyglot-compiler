package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.AmbQualifierNode_c;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Prefix;
import polyglot.ast.QualifierNode;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class JL5AmbQualifierNode_c extends AmbQualifierNode_c implements JL5AmbQualifierNode {

    protected List typeArguments;
    
    public JL5AmbQualifierNode_c(Position pos, Prefix qual, Id name, List typeArguments){
        super(pos, qual, name);
        this.typeArguments = typeArguments;
    }
    
    public List typeArguments(){
        return typeArguments;
    }
    
    public JL5AmbQualifierNode typeArguments(List args){
        JL5AmbQualifierNode_c n = (JL5AmbQualifierNode_c) copy();
        n.typeArguments = args;
        return n;
    }

    public Node disambiguate(ContextVisitor sc) throws SemanticException {
        Node n = sc.nodeFactory().disamb().disambiguate(this, sc, position(), qual, name);
        // Create a parameterized type out of the JL5ParsedClassType
        if (n instanceof CanonicalTypeNode && ((CanonicalTypeNode)n).type() instanceof JL5ParsedClassType) {
        	JL5TypeSystem ts = (JL5TypeSystem) sc.typeSystem();
        	JL5ParsedClassType ct = (JL5ParsedClassType) ((CanonicalTypeNode)n).type();
            if (typeArguments.isEmpty()) {
                if (ct.isGeneric()) { // it's a raw type
                    ct = ts.rawType(ct);
                }
               	// it's a non-generic type, just return it.  
            } else {
                ParameterizedType pt = ((JL5TypeSystem)sc.typeSystem()).parameterizedType(ct);
                ArrayList typeArgs = new ArrayList(typeArguments.size());
                for (Iterator it = typeArguments.iterator(); it.hasNext(); ){
                    TypeNode tn = (TypeNode)it.next();
                    Type t = tn.type();
                    typeArgs.add(t);
                }
                pt.typeArguments(typeArgs);
                ct = pt;
            }
            CanonicalTypeNode an = sc.nodeFactory().CanonicalTypeNode(n.position(), ct);
            return an;
        }
        else if (n instanceof QualifierNode){
            return n;
        }
        throw new SemanticException("Could not find type or package \"" +
                (qual == null ? name : qual.toString() + "." + name) +
                "\".", position());
    }
}
