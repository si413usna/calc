parser grammar Grammar;
// grammar for basic calculator language

tokens {PRINT, SAVE, X, LP, RP, ADDOP, MULOP, INT, NEWLINE}

prog
  : stmt prog  #RegularProg
  | EOF        #EmptyProg
  ;

stmt
  : PRINT expr NEWLINE  #PrintStmt
  | SAVE expr NEWLINE   #SaveStmt
  | NEWLINE             #EmptyStmt
  ;

expr
  : INT              #LiteralExpr
  | X                #VarExpr
  | ADDOP expr       #SignExpr
  | expr MULOP expr  #MulExpr
  | expr ADDOP expr  #AddExpr
  | LP expr RP       #ParenExpr
  ;
