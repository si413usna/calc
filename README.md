# ptree: Scanner and Parser Visualization

This program will create a Scanner (Tokenizer) and Parser based on
given token spec and grammar files.

## Getting the jar

Easiest: Download the released pre-built
[ptree.jar](https://github.com/si413usna/ptree/releases/latest/download/ptree.jar)
file.

Or: install Apache Maven and run

    mvn package

which will create a jar file in `target/ptree-VERSION.jar`.

## Usage

You will need JDK 21 or later installed.
(Check by running `javac -version`.)

To run the ptree, tool, do:

    java -jar ptree.jar [-q] <tokenSpec.txt> [<Grammar.g4>] <sourcecode.txt>

If the grammar file is omitted, then just the tokenization is shown.

## Token spec and grammar files

The scanner is built in Java based on Java regex syntax. The spec
is formatted like this:

    TOKNAME: regex
    ANOTHER: regex
    ignore: regex

where the all-caps `TOKNAME`s are token names, the `regex` are Java
regular expressions, and the special name `ignore` means occurrences of
that regex will be skipped by the tokenizer and omitted from the token
stream.

The parser spec is in the format of [ANTLR v4](https://github.com/antlr/antlr4/blob/4.6/doc/index.md),
and ANTLR is used to generate the parser on the fly.

The [examples folder](examples/) contains some sample scanner specs,
grammars, and small programs in a couple of languages so you can see how
these files are formatted.
