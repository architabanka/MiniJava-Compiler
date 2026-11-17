%{
#include<iostream>
#include<map>
#include<string>
#include<vector>
#include<sstream>
#include<cstring>
#include<stdlib.h>
using namespace std;

int yylex(void);
void yyerror (const char *);
//extern int yylineno;
//extern char *yytext;
struct Macro{
    int arg_num;
    vector<string>args;
    string body;
};
map<string,Macro>macros;

vector<string>parse_list(const char* args){
    vector<string>res;
    if(!args||strlen(args)==0)return res;
    string input(args);
    string current="";
    int pc=0;
    
    for(int i=0;i<input.length();i++){
        char c=input[i];
        if(c=='(')pc++;
        else if(c == ')')pc--;
        if(c==','&&pc==0){
            size_t start=current.find_first_not_of(" \t\n\r");
            size_t end=current.find_last_not_of(" \t\n\r");
            if(start!=string::npos){
                res.push_back(current.substr(start,(int)end-start+1));
            }
            current=""; 
        }else{
            current+=c;
        }
    }
    size_t start=current.find_first_not_of(" \t\n\r");
    size_t end=current.find_last_not_of(" \t\n\r");
    if(start!=string::npos){
        res.push_back(current.substr(start,(int)end-start+1));
    }
    return res;
}
string replace_str(string body, string target, string present){
    size_t start_pos=0;
    while((start_pos=body.find(present,start_pos))!=std::string::npos){
        body.replace(start_pos,present.length(),target);
        start_pos+=target.length(); 
    }
    return body;
}

char* concat2(const char* s1, const char* s2){
    const char* p1= ((s1==NULL)? "": s1);
    const char* p2= ((s2==NULL)? "": s2);
    char* res = (char*)malloc(strlen(p1)+strlen(p2)+1);
    if (res == NULL) {
        return NULL;
    }
    strcpy(res, p1);
    strcat(res, p2);
    return res;
}
char* concat3(const char* s1, const char* s2, const char* s3){
    char* temp=concat2(s1,s2);
    char* res= concat2(temp, s3);
    free(temp);
    return res;
}
char* concat4(const char* s1, const char* s2, const char* s3, const char* s4){
    char* temp=concat2(s1,s2);
    char* res=concat3(temp,s3,s4);
    free(temp);
    return res;
}
char* expand_macro(char* name, char* args){
    string macro_name=name;

    if(macros.find(macro_name)==macros.end()){
        yyerror("Macro not found");
        return strdup("");
    }
    
    vector<string>arg=parse_list(args);
    if(arg.size()!=macros[macro_name].args.size()){
        yyerror("Macro argument count mismatch");
        return strdup("");
    }
    string body=macros[macro_name].body;
    string res="";
    int n=body.length();
    
    for(int i=0;i<n;){
        if(isalpha(body[i])||body[i]=='_'){
            string ident="";
            while(i<n&&(isalnum(body[i])||body[i] == '_')){
                ident+=body[i];
                i++;
            }
            bool replaced=false;
            for(int j=0;j<macros[macro_name].args.size();j++){
                if(ident==macros[macro_name].args[j]){
                    res+=arg[j];
                    replaced=true;
                    break;
                }
            }
            if(!replaced){
                res+=ident;
            }
        }else{
            res+=body[i];
            i++;
        }
    }
    return strdup(res.c_str());
}
%}

%union {
    char* str;
}

%token <str> DEFINE_EXPR DEFINE_EXPR0 DEFINE_EXPR1 DEFINE_EXPR2
%token <str> DEFINE_STMT DEFINE_STMT0 DEFINE_STMT1 DEFINE_STMT2
%token <str> INT BOOLEAN STRING
%token <str> CLASS IF ELSE WHILE PUBLIC PRINT STATIC VOID MAIN EXTENDS RETURN LENGTH TRUE FALSE THIS NEW 
%token <str> AND OR NEQ LEQ ADD MINUS MUL DIV NOT
%token <str> LPAREN RPAREN LBRACE RBRACE LBRACKET RBRACKET SEMICOLON COMMA DOT EQUALS
%token <str> IDENTIFIER INTEGER_LITERAL

