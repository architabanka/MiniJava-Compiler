%option noyywrap yylineno
%{
#include "A1.tab.h"
#include <string.h>
#include <stdlib.h>
#include<stdio.h>

int token(int token_id){
    yylval.str=strdup(yytext);
    return token_id;
}
%}

%%

[ \t\n\r]+ {/* Ignore whitespace */}
"//".* {/* Ignore comments */}
"/*"([^*]|\*+[^*/])*\*+"/" { /* Ignore block comments */ } ////// seee

"#defineExpr0" {return token(DEFINE_EXPR0);}
"#defineExpr1" {return token(DEFINE_EXPR1);}
"#defineExpr2" {return token(DEFINE_EXPR2);}
"#defineExpr" {return token(DEFINE_EXPR);}
"#defineStmt0" {return token(DEFINE_STMT0);}
"#defineStmt1" {return token(DEFINE_STMT1);}
"#defineStmt2" {return token(DEFINE_STMT2);}
"#defineStmt" {return token(DEFINE_STMT);}

"int" {return token(INT);}
"boolean" {return token(BOOLEAN);}
"class" {return token(CLASS);}
"if" {return token(IF);}
"else" {return token(ELSE);}
"while" {return token(WHILE);}
"public" {return token(PUBLIC);}
"System.out.println" {return token(PRINT);}
"String" {return token(STRING);}
"static" {return token(STATIC);}
"void" {return token(VOID);}
"main" {return token(MAIN);}
"extends" {return token(EXTENDS);}
"return" {return token(RETURN);}
"length" {return token(LENGTH);}
"true" {return token(TRUE);}
"false" {return token(FALSE);}
"this" {return token(THIS);}
"new" {return token(NEW);}  

"&&" {return token(AND);}
"||" {return token(OR);}
"!=" {return token(NEQ);}
"<=" {return token(LEQ);}
"+" {return token(ADD);}
"-" {return token(MINUS);}
"*" {return token(MUL);}
"/" {return token(DIV);}
"!" {return token(NOT);}

"(" {return token(LPAREN);}
")" {return token(RPAREN);}
"{" {return token(LBRACE);}
"}" {return token(RBRACE);}
"[" {return token(LBRACKET);}
"]" {return token(RBRACKET);}
";" {return token(SEMICOLON);}
"," {return token(COMMA);}
"." {return token(DOT);}
"=" {return token(EQUALS);}

[a-zA-Z_][a-zA-Z0-9_]* { return token(IDENTIFIER);}
[0-9]+ {return token(INTEGER_LITERAL);}

. {return token(yytext[0]);}

%%
