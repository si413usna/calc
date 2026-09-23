# calc: Interpreter for simple calculator language

## Prerequisites

You will a JDK (Java version 21 or later), and need Apache Maven.
Try `javac -version` and `mvn --version` to check what you have.

To install on Debian/Ubuntu you can do

    sudo apt install default-jdk maven


## Easy way to compile and run

Just run

    ./run.sh

This runs `mvn compile`, then grabs the correct classpath from maven,
then runs the main method from `Main.java` with that classpath.

For example, to run the test program, you would do

    ./run.sh calcprog.txt

If you just want to see the token stream or parse tree, put the word
`token` or `parse` as an extra command-line argument before the source
file name, like

    ./run.sh parse calcprog.txt


## Manual build and run

To compile everything from scratch, do

    mvn clean package

which creates a runnable jar file under `target/calc-1.0.jar`

Then you can run that jar directly, like

    java -jar target/calc-1.0.jar calcprog.txt


## Code layout

*   Token specifications (regexes):
    [src/main/resources/si413/tokenSpec.txt](src/main/resources/si413/tokenSpec.txt)

*   Grammar spec (for ANTLR):
    [src/main/antlr4/si413/Grammar.g4](src/main/antlr4/si413/Grammar.g4)

*   Actual code in [src/main/java/si413](src/main/java/si413):

    *   [Interpreter.java](src/main/java/si413/Interpreter.java):
        Contains "visitor" classes to execute each node in the parse
        tree. **This is where the main logic (semantics) happens!**

    *   [Main.java](src/main/java/si413/Main.java):
        Processes command-line args, then calls the Tokenizer, the
        ANTLR-generated parser, and finally the `Interpreter`.

    *   [Tokenizer.java](src/main/java/si413/Tokenizer.java):
        Scans a source file and outputs a token stream according to the
        `tokenSpec.txt`, using the maximal munch rule.

    *   [Errors.java](src/main/java/si413/Errors.java):
        Convenience class for returning exceptions and raising runtime errors.

    *   [SyntaxViz.java](src/main/java/si413/SyntaxViz.java):
        Methods to display a token stream or parse tree.