%nonassoc LOWER_THAN_ELSE
%nonassoc ELSE
%left OR
%left AND
%left NEQ
%left LEQ
%left ADD MINUS
%left MUL DIV
%right NOT
%left DOT LBRACKET

%type <str> Goal TypeDeclarationList MacroDefinitionList MacroDefinition MacroDefStatement MacroDefExpression
%type <str> IdentifierListE MainClass MethodDeclarationList TypeDeclaration IdentifierListV TypeIdentifierListE TypeIdentifierList MethodDeclaration Type
%type <str> StatementList Statement ExpressionListE ExpressionList Expression PrimaryExpression 

%%  
Goal : 
    MacroDefinitionList MainClass TypeDeclarationList
    {
        printf("%s\n%s",$2,$3);
        $$=concat2($2,$3);
    }
    ;

TypeDeclarationList :
    /* empty */ {$$=strdup("");}
    | TypeDeclarationList TypeDeclaration {$$=concat2($1,$2);}
    ;

MacroDefinitionList :
    /* empty */ {
        $$=strdup("");
    }
    | MacroDefinitionList MacroDefinition {
        $$=concat2($1,$2);
    }
    ;

MacroDefinition :
    MacroDefExpression {
        $$=strdup($1);
    }
    | MacroDefStatement {
        $$=strdup($1);
    };

MacroDefStatement : 
    DEFINE_STMT IDENTIFIER LPAREN IDENTIFIER COMMA IDENTIFIER COMMA IDENTIFIER IdentifierListE RPAREN LBRACE StatementList RBRACE {
        Macro m;
        m.args.push_back($4);
        m.args.push_back($6);
        m.args.push_back($8);
        vector<string>args_more=parse_list($9);
        m.args.insert(m.args.end(),args_more.begin(),args_more.end());

        m.body=string("{") +$12+ string("}\n");
        macros[$2]=m;
        $$=strdup("");
    }
    | DEFINE_STMT0 IDENTIFIER LPAREN RPAREN LBRACE StatementList RBRACE {
        Macro m;
        m.body=string("{")+$6+string("}\n");
        macros[$2]=m;
        $$=strdup("");
    }
    | DEFINE_STMT1 IDENTIFIER LPAREN IDENTIFIER RPAREN LBRACE StatementList RBRACE {
        Macro m;
        m.args.push_back($4);
        m.body=string("{")+$7+string("}\n");
        macros[$2]=m;
        $$=strdup("");
    }
    | DEFINE_STMT2 IDENTIFIER LPAREN IDENTIFIER COMMA IDENTIFIER RPAREN LBRACE StatementList RBRACE {
        Macro m;
        m.args.push_back($4);
        m.args.push_back($6);
        m.body=string("{")+$9+string("}\n");
        macros[$2]=m;
        $$=strdup("");
    }
    ;

MacroDefExpression : 
    DEFINE_EXPR IDENTIFIER LPAREN IDENTIFIER COMMA IDENTIFIER COMMA IDENTIFIER IdentifierListE RPAREN LPAREN Expression RPAREN {
        Macro m;
        m.args.push_back($4);
        m.args.push_back($6);
        m.args.push_back($8);

        vector<string>args_more=parse_list($9);
        m.args.insert(m.args.end(),args_more.begin(),args_more.end());
        m.body=string("(")+$12+string(")");
        macros[$2]=m;
        $$=strdup("");
    }
    | DEFINE_EXPR0 IDENTIFIER LPAREN RPAREN LPAREN Expression RPAREN {
        Macro m;
        m.body=string("(")+$6+string(")");
        macros[$2]=m;
        $$=strdup("");
    } 
    | DEFINE_EXPR1 IDENTIFIER LPAREN IDENTIFIER RPAREN LPAREN Expression RPAREN {
        Macro m;
        m.args.push_back($4);
        m.body=string("(")+$7+string(")");
        macros[$2]=m;
        $$=strdup("");
    }
    | DEFINE_EXPR2 IDENTIFIER LPAREN IDENTIFIER COMMA IDENTIFIER RPAREN LPAREN Expression RPAREN {
        Macro m;
        m.args.push_back($4);
        m.args.push_back($6);
        m.body=string("(")+$9+string(")");
        macros[$2]=m;
        $$=strdup("");
    }
    ;

