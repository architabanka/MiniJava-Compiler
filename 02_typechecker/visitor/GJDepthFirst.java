package visitor;
import syntaxtree.*;
import visitor.GJDepthFirst.MethodInfo;

import java.util.*;

public class GJDepthFirst<R,A> implements GJVisitor<R,A>{
    public static class TypeException extends RuntimeException {}
    public static class SymbolNotFoundException extends RuntimeException {}

    public static class MiniJavaType{
        public String name; 
        public MiniJavaType(String name){
            this.name = name;
        }
        public static final MiniJavaType INT = new MiniJavaType("int");
        public static final MiniJavaType BOOLEAN = new MiniJavaType("boolean");
        public static final MiniJavaType INT_ARRAY = new MiniJavaType("int[]");

        public boolean isInt(){return "int".equals(name);}
        public boolean isBoolean(){return "boolean".equals(name);}
        public boolean isIntArray(){return "int[]".equals(name);}
        public boolean isClass(){ return !isInt()&& !isBoolean() && !isIntArray();}

        @Override
        public boolean equals(Object o){
            if(this==o) return true;
            if(!(o instanceof MiniJavaType))return false;
            return name.equals(((MiniJavaType)o).name);
        }

        @Override
        public int hashCode(){return name.hashCode();}

        @Override
        public String toString(){return name;}

    } 
    public static class ClassInfo{
        public String class_name;
        public String parent_name;
        public Map<String, MiniJavaType> fields = new HashMap<String, MiniJavaType>();
        public Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();

    }
    public static class MethodInfo{
        public String method_name;
        public MiniJavaType return_type;
        public List<MiniJavaType> param_types = new ArrayList<MiniJavaType>();
        public List<String> param_names= new ArrayList<String>();
        public Map<String, MiniJavaType> local_vars = new HashMap<String, MiniJavaType>();
    }

    public static class SymbolTable{
        public Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();
    }

    public static class SymbolTableVisitor extends GJDepthFirst<Object,Object>{
        public SymbolTable symbolTable;
        public ClassInfo currentClass;
        public MethodInfo currentMethod;
        public String mainClassName;

        public SymbolTableVisitor(SymbolTable st){
            this.symbolTable=st;
        }
        public String getName(Identifier id){
            return id.f0.tokenImage;
        }
        @Override
        public Object visit (Goal g, Object argu){
            g.f0.accept(this,null);
            g.f1.accept(this, null);
            return null;
        }
        @Override
        public Object visit(MainClass n, Object argu) {
            mainClassName=getName(n.f1);
            ClassInfo mainClass=new ClassInfo();
            mainClass.class_name=mainClassName;
            symbolTable.classes.put(mainClassName,mainClass);
            currentClass=mainClass;
            currentMethod=null;
            n.f14.accept(this, null);
            currentClass=null;
            return null;
        }

        @Override
        public Object visit(ClassDeclaration n, Object argu){
            String className=getName(n.f1);
            if(symbolTable.classes.containsKey(className)){
                throw new TypeException();
            }
            ClassInfo classN=new ClassInfo();
            classN.class_name=className;
            symbolTable.classes.put(className,classN);
            currentClass=classN;
            currentMethod=null;
            if(n.f3.present())n.f3.accept(this,null);
            if(n.f4.present())n.f4.accept(this,null);
            currentClass=null;
            return null;

        }

        @Override
        public Object visit(ClassExtendsDeclaration n,Object argu){
            String className=getName(n.f1);
            String parentName=getName(n.f3);
            if(symbolTable.classes.containsKey(className)){
                throw new TypeException();
            }
            ClassInfo classN=new ClassInfo();
            classN.class_name=className;
            classN.parent_name=parentName;
            symbolTable.classes.put(className,classN);
            currentClass=classN;
            currentMethod=null;
            if(n.f5.present())n.f5.accept(this,null);
            if(n.f6.present())n.f6.accept(this,null);
            currentClass=null;
            return null;

        }
        @Override 
        public Object visit(VarDeclaration n, Object argu) {
            MiniJavaType type=(MiniJavaType) n.f0.accept(this,null);
            String name=getName(n.f1);
            if(currentMethod!=null){
                if (currentMethod.local_vars.containsKey(name)||currentMethod.param_names.contains(name)) {
                    throw new TypeException();
                }
                currentMethod.local_vars.put(name,type);
            } else if(currentClass!=null){
                if(currentClass.fields.containsKey(name)) {
                    throw new TypeException();
                }
                currentClass.fields.put(name,type);
            }
            return null;
        }

