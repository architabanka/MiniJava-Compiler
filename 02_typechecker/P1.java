import syntaxtree.*;
import visitor.*;
import java.util.*;

public class P1{
    public static void main(String[] args){
        try{
            Node root = new MiniJavaParser(System.in).Goal();
            GJDepthFirst.SymbolTable symbolTable=new GJDepthFirst.SymbolTable();
            //Pass 1:Build Symbol Table
            GJDepthFirst.SymbolTableVisitor stVisitor = new GJDepthFirst.SymbolTableVisitor(symbolTable);
            root.accept(stVisitor,null);
            
            stVisitor.validateSymbolTable(symbolTable);

            //Pass 2: Type Check
            GJDepthFirst.TypeCheckVisitor tcVisitor= new GJDepthFirst.TypeCheckVisitor(symbolTable);
            root.accept(tcVisitor,null);

            System.out.println("Program type checked successfully");
        }
        catch (GJDepthFirst.SymbolNotFoundException e){
            System.out.println("Symbol not found");
        }
        catch (GJDepthFirst.TypeException e){
            System.out.println("Type error");
        }
        catch (ParseException e){
            System.out.println(e.toString());
        }
        catch (Exception e){
            System.out.println("Type error");
        }
    }
}