IdentifierListE :
    /* empty */ {$$=strdup("");}
    | IdentifierListE COMMA IDENTIFIER
    {
       $$=concat3($1, ", ",$3);
    }
    ;

MainClass :
    CLASS IDENTIFIER LBRACE PUBLIC STATIC VOID MAIN LPAREN STRING LBRACKET RBRACKET IDENTIFIER RPAREN LBRACE PRINT LPAREN Expression RPAREN SEMICOLON RBRACE RBRACE
    {
        char* t1=concat4("class ", $2, " {\n  public static void main(String[] ", $12);
        char* t2=concat4(t1, ") {\n    System.out.println(", $17, ");\n  }\n}\n");
        $$=t2;
        free(t1);
    }
    ;

MethodDeclarationList :
    /* empty */ {$$=strdup(""); }
    | MethodDeclarationList MethodDeclaration {
        $$=concat3($1," ",$2);
    }
    ;

TypeDeclaration :
    CLASS IDENTIFIER LBRACE IdentifierListV MethodDeclarationList RBRACE {
        char* temp=concat4("class ",$2,"{",$4);
        $$=concat3(temp,$5,"}");
        free(temp);
    }
    | CLASS IDENTIFIER EXTENDS IDENTIFIER LBRACE IdentifierListV MethodDeclarationList RBRACE {
        char* temp=concat4("class ",$2," extends ",$4);
        char* temp1=concat4(temp," {",$6,$7);
        $$=concat2(temp1,"}");
        free(temp);
        free(temp1);
    }
    ;

IdentifierListV :
    /* empty */ { $$=strdup("");}
    | IdentifierListV Type IDENTIFIER SEMICOLON
    {
        char* temp=concat4($1," ",$2," ");
        $$=concat3(temp,$3,";");
        free(temp);
    }
    ;

TypeIdentifierListE :
    /* empty */ {
        $$=strdup("");
    }
    | Type IDENTIFIER TypeIdentifierList
    {
        char* temp=concat4($1," ",$2," ");
        $$=concat2(temp, $3);
        free(temp);
    }
    ;

TypeIdentifierList : 
    /* empty */{
        $$=strdup("");
    }
    | TypeIdentifierList COMMA Type IDENTIFIER {
        char* temp=concat4($1, ", ", $3, " ");
        $$=concat2(temp, $4);
        free(temp);
    }
    ;

MethodDeclaration : 
    PUBLIC Type IDENTIFIER LPAREN TypeIdentifierListE RPAREN LBRACE IdentifierListV StatementList RETURN Expression SEMICOLON RBRACE
    {
        char* temp=concat4("public ", $2, " ", $3);
        char* temp1=concat4(temp, "(", $5, "){\n");
        temp=concat4(temp1, $8, "\n", $9);
        $$=concat4(temp, "\nreturn ", $11, ";}\n");
        free(temp);
        free(temp1);
    }   
    ;

Type :
    INT LBRACKET RBRACKET {$$=strdup("int[]");}
    | BOOLEAN {$$=strdup("boolean");}
    | INT {$$=strdup("int");}
    | IDENTIFIER {$$=strdup($1);}
    ;

StatementList :
    /* empty */ {$$=strdup("");}
    | Statement StatementList {$$=concat3($1,"\n",$2);}
    ;

