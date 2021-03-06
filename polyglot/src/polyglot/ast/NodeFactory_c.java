/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.*;

/**
 * A <code>NodeFactory</code> constructs AST nodes.  All node construction
 * should go through this factory or by done with the <code>copy()</code>
 * method of <code>Node</code>.
 */
public class NodeFactory_c extends AbstractNodeFactory_c
{
    private final ExtFactory extFactory;
    private final DelFactory delFactory;
    
    // use an empty implementation of AbstractExtFactory_c and
    // AbstractDelFactory_c, so we don't need to do null checks
    protected static class EmptyExtFactory extends AbstractExtFactory_c { }
    protected static class EmptyDelFactory extends AbstractDelFactory_c { }
    
    public NodeFactory_c() {
        this(new EmptyExtFactory(), 
             new EmptyDelFactory()); 
    }
    public NodeFactory_c(ExtFactory extFactory) {
        this(extFactory, 
             new EmptyDelFactory()); 
    }
    public NodeFactory_c(ExtFactory extFactory,
                           DelFactory delFactory ) {
        this.extFactory = extFactory; 
        this.delFactory = delFactory; 
    }
    
    protected ExtFactory extFactory() {
        return this.extFactory;
    }

    protected DelFactory delFactory() {
        return this.delFactory;
    }

    /**
     * Utility method to find an instance of an Extension Factory
     */
    protected final ExtFactory findExtFactInstance(Class c) {
        ExtFactory e = extFactory();
        while (e != null) {
            if (c.isInstance(e)) {
                // the factory e is an instance of the class c
                return e;
            }
            e = e.nextExtFactory();
        }
        return null;
    }
    
    public FlagsNode FlagsNode(Position pos, Flags flags) {
	    FlagsNode n = new FlagsNode_c(pos, flags);
	    n = (FlagsNode) n.ext(extFactory.extFlagsNode());
	    n = (FlagsNode) n.del(delFactory.delFlagsNode());
	    return n;
    }
    public Id Id(Position pos, Name name) {
        Id n = new Id_c(pos, name);
        n = (Id) n.ext(extFactory.extId());
        n = (Id) n.del(delFactory.delId());
        return n;
    }

    public AmbPrefix AmbPrefix(Position pos, Prefix prefix, Id name) {
        AmbPrefix n = new AmbPrefix_c(pos, prefix, name);
        n = (AmbPrefix)n.ext(extFactory.extAmbPrefix());
        n = (AmbPrefix)n.del(delFactory.delAmbPrefix());
        return n;
    }
    
    public AmbReceiver AmbReceiver(Position pos, Prefix prefix, Id name) {
        AmbReceiver n = new AmbReceiver_c(pos, prefix, name);
        n = (AmbReceiver)n.ext(extFactory.extAmbReceiver());
        n = (AmbReceiver)n.del(delFactory.delAmbReceiver());
        return n;
    }

    public AmbQualifierNode AmbQualifierNode(Position pos, Prefix qualifier, Id name) {
        AmbQualifierNode n = new AmbQualifierNode_c(pos, qualifier, name);
        n = (AmbQualifierNode)n.ext(extFactory.extAmbQualifierNode());
        n = (AmbQualifierNode)n.del(delFactory.delAmbQualifierNode());
        return n;
    }
    
    public AmbExpr AmbExpr(Position pos, Id name) {
        AmbExpr n = new AmbExpr_c(pos, name);
        n = (AmbExpr)n.ext(extFactory.extAmbExpr());
        n = (AmbExpr)n.del(delFactory.delAmbExpr());
        return n;
    }
    
    public AmbTypeNode AmbTypeNode(Position pos, Prefix qualifier, Id name) {
        AmbTypeNode n = new AmbTypeNode_c(pos, qualifier, name);
        n = (AmbTypeNode)n.ext(extFactory.extAmbTypeNode());
        n = (AmbTypeNode)n.del(delFactory.delAmbTypeNode());
        return n;
    }

    public ArrayAccess ArrayAccess(Position pos, Expr base, Expr index) {
        ArrayAccess n = new ArrayAccess_c(pos, base, index);
        n = (ArrayAccess)n.ext(extFactory.extArrayAccess());
        n = (ArrayAccess)n.del(delFactory.delArrayAccess());
        return n;
    }

    public ArrayInit ArrayInit(Position pos, List<Expr> elements) {
        ArrayInit n = new ArrayInit_c(pos, CollectionUtil.nonNullList(elements));
        n = (ArrayInit)n.ext(extFactory.extArrayInit());
        n = (ArrayInit)n.del(delFactory.delArrayInit());
        return n;
    }