        @Override
        public Object visit(MethodDeclaration n, Object argu){
            MiniJavaType returnType=(MiniJavaType)n.f1.accept(this, null);
            String methodName=getName(n.f2);
            if(currentClass.methods.containsKey(methodName)){
                throw new TypeException();
            }
            MethodInfo mn=new MethodInfo();
            mn.method_name=methodName;
            mn.return_type=returnType;
            currentMethod=mn;
            if(n.f4.present()){n.f4.accept(this, null);}
            if(n.f7.present()){n.f7.accept(this, null);}
            currentClass.methods.put(methodName, mn);
            currentMethod=null;
            return null;
        }
        @Override
        public Object visit(FormalParameter n,Object argu){
            MiniJavaType type=(MiniJavaType)n.f0.accept(this, null);
            String name=getName(n.f1);
            if(currentMethod.param_names.contains(name)){
                throw new TypeException();
            }
            currentMethod.param_names.add(name);
            currentMethod.param_types.add(type);
            return null;
        }

        @Override
        public Object visit(Type n, Object argu) {
            return n.f0.accept(this, null);
        }
        @Override
        public Object visit(ArrayType n, Object argu) {
            return MiniJavaType.INT_ARRAY;
        }

        @Override
        public Object visit(BooleanType n, Object argu) {
            return MiniJavaType.BOOLEAN;
        }

        @Override
        public Object visit(IntegerType n, Object argu) {
            return MiniJavaType.INT;
        }

        @Override
        public Object visit(Identifier n, Object argu) {
           return new MiniJavaType(n.f0.tokenImage);
        }
        private boolean isSubtype(SymbolTable st, MiniJavaType t1, MiniJavaType t2){
            if(t1.equals(t2)) return true;
            if(t1.isClass()&&t2.isClass()){
                String curr=t1.name;
                while(curr!=null){
                    if(curr.equals(t2.name))return true;
                    ClassInfo c=st.classes.get(curr);
                    if(c==null) return false;
                    curr=c.parent_name;
                }
            }
            return false;
        }

        public void validateSymbolTable(SymbolTable st){
            Set<String> gValidated=new HashSet<>();
            for(ClassInfo c:st.classes.values()){
                Set<String>vis=new HashSet<>();
                String curr=c.class_name;
                while(curr!=null){
                    if(gValidated.contains(curr)){
                        break; 
                    }
                    if(!st.classes.containsKey(curr)){
                        throw new SymbolNotFoundException();
                    }
                    if(!vis.add(curr)){
                        throw new TypeException(); 
                    }
                    ClassInfo info=st.classes.get(curr);
                    curr=info.parent_name;
                }
                gValidated.addAll(vis);
            }
                 
            for(ClassInfo c:st.classes.values()){
                for(MiniJavaType t:c.fields.values()){
                    if(t.isClass()&&!st.classes.containsKey(t.name)){
                        throw new SymbolNotFoundException();
                    }
                }

                for(MethodInfo m:c.methods.values()){
                    if(m.return_type.isClass()&&!st.classes.containsKey(m.return_type.name)){
                        throw new SymbolNotFoundException();
                    }
                    for(MiniJavaType t:m.param_types){
                        if(t.isClass()&&!st.classes.containsKey(t.name)){
                            throw new SymbolNotFoundException();
                        }
                    }
                    for(MiniJavaType t:m.local_vars.values()){
                        if(t.isClass()&&!st.classes.containsKey(t.name)){
                            throw new SymbolNotFoundException();
                        }
                    }
                }
            }
            for(ClassInfo c:st.classes.values()){
                for (MethodInfo childMethod:c.methods.values()){
                    String parentName=c.parent_name;
                    while(parentName!=null) {
                        ClassInfo parentClass=st.classes.get(parentName);
                        
                        if(parentClass.methods.containsKey(childMethod.method_name)){
                            MethodInfo parentMethod=parentClass.methods.get(childMethod.method_name);

                            if (!isSubtype(st, childMethod.return_type, parentMethod.return_type)){
                                throw new TypeException();
                            }
                            if (childMethod.param_types.size()!=parentMethod.param_types.size()){
                                throw new TypeException();
                            }
                            for(int i=0;i<childMethod.param_types.size();i++){
                                if (!childMethod.param_types.get(i).equals(parentMethod.param_types.get(i))){
                                    throw new TypeException();
                                }
                            }
                            break; 
                        }
                        parentName=parentClass.parent_name;
                    }
                }
            }

        }
        
    }

