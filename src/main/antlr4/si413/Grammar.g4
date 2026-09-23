parser grammar Grammar;
// grammar for basic calculator language

tokens {PRINT, SAVE, X, LP, RP, ADDOP, MULOP, INT}

prog
  : stmt prog  #RegularProg
  | EOF        #EmptyProg
  ;

stmt
  : PRINT LP expr RP  #PrintStmt
  | SAVE LP expr RP   #SaveStmt
  ;

expr
  : INT              #LiteralExpr
  | X                #VarExpr
  | ADDOP expr       #SignExpr
  | expr MULOP expr  #MulExpr
  | expr ADDOP expr  #AddExpr
  ;