Statement :
    LBRACE StatementList RBRACE {$$=concat3("{",$2,"}\n");}
    | PRINT LPAREN Expression RPAREN SEMICOLON {$$=concat3("System.out.println(",$3,");\n");}
    | IDENTIFIER EQUALS Expression SEMICOLON {$$=concat4($1, " = ",$3,";\n");}
    | IDENTIFIER LBRACKET Expression RBRACKET EQUALS Expression SEMICOLON 
    {
        char* temp=concat4($1, "[",$3,"] = ");
        $$=concat3(temp,$6,";\n");
        free(temp);
    }
    | IF LPAREN Expression RPAREN Statement %prec LOWER_THAN_ELSE {$$=concat4("if (",$3,")", $5);}
    | IF LPAREN Expression RPAREN Statement ELSE Statement
    {
        char* temp=concat3("if (",$3,")");
        $$=concat4(temp,$5,"else ",$7);
        free(temp);
    }
    | WHILE LPAREN Expression RPAREN Statement {$$=concat4("while (",$3,")", $5);}
    | IDENTIFIER LPAREN ExpressionListE RPAREN SEMICOLON {
        $$=expand_macro($1, $3);    
    }
    ;

ExpressionListE :
    /* empty */ { $$=strdup("");}
    | ExpressionList { $$=strdup($1);}
    ;

ExpressionList :
    Expression {$$=strdup($1);}
    | ExpressionList COMMA Expression {$$=concat3($1,", ",$3);}
    ;

Expression : 
    PrimaryExpression AND PrimaryExpression {$$=concat3($1, " && ",$3);}
    | PrimaryExpression OR PrimaryExpression {$$=concat3($1, " || ",$3);}
    | PrimaryExpression NEQ PrimaryExpression {$$=concat3($1, " != ",$3);}
    | PrimaryExpression LEQ PrimaryExpression {$$=concat3($1, " <= ",$3);}
    | PrimaryExpression ADD PrimaryExpression {$$=concat3($1, " + ",$3);}
    | PrimaryExpression MINUS PrimaryExpression {$$=concat3($1, " - ",$3);}
    | PrimaryExpression MUL PrimaryExpression {$$=concat3($1, " * ",$3);}
    | PrimaryExpression DIV PrimaryExpression {$$=concat3($1, " / ",$3);}
    | PrimaryExpression LBRACKET PrimaryExpression RBRACKET {$$=concat4($1,"[",$3,"]");}
    | PrimaryExpression DOT LENGTH {$$=concat2($1, ".length");}
    | PrimaryExpression DOT IDENTIFIER LPAREN ExpressionListE RPAREN
    {
        char* temp=concat3(".",$3,"(");
        $$=concat4($1,temp,$5,")");
        free(temp);
    }
    | IDENTIFIER LPAREN ExpressionListE RPAREN 
    {
        $$=expand_macro($1,$3);
    }
    | PrimaryExpression {$$=strdup($1);}
    ;
 
PrimaryExpression :
    INTEGER_LITERAL{$$=strdup($1);}
    | TRUE {$$=strdup("true");}
    | FALSE {$$=strdup("false");}
    | IDENTIFIER {$$=strdup($1);}
    | THIS {$$=strdup("this");}
    | NEW INT LBRACKET Expression RBRACKET
    {
        $$=concat3("new int[",$4,"]");
    }
    | NEW IDENTIFIER LPAREN RPAREN
    {   
        $$=concat3("new ",$2,"()");
    }
    | NOT Expression 
    { $$=concat2("!", $2);}
    | LPAREN Expression RPAREN
    { $$=concat3("(", $2, ")");}
    ;
%%

void yyerror (const char *s) {
/*
 fprintf(stderr,
            "Parse error at line %d: %s, token = '%s'\n",
            yylineno, s, yytext);
*/
  printf("//Failed to parse input code");
  exit(0);
}

int main () {
    yyparse ();
    return 0;
}