    public static class TypeCheckVisitor extends GJDepthFirst<MiniJavaType, Object>{
        public SymbolTable symbolTable;
        public ClassInfo currentClass;
        public MethodInfo currentMethod;

        public TypeCheckVisitor(SymbolTable st){
            this.symbolTable = st;
        }
        private String getName(Identifier id){
            return id.f0.tokenImage;
        }
        @Override
        public MiniJavaType visit(Goal n, Object argu) {
           n.f0.accept(this, null);
           n.f1.accept(this, null);
           return null;
        }
        @Override
        public MiniJavaType visit(MainClass n, Object argu) {
           currentClass=symbolTable.classes.get(getName(n.f1));
           currentMethod=null;
           n.f14.accept(this,null);
           currentClass=null;
           return null;
        }
        @Override
        public MiniJavaType visit(ClassDeclaration n, Object argu) {
            currentClass=symbolTable.classes.get(getName(n.f1));
            currentMethod=null;
            if(n.f4.present()){n.f4.accept(this, null);}
            currentClass=null;
            return null;
        }

        @Override
        public MiniJavaType visit(ClassExtendsDeclaration n, Object argu) {
            currentClass=symbolTable.classes.get(getName(n.f1));
            currentMethod = null;
            if (n.f6.present()) {
                n.f6.accept(this, null);
            }
            currentClass = null;
            return null;
        }
        @Override
        public MiniJavaType visit(MethodDeclaration n, Object argu) {
            currentMethod=currentClass.methods.get(getName(n.f2));
            if (n.f8.present()) {
                n.f8.accept(this,null);
            }
            MiniJavaType retType=n.f10.accept(this, null);
            if(!isSubtype(retType,currentMethod.return_type)){
                throw new TypeException();
            }
            currentMethod=null;
            return null;
        }
        private boolean isSubtype(MiniJavaType t1, MiniJavaType t2){
            if(t1.equals(t2)) return true;
            if(t1.isClass()&&t2.isClass()){
                String curr=t1.name;
                while(curr!=null){
                    if(curr.equals(t2.name))return true;
                    ClassInfo c=symbolTable.classes.get(curr);
                    if(c==null) return false;
                    curr=c.parent_name;
                }
            }
            return false;
        }
        @Override
        public MiniJavaType visit(Statement n,Object argu){
           n.f0.accept(this,argu);
           return null;
        }
        @Override
        public MiniJavaType visit(Block n,Object argu){
           if(n.f1.present()){n.f1.accept(this, argu);}
           return null;
        }
        @Override
        public MiniJavaType visit(AssignmentStatement n, Object argu){
            MiniJavaType lhs=LookupVar(getName(n.f0));
            MiniJavaType rhs=n.f2.accept(this, argu);
            if(!isSubtype(rhs, lhs))throw new TypeException();
            return null;
        }
        private MiniJavaType LookupVar(String name){
            if(currentMethod!=null){
                if(currentMethod.local_vars.containsKey(name)){
                    return currentMethod.local_vars.get(name);
                }
                int idx=currentMethod.param_names.indexOf(name);
                if(idx!=-1){
                    return currentMethod.param_types.get(idx);
                }
            }
            if(currentClass!=null){
                String curr=currentClass.class_name;
                while(curr!=null){
                    ClassInfo inf= symbolTable.classes.get(curr);
                    if(inf!=null&&inf.fields.containsKey(name)){
                        return inf.fields.get(name);
                    }
                    curr=(inf==null)? null:inf.parent_name;
                }
            }
            throw new SymbolNotFoundException();
            
        }
        @Override
        public MiniJavaType visit(ArrayAssignmentStatement n,Object argu){
            MiniJavaType arr=LookupVar(getName(n.f0));
            if(!arr.isIntArray()){
                throw new TypeException();
            }
            MiniJavaType idx=n.f2.accept(this,null);
            if(!idx.isInt()){
                throw new TypeException();
            }
            MiniJavaType val=n.f5.accept(this,null);
            if(!val.isInt()){
                throw new TypeException();
            }
            return null;
        }
        @Override
        public MiniJavaType visit(IfthenElseStatement n, Object argu){
            MiniJavaType cond=n.f2.accept(this,null);
            if(!cond.isBoolean()){
                throw new TypeException();
            }
            n.f4.accept(this, null);
            n.f6.accept(this, null);
            return null;
        }
        @Override 
        public MiniJavaType visit(IfthenStatement n,Object argu){
            MiniJavaType cond=n.f2.accept(this,null);
            if(!cond.isBoolean()){
                throw new TypeException();
            }
            n.f4.accept(this, null);
            return null;
        }
        @Override
        public MiniJavaType visit(WhileStatement n, Object argu) {
            MiniJavaType cond=n.f2.accept(this, null);
            if (!cond.isBoolean()) {
                throw new TypeException();
            }
            n.f4.accept(this, null);
            return null;
        
        }
        @Override
        public MiniJavaType visit(PrintStatement n, Object argu) {
            MiniJavaType cond=n.f2.accept(this, null);
            if (!cond.isInt()) {
                throw new TypeException();
            }
            return null;
        }
        @Override
        public MiniJavaType visit(Expression n, Object argu) {
            return n.f0.accept(this, argu);
        }
        @Override
        public MiniJavaType visit(AndExpression n, Object argu) {
            MiniJavaType lhs=n.f0.accept(this, null);
            MiniJavaType rhs=n.f2.accept(this, null);
            if(!lhs.isBoolean()||!rhs.isBoolean()){
                throw new TypeException();
            }
            return MiniJavaType.BOOLEAN;
        }
        @Override
        public MiniJavaType visit(OrExpression n, Object argu) {
            MiniJavaType lhs=n.f0.accept(this, null);
            MiniJavaType rhs=n.f2.accept(this, null);
            if(!lhs.isBoolean()||!rhs.isBoolean()){
                throw new TypeException();
            }
            return MiniJavaType.BOOLEAN;
        }
        @Override
        public MiniJavaType visit(CompareExpression n, Object argu) {
           MiniJavaType lhs = n.f0.accept(this, null);
           MiniJavaType rhs = n.f2.accept(this, null);
           if (!lhs.isInt()||!rhs.isInt()) {
              throw new TypeException();
           }
           return MiniJavaType.BOOLEAN;
        }
        @Override
        public MiniJavaType visit(neqExpression n, Object argu) {
            MiniJavaType lhs = n.f0.accept(this, null);
            MiniJavaType rhs = n.f2.accept(this, null);
            if(lhs.isInt()&&rhs.isInt()) {
                return MiniJavaType.BOOLEAN;
            }
            if(lhs.isBoolean()&&rhs.isBoolean()){
                return MiniJavaType.BOOLEAN;
            }
            if(lhs.isIntArray()&&rhs.isIntArray()){
                return MiniJavaType.BOOLEAN;
            }
            if(lhs.isClass()&&rhs.isClass()){
                if (isSubtype(lhs, rhs) || isSubtype(rhs, lhs)) {
                    return MiniJavaType.BOOLEAN;
                }
            }
            throw new TypeException();
        }