    public Assert Assert(Position pos, Expr cond, Expr errorMessage) {
        Assert n = new Assert_c(pos, cond, errorMessage);
        n = (Assert)n.ext(extFactory.extAssert());
        n = (Assert)n.del(delFactory.delAssert());
        return n;
    }

    public Assign Assign(Position pos, Expr left, Assign.Operator op, Expr right) {
        if (left instanceof Local) {
            return LocalAssign(pos, (Local)left, op, right);
        }
        else if (left instanceof Field) {
            return FieldAssign(pos, ((Field)left).target(), ((Field)left).name(), op, right);
        } 
        else if (left instanceof ArrayAccess) {
            return ArrayAccessAssign(pos, ((ArrayAccess)left).array(), ((ArrayAccess)left).index(), op, right);
        }
        else if (left instanceof Ambiguous) {
            return AmbAssign(pos, left, op, right);
        }
        else {
            throw new InternalCompilerError("Cannot create assignment to " + left);
        }
    }

    public LocalAssign LocalAssign(Position pos, Local left, Assign.Operator op, Expr right) {
        LocalAssign n = new LocalAssign_c(pos, left, op, right);
        n = (LocalAssign)n.ext(extFactory.extLocalAssign());
        n = (LocalAssign)n.del(delFactory.delLocalAssign());
        return n;
    }
    public FieldAssign FieldAssign(Position pos, Receiver target, Id name, Assign.Operator op, Expr right) {
        FieldAssign n = new FieldAssign_c(pos, target, name, op, right);
        n = (FieldAssign)n.ext(extFactory.extFieldAssign());
        n = (FieldAssign)n.del(delFactory.delFieldAssign());
        return n;
    }
    public ArrayAccessAssign ArrayAccessAssign(Position pos, Expr array, Expr index, Assign.Operator op, Expr right) {
        ArrayAccessAssign n = new ArrayAccessAssign_c(pos, array, index, op, right);
        n = (ArrayAccessAssign)n.ext(extFactory.extArrayAccessAssign());
        n = (ArrayAccessAssign)n.del(delFactory.delArrayAccessAssign());
        return n;
    }
    public AmbAssign AmbAssign(Position pos, Expr left, Assign.Operator op, Expr right) {
        AmbAssign n = new AmbAssign_c(pos, left, op, right);
        n = (AmbAssign)n.ext(extFactory.extAmbAssign());
        n = (AmbAssign)n.del(delFactory.delAmbAssign());
        return n;
    }

    public Binary Binary(Position pos, Expr left, Binary.Operator op, Expr right) {
        Binary n = new Binary_c(pos, left, op, right);
        n = (Binary)n.ext(extFactory.extBinary());
        n = (Binary)n.del(delFactory.delBinary());
        return n;
    }

    public Block Block(Position pos, List statements) {
        Block n = new Block_c(pos, CollectionUtil.nonNullList(statements));
        n = (Block)n.ext(extFactory.extBlock());
        n = (Block)n.del(delFactory.delBlock());
        return n;
    }

    public SwitchBlock SwitchBlock(Position pos, List statements) {
        SwitchBlock n = new SwitchBlock_c(pos, CollectionUtil.nonNullList(statements));
        n = (SwitchBlock)n.ext(extFactory.extSwitchBlock());
        n = (SwitchBlock)n.del(delFactory.delSwitchBlock());
        return n;
    }

    public BooleanLit BooleanLit(Position pos, boolean value) {
        BooleanLit n = new BooleanLit_c(pos, value);
        n = (BooleanLit)n.ext(extFactory.extBooleanLit());
        n = (BooleanLit)n.del(delFactory.delBooleanLit());
        return n;
    }

    public Branch Branch(Position pos, Branch.Kind kind, Id label) {
        Branch n = new Branch_c(pos, kind, label);
        n = (Branch)n.ext(extFactory.extBranch());
        n = (Branch)n.del(delFactory.delBranch());
        return n;
    }

    public Call Call(Position pos, Receiver target, Id name, List<Expr> args) {
        Call n = new Call_c(pos, target, name, CollectionUtil.nonNullList(args));
        n = (Call)n.ext(extFactory.extCall());
        n = (Call)n.del(delFactory.delCall());
        return n;
    }

    public Case Case(Position pos, Expr expr) {
        Case n = new Case_c(pos, expr);
        n = (Case)n.ext(extFactory.extCase());
        n = (Case)n.del(delFactory.delCase());
        return n;
    }

