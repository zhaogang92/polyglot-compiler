/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.HashSet;
import java.util.Set;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;

/** Visitor which ensures that field intializers and initializers do not
 * make illegal forward references to fields.
 *  This is an implementation of the rules of the Java Language Spec, 2nd
 * Edition, Section 8.3.2.3 
 */
public class FwdReferenceChecker extends ContextVisitor
{
    public FwdReferenceChecker(Job job, TypeSystem ts, NodeFactory nf) {
	super(job, ts, nf);
    }

    private boolean inInitialization = false;
    private boolean inStaticInit = false;
    private Set<FieldDef> declaredFields = new HashSet<FieldDef>();
    
    protected NodeVisitor enterCall(Node n) throws SemanticException {
        if (n instanceof FieldDecl) {
            FieldDecl fd = (FieldDecl)n;

            FwdReferenceChecker frc = (FwdReferenceChecker)this.copy();
            frc.inInitialization = true;
            frc.inStaticInit = fd.flags().flags().isStatic();

            frc.declaredFields = new HashSet<FieldDef>(declaredFields);
            declaredFields.add(fd.fieldDef());
            return frc;
        }
        else if (n instanceof Initializer) {
            FwdReferenceChecker frc = (FwdReferenceChecker)this.copy();
            frc.inInitialization = true;
            frc.inStaticInit = ((Initializer)n).flags().flags().isStatic();
            return frc;
        }
        else if (n instanceof FieldAssign) {
            FwdReferenceChecker frc = (FwdReferenceChecker)this.copy();
            return frc;
        }
        else if (n instanceof Field) {
            if (inInitialization) {
                // we need to check if this is an illegal fwd reference.
                Field f = (Field)n;
                
                // an illegal fwd reference if a usage of an instance 
                // (resp. static) field occurs in an instance (resp. static)
                // initialization, and the innermost enclosing class or 
                // interface of the usage is the same as the container of
                // the field, and we have not yet seen the field declaration.
                //
                // In addition, if a field is not accessed as a simple name, 
                // then all is ok
                
                ClassType currentClass = context().currentClass();
                StructType fContainer = f.fieldInstance().container();

                if (inStaticInit == f.fieldInstance().flags().isStatic() &&
                    currentClass.typeEquals(fContainer, context) &&
                   !declaredFields.contains(f.fieldInstance().def()) &&
                   f.isTargetImplicit()) {
                    throw new SemanticException("Illegal forward reference", 
                                                f.position());
                }
            }
        }
        return this;        
    }
}