        @Override
        public MiniJavaType visit(PlusExpression n, Object argu) {
            MiniJavaType lhs = n.f0.accept(this, null);
            MiniJavaType rhs = n.f2.accept(this, null);
            if (!lhs.isInt() || !rhs.isInt()) {
                throw new TypeException();
            }
            return MiniJavaType.INT;
        }

        @Override
        public MiniJavaType visit(MinusExpression n, Object argu) {
            MiniJavaType lhs = n.f0.accept(this, null);
            MiniJavaType rhs = n.f2.accept(this, null);
            if (!lhs.isInt() || !rhs.isInt()) {
                throw new TypeException();
            }
            return MiniJavaType.INT;
        }

        @Override
        public MiniJavaType visit(TimesExpression n, Object argu) {
            MiniJavaType lhs = n.f0.accept(this, null);
            MiniJavaType rhs = n.f2.accept(this, null);
            if (!lhs.isInt() || !rhs.isInt()) {
                throw new TypeException();
            }
            return MiniJavaType.INT;
        }

        @Override
        public MiniJavaType visit(DivExpression n, Object argu) {
            MiniJavaType lhs = n.f0.accept(this, null);
            MiniJavaType rhs = n.f2.accept(this, null);
            if (!lhs.isInt() || !rhs.isInt()) {
                throw new TypeException();
            }
            return MiniJavaType.INT;
        }
        @Override
        public MiniJavaType visit(ArrayLookup n, Object argu) {
            MiniJavaType arr=n.f0.accept(this, null);
            if(!arr.isIntArray()){
                throw new TypeException();
            }
            MiniJavaType idx=n.f2.accept(this, null);
            if(!idx.isInt()){
                throw new TypeException();
            }
            return MiniJavaType.INT;
        }
  
