package si413;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.lang.reflect.Method;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    /** Calls the Tokenizer to extract tokens from the source text file. */
    public static TokenStream tokenize(Path sourceFile) throws IOException {
        return new Tokenizer(
            Main.class.getResourceAsStream("tokenSpec.txt"),
            Grammar.VOCABULARY
        ).streamFrom(sourceFile);
    }

    /** Calls the ANTLR-generated parser to form the tokens into a parse tree. */
    public static ParseTree parse(TokenStream tokens) {
        Grammar parser = new Grammar(tokens);
        Errors.register(parser);
        try {
            return (ParseTree) Grammar.class.getMethod(Grammar.ruleNames[0]).invoke(parser);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("problem invoking start rule method from parser: " + e);
        }
    }

    private static void usage() {
        System.err.format(
            """
            usage: java -jar calc [command] sourceFile
            where command is one of
                tokens: show token stream
                parse: show parse tree
                interp: (default) run the interpreter
            """);
        System.exit(2);
    }

    private enum Command {
        TOKENS, PTREE, INTERP
    }

    public static void main(String[] args) throws IOException {
        Command command = Command.INTERP;
        String fname = null;

        if (args.length == 1) {
            if (args[0].startsWith("-")) usage();
            fname = args[0];
        }
        else if (args.length == 2) {
            switch (args[0].charAt(0)) {
                case 't':
                    command = Command.TOKENS;
                    break;
                case 'p':
                    command = Command.PTREE;
                    break;
                case 'i':
                    command = Command.INTERP;
                    break;
                default:
                    usage();
            }
            fname = args[1];
        }
        else usage();

        Tokenizer tokenizer = new Tokenizer(
            Main.class.getResourceAsStream("tokenSpec.txt"),
            Grammar.VOCABULARY);

        TokenStream tokens = null;
        try { tokens = tokenize(Path.of(fname)); }
        catch (NoSuchFileException e) {
            System.err.format("File not found: %s\n", fname);
            System.exit(2);
        }

        if (command == Command.TOKENS) {
            new SyntaxViz(tokenizer).showTokens(tokens);
            System.exit(0);
        }

        ParseTree ptree = parse(tokens);

        if (command == Command.PTREE) {
            new SyntaxViz(tokenizer).showParseTree(ptree);
            System.exit(0);
        }

        new Interpreter().execute(ptree);
        System.exit(0);
    }
}
