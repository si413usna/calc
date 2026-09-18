parser grammar GogoGrammar;

tokens { STRLIT, COMMA, REV, LP, RP, GOGO, ASK }

prog: statements EOF ;

statements
  : print
  | print statements
  ;

print: GOGO expression ;

expression
  : expression COMMA item
  | item
  ;

item
  : STRLIT
  | ASK
  | REV item
  | LP expression RP
  ;