        @Override
        public MiniJavaType visit(ArrayLength n, Object argu) {
           MiniJavaType arr=n.f0.accept(this, null);
            if(!arr.isIntArray()){
                throw new TypeException();
            }
           return MiniJavaType.INT;
        }
        @Override
        public MiniJavaType visit(BracketExpression n, Object argu) {
            return n.f1.accept(this, null);
        }
        @Override
        public MiniJavaType visit(NotExpression n, Object argu) {
            MiniJavaType cond = n.f1.accept(this, null);
            if(!cond.isBoolean()){
                throw new TypeException();
            }
            return MiniJavaType.BOOLEAN;
        } 
        @Override
        public MiniJavaType visit(AllocationExpression n, Object argu) {
            String cName = getName(n.f1);
            if(!symbolTable.classes.containsKey(cName)){
                throw new SymbolNotFoundException();
            }
            return new MiniJavaType(cName);
        }
        @Override
        public MiniJavaType visit(ArrayAllocationExpression n, Object argu) {
           MiniJavaType sz=n.f3.accept(this, null);
           if(!sz.isInt()){
              throw new TypeException();
           }
           return MiniJavaType.INT_ARRAY;
        }
        @Override
        public MiniJavaType visit(ThisExpression n, Object argu) {
           if(currentClass==null || currentMethod==null) {
              throw new TypeException();
           }
           return new MiniJavaType(currentClass.class_name);
        }
        @Override
        public MiniJavaType visit(IntegerLiteral n, Object argu){
           return MiniJavaType.INT;
        }
  
        @Override
        public MiniJavaType visit(TrueLiteral n, Object argu){
           return MiniJavaType.BOOLEAN;
        }
  
        @Override
        public MiniJavaType visit(FalseLiteral n, Object argu){
           return MiniJavaType.BOOLEAN;
        }
  
        @Override
        public MiniJavaType visit(Identifier n, Object argu){
           return LookupVar(n.f0.tokenImage);
        }
        @Override
        public MiniJavaType visit(PrimaryExpression n, Object argu) {
           return n.f0.accept(this, argu);
        }
        @Override
        public MiniJavaType visit(MessageSend n, Object argu) {
            MiniJavaType obj=n.f0.accept(this, null);
            if(!obj.isClass()){
                throw new TypeException();
            }
            String mName=getName(n.f2);
            MethodInfo mInfo=LookupMethod(obj.name,mName);

            List<MiniJavaType>argTypes=new ArrayList<MiniJavaType>();
            if(n.f4.present()){
                n.f4.accept(this, argTypes);
            }

            if(argTypes.size()!=mInfo.param_types.size()){
                throw new TypeException();
            }
            for(int i=0;i<argTypes.size();i++){
                if(!isSubtype(argTypes.get(i),mInfo.param_types.get(i))){
                throw new TypeException();
                }
            }
            return mInfo.return_type;
        }
        private MethodInfo LookupMethod(String className, String methodName){
            String curr=className;
            while(curr!=null){
                ClassInfo m=symbolTable.classes.get(curr);
                if(m==null){
                    throw new SymbolNotFoundException();
                }
                if(m.methods.containsKey(methodName)){
                    return m.methods.get(methodName);
                }
                curr=m.parent_name;
            }
            throw new SymbolNotFoundException();
        }
        @Override
        public MiniJavaType visit(ExpressionList n,Object argu){
            List<MiniJavaType> list=(List<MiniJavaType>)argu;
            list.add(n.f0.accept(this, null));
            if (n.f1.present()) {
                n.f1.accept(this, list);
            }
            return null;
        }