    public Cast Cast(Position pos, TypeNode type, Expr expr) {
        Cast n = new Cast_c(pos, type, expr);
        n = (Cast)n.ext(extFactory.extCast());
        n = (Cast)n.del(delFactory.delCast());
        return n;
    }

    public Catch Catch(Position pos, Formal formal, Block body) {
        Catch n = new Catch_c(pos, formal, body);
        n = (Catch)n.ext(extFactory.extCatch());
        n = (Catch)n.del(delFactory.delCatch());
        return n;
    }

    public CharLit CharLit(Position pos, char value) {
        CharLit n = new CharLit_c(pos, value);
        n = (CharLit)n.ext(extFactory.extCharLit());
        n = (CharLit)n.del(delFactory.delCharLit());
        return n;
    }

    public ClassBody ClassBody(Position pos, List<ClassMember> members) {
        ClassBody n = new ClassBody_c(pos, CollectionUtil.nonNullList(members));
        n = (ClassBody)n.ext(extFactory.extClassBody());
        n = (ClassBody)n.del(delFactory.delClassBody());
        return n;
    }
    
    public ClassDecl ClassDecl(Position pos, FlagsNode flags, Id name, TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
        ClassDecl n = new ClassDecl_c(pos, flags, name, superClass, CollectionUtil.nonNullList(interfaces), body);
        n = (ClassDecl)n.ext(extFactory.extClassDecl());
        n = (ClassDecl)n.del(delFactory.delClassDecl());
        return n;
    }

    public ClassLit ClassLit(Position pos, TypeNode typeNode) {
        ClassLit n = new ClassLit_c(pos, typeNode);
        n = (ClassLit)n.ext(extFactory.extClassLit());
        n = (ClassLit)n.del(delFactory.delClassLit());
        return n;
    }

    public Conditional Conditional(Position pos, Expr cond, Expr consequent, Expr alternative) {
        Conditional n = new Conditional_c(pos, cond, consequent, alternative);
        n = (Conditional)n.ext(extFactory.extConditional());
        n = (Conditional)n.del(delFactory.delConditional());
        return n;
    }

