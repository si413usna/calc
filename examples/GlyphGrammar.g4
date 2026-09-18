parser grammar GlyphGrammar;

tokens { BIND, SHOW, NUM, GLYPH }

program: assignments print ;

assignments
  : assignments assign
  |
  ;

assign: GLYPH value BIND ;

print: SHOW value ;

value
  : NUM
  | GLYPH
  | value value
  ;