        @Override
        public MiniJavaType visit(ExpressionRest n, Object argu){
            List<MiniJavaType> list=(List<MiniJavaType>)argu;
            list.add(n.f1.accept(this, null));
            return null;
        }
    }
    //
    // Auto class visitors--probably don't need to be overridden.
  
    public R visit(NodeList n, A argu) {
        R _ret=null;
        int _count=0;
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
        e.nextElement().accept(this,argu);
        _count++;
        }
        return _ret;
    }

    public R visit(NodeListOptional n, A argu) {
        if ( n.present() ) {
        R _ret=null;
        int _count=0;
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            e.nextElement().accept(this,argu);
            _count++;
        }
        return _ret;
        }
        else
        return null;
    }

    public R visit(NodeOptional n, A argu) {
        if ( n.present() )
        return n.node.accept(this,argu);
        else
        return null;
    }

    public R visit(NodeSequence n, A argu) {
        R _ret=null;
        int _count=0;
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            e.nextElement().accept(this,argu);
            _count++;
        }
        return _ret;
    }

    public R visit(NodeToken n, A argu) { return (R)n.tokenImage; }

    //
    // User-generated visitor methods below
    //

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public R visit(Goal n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> PrintStatement()
     * f15 -> "}"
     * f16 -> "}"
     */
    public R visit(MainClass n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        n.f10.accept(this, argu);
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        n.f13.accept(this, argu);
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);
        n.f16.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public R visit(TypeDeclaration n, A argu) {
        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public R visit(ClassDeclaration n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public R visit(ClassExtendsDeclaration n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public R visit(VarDeclaration n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public R visit(MethodDeclaration n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        n.f10.accept(this, argu);
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> ( FormalParameterRest() )*
     */
    public R visit(FormalParameterList n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public R visit(FormalParameter n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public R visit(FormalParameterRest n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public R visit(Type n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public R visit(ArrayType n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "boolean"
     */
    public R visit(BooleanType n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "int"
     */
    public R visit(IntegerType n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public R visit(Statement n, A argu) {
        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public R visit(Block n, A argu) {
        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public R visit(AssignmentStatement n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public R visit(ArrayAssignmentStatement n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> IfthenElseStatement()
     *       | IfthenStatement()
     */
    public R visit(IfStatement n, A argu) {
        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public R visit(IfthenStatement n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public R visit(IfthenElseStatement n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public R visit(WhileStatement n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public R visit(PrintStatement n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> OrExpression()
     *       | AndExpression()
     *       | CompareExpression()
     *       | neqExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | DivExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    public R visit(Expression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    public R visit(AndExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "||"
     * f2 -> PrimaryExpression()
     */
    public R visit(OrExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<="
     * f2 -> PrimaryExpression()
     */
    public R visit(CompareExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "!="
     * f2 -> PrimaryExpression()
     */
    public R visit(neqExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public R visit(PlusExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public R visit(MinusExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public R visit(TimesExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "/"
     * f2 -> PrimaryExpression()
     */
    public R visit(DivExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public R visit(ArrayLookup n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public R visit(ArrayLength n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public R visit(MessageSend n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Expression()
     * f1 -> ( ExpressionRest() )*
     */
    public R visit(ExpressionList n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public R visit(ExpressionRest n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    public R visit(PrimaryExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public R visit(IntegerLiteral n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "true"
     */
    public R visit(TrueLiteral n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "false"
     */
    public R visit(FalseLiteral n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public R visit(Identifier n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "this"
     */
    public R visit(ThisExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public R visit(ArrayAllocationExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public R visit(AllocationExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "!"
     * f1 -> Expression()
     */
    public R visit(NotExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public R visit(BracketExpression n, A argu) {
        /* YOUR CODE HERE */

        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Identifier()
     * f1 -> ( IdentifierRest() )*
     */
    public R visit(IdentifierList n, A argu) {
        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ","
     * f1 -> Identifier()
     */
    public R visit(IdentifierRest n, A argu) {
        R _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }
    
}