    public ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, Expr outer, List<Expr> args) {
        ConstructorCall n = new ConstructorCall_c(pos, kind, outer, CollectionUtil.nonNullList(args));
        n = (ConstructorCall)n.ext(extFactory.extConstructorCall());
        n = (ConstructorCall)n.del(delFactory.delConstructorCall());
        return n;
    }
    
    public ConstructorDecl ConstructorDecl(Position pos, FlagsNode flags, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        ConstructorDecl n = new ConstructorDecl_c(pos, flags, name, CollectionUtil.nonNullList(formals), CollectionUtil.nonNullList(throwTypes), body);
        n = (ConstructorDecl)n.ext(extFactory.extConstructorDecl());
        n = (ConstructorDecl)n.del(delFactory.delConstructorDecl());
        return n;
    }

    public FieldDecl FieldDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init) {
        FieldDecl n = new FieldDecl_c(pos, flags, type, name, init);
        n = (FieldDecl)n.ext(extFactory.extFieldDecl());
        n = (FieldDecl)n.del(delFactory.delFieldDecl());
        return n;
    }

    public Do Do(Position pos, Stmt body, Expr cond) {
        Do n = new Do_c(pos, body, cond);
        n = (Do)n.ext(extFactory.extDo());
        n = (Do)n.del(delFactory.delDo());
        return n;
    }

    public Empty Empty(Position pos) {
        Empty n = new Empty_c(pos);
        n = (Empty)n.ext(extFactory.extEmpty());
        n = (Empty)n.del(delFactory.delEmpty());
        return n;
    }

    public Eval Eval(Position pos, Expr expr) {
        Eval n = new Eval_c(pos, expr);
        n = (Eval)n.ext(extFactory.extEval());
        n = (Eval)n.del(delFactory.delEval());
        return n;
    }
    
    public Field Field(Position pos, Receiver target, Id name) {
        Field n = new Field_c(pos, target, name);
        n = (Field)n.ext(extFactory.extField());
        n = (Field)n.del(delFactory.delField());
        return n;
    }

    public FloatLit FloatLit(Position pos, FloatLit.Kind kind, double value) {
        FloatLit n = new FloatLit_c(pos, kind, value);
        n = (FloatLit)n.ext(extFactory.extFloatLit());
        n = (FloatLit)n.del(delFactory.delFloatLit());
        return n;
    }

    public For For(Position pos, List<ForInit> inits, Expr cond, List<ForUpdate> iters, Stmt body) {
        For n = new For_c(pos, CollectionUtil.nonNullList(inits), cond, CollectionUtil.nonNullList(iters), body);
        n = (For)n.ext(extFactory.extFor());
        n = (For)n.del(delFactory.delFor());
        return n;
    }

    public Formal Formal(Position pos, FlagsNode flags, TypeNode type, Id name) {
        Formal n = new Formal_c(pos, flags, type, name);
        n = (Formal)n.ext(extFactory.extFormal());
        n = (Formal)n.del(delFactory.delFormal());
        return n;
    }
    
    public If If(Position pos, Expr cond, Stmt consequent, Stmt alternative) {
        If n = new If_c(pos, cond, consequent, alternative);
        n = (If)n.ext(extFactory.extIf());
        n = (If)n.del(delFactory.delIf());
        return n;
    }
    
    public Import Import(Position pos, Import.Kind kind, QName name) {
        Import n = new Import_c(pos, kind, name);
        n = (Import)n.ext(extFactory.extImport());
        n = (Import)n.del(delFactory.delImport());
        return n;
    }

    public Initializer Initializer(Position pos, FlagsNode flags, Block body) {
        Initializer n = new Initializer_c(pos, flags, body);
        n = (Initializer)n.ext(extFactory.extInitializer());
        n = (Initializer)n.del(delFactory.delInitializer());
        return n;
    }

    public Instanceof Instanceof(Position pos, Expr expr, TypeNode type) {
        Instanceof n = new Instanceof_c(pos, expr, type);
        n = (Instanceof)n.ext(extFactory.extInstanceof());
        n = (Instanceof)n.del(delFactory.delInstanceof());
        return n;
    }

    public IntLit IntLit(Position pos, IntLit.Kind kind, long value) {
        IntLit n = new IntLit_c(pos, kind, value);
        n = (IntLit)n.ext(extFactory.extIntLit());
        n = (IntLit)n.del(delFactory.delIntLit());
        return n;
    }

    public Labeled Labeled(Position pos, Id label, Stmt body) {
        Labeled n = new Labeled_c(pos, label, body);
        n = (Labeled)n.ext(extFactory.extLabeled());
        n = (Labeled)n.del(delFactory.delLabeled());
        return n;
    }

    public Local Local(Position pos, Id name) {
        Local n = new Local_c(pos, name);
        n = (Local)n.ext(extFactory.extLocal());
        n = (Local)n.del(delFactory.delLocal());
        return n;
    }

    public LocalClassDecl LocalClassDecl(Position pos, ClassDecl decl) {
        LocalClassDecl n = new LocalClassDecl_c(pos, decl);
        n = (LocalClassDecl)n.ext(extFactory.extLocalClassDecl());
        n = (LocalClassDecl)n.del(delFactory.delLocalClassDecl());
        return n;
    }
    
    public LocalDecl LocalDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init) {
        LocalDecl n = new LocalDecl_c(pos, flags, type, name, init);
        n = (LocalDecl)n.ext(extFactory.extLocalDecl());
        n = (LocalDecl)n.del(delFactory.delLocalDecl());
        return n;
    }

    public MethodDecl MethodDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        MethodDecl n = new MethodDecl_c(pos, flags, returnType, name, CollectionUtil.nonNullList(formals), CollectionUtil.nonNullList(throwTypes), body);
        n = (MethodDecl)n.ext(extFactory.extMethodDecl());
        n = (MethodDecl)n.del(delFactory.delMethodDecl());
        return n;
    }

    public New New(Position pos, Expr outer, TypeNode objectType, List<Expr> args, ClassBody body) {
        New n = new New_c(pos, outer, objectType, CollectionUtil.nonNullList(args), body);
        n = (New)n.ext(extFactory.extNew());
        n = (New)n.del(delFactory.delNew());
        return n;
    }

    public NewArray NewArray(Position pos, TypeNode base, List dims, int addDims, ArrayInit init) {
        NewArray n = new NewArray_c(pos, base, CollectionUtil.nonNullList(dims), addDims, init);
        n = (NewArray)n.ext(extFactory.extNewArray());
        n = (NewArray)n.del(delFactory.delNewArray());
        return n;
    }
    
    public NodeList NodeList(Position pos, NodeFactory nf, List nodes) {
        NodeList n = new NodeList_c(pos, nf, nodes);
        n = (NodeList) n.ext(extFactory.extNodeList());
        n = (NodeList) n.del(delFactory.delNodeList());
        return n;
    }

    public NullLit NullLit(Position pos) {
        NullLit n = new NullLit_c(pos);
        n = (NullLit)n.ext(extFactory.extNullLit());
        n = (NullLit)n.del(delFactory.delNullLit());
        return n;
    }

    public Return Return(Position pos, Expr expr) {
        Return n = new Return_c(pos, expr);
        n = (Return)n.ext(extFactory.extReturn());
        n = (Return)n.del(delFactory.delReturn());
        return n;
    }

    public SourceCollection SourceCollection(Position pos, List sources) {
        SourceCollection n = new SourceCollection_c(pos, CollectionUtil.nonNullList(sources));
        n = (SourceCollection)n.ext(extFactory.extSourceCollection());
        n = (SourceCollection)n.del(delFactory.delSourceCollection());
        return n;
    }

    public SourceFile SourceFile(Position pos, PackageNode packageName, List<Import> imports, List<TopLevelDecl> decls) {
        SourceFile n = new SourceFile_c(pos, packageName, CollectionUtil.nonNullList(imports), CollectionUtil.nonNullList(decls));
        n = (SourceFile)n.ext(extFactory.extSourceFile());
        n = (SourceFile)n.del(delFactory.delSourceFile());
        return n;
    }

    public Special Special(Position pos, Special.Kind kind, TypeNode outer) {
         Special n = new Special_c(pos, kind, outer);
        n = (Special)n.ext(extFactory.extSpecial());
        n = (Special)n.del(delFactory.delSpecial());
        return n;
    }

    public StringLit StringLit(Position pos, String value) {
        StringLit n = new StringLit_c(pos, value);
        n = (StringLit)n.ext(extFactory.extStringLit());
        n = (StringLit)n.del(delFactory.delStringLit());
        return n;
    }

    public Switch Switch(Position pos, Expr expr, List elements) {
        Switch n = new Switch_c(pos, expr, CollectionUtil.nonNullList(elements));
        n = (Switch)n.ext(extFactory.extSwitch());
        n = (Switch)n.del(delFactory.delSwitch());
        return n;
    }

    public Synchronized Synchronized(Position pos, Expr expr, Block body) {
        Synchronized n = new Synchronized_c(pos, expr, body);
        n = (Synchronized)n.ext(extFactory.extSynchronized());
        n = (Synchronized)n.del(delFactory.delSynchronized());
        return n;
    }

    public Throw Throw(Position pos, Expr expr) {
        Throw n = new Throw_c(pos, expr);
        n = (Throw)n.ext(extFactory.extThrow());
        n = (Throw)n.del(delFactory.delThrow());
        return n;
    }

    public Try Try(Position pos, Block tryBlock, List catchBlocks, Block finallyBlock) {
        Try n = new Try_c(pos, tryBlock, CollectionUtil.nonNullList(catchBlocks), finallyBlock);
        n = (Try)n.ext(extFactory.extTry());
        n = (Try)n.del(delFactory.delTry());
        return n;
    }

    public ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base) {
        ArrayTypeNode n = new ArrayTypeNode_c(pos, base);
        n = (ArrayTypeNode)n.ext(extFactory.extArrayTypeNode());
        n = (ArrayTypeNode)n.del(delFactory.delArrayTypeNode());
        return n;
    }

    public CanonicalTypeNode CanonicalTypeNode(Position pos, Ref<? extends Type> type) {
        CanonicalTypeNode n = new CanonicalTypeNode_c(pos, type);
        n = (CanonicalTypeNode)n.ext(extFactory.extCanonicalTypeNode());
        n = (CanonicalTypeNode)n.del(delFactory.delCanonicalTypeNode());
        return n;
    }

    public PackageNode PackageNode(Position pos, Ref<? extends Package> p) {
        PackageNode n = new PackageNode_c(pos, p);
        n = (PackageNode)n.ext(extFactory.extPackageNode());
        n = (PackageNode)n.del(delFactory.delPackageNode());
        return n;
    }

    public Unary Unary(Position pos, Unary.Operator op, Expr expr) {
        Unary n = new Unary_c(pos, op, expr);
        n = (Unary)n.ext(extFactory.extUnary());
        n = (Unary)n.del(delFactory.delUnary());
        return n;
    }

    public While While(Position pos, Expr cond, Stmt body) {
        While n = new While_c(pos, cond, body);
        n = (While)n.ext(extFactory.extWhile());
        n = (While)n.del(delFactory.delWhile());
        return n;
    }
}
