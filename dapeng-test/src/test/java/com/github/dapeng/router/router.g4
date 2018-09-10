grammar router;

routes: (route)* EOF;

route: left '=>' right;

left:  'otherwise'
     | matcher (';' matcher)*;

matcher: ID 'match' patterns;

patterns: pattern (',' pattern)*;

pattern:  '~' pattern
        | string
        | regexpString
        | rangeString
        | NUMBER
        | ip
        | mod;

right: rightPattern (',' rightPattern)*;

rightPattern:  '~' rightPattern
             | ip;

regexpString: 'r' QUOT (ID | NUMBER | '.*')+ QUOT;
ip: 'ip' QUOT (NUMBER '.')+ NUMBER ('/' NUMBER)? QUOT;
mod: '%' QUOT NUMBER 'n+' (NUMBER|rangeString) QUOT;
rangeString: NUMBER '..' NUMBER ;
string: QUOT (ID | NUMBER) QUOT;

QUOT: '"';
ID : [a-z,A-Z,'_']+ ;
NUMBER : [0-9]+;
WS: [ \t\r\n]+ -> skip;

