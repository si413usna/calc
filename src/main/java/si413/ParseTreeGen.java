package si413;

/*
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import javax.tools.*;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
*/

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaFileObject;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/** Parse tree generator.
 * The tokens and grammar come from src/main/resource/si413/tokenSpec.txt
 * and src/main/antlr4/si413/ParseRules.g4 respectively.
 */
public class ParseTreeGen {
    private Class<?> parserClass;
    private Tokenizer tokenizer;
    private String[] ruleNames;

    public ParseTreeGen(Class<?> parserClass, File specFile) throws ReflectiveOperationException, IOException {
        this.parserClass = parserClass;
        this.tokenizer = new Tokenizer(
            new java.io.FileInputStream(specFile),
            (Vocabulary) parserClass.getField("VOCABULARY").get(null)
        );
        this.ruleNames = (String[]) parserClass.getField("ruleNames").get(null);
    }

    public ParseTreeGen(File specFile) throws IOException {
        this.parserClass = null;
        this.tokenizer = new Tokenizer(new java.io.FileInputStream(specFile), null);
        this.ruleNames = null;
    }

    public TokenStream getStream(Path sourceFile) throws IOException {
        return tokenizer.streamFrom(sourceFile);
    }

    public void checkTokens(TokenStream source) throws IOException {
        source.seek(0);
        int mark = source.mark();
        while (true) {
            Token tok = source.LT(1);
            if (tok.getType() == -1) break;
            source.consume();
        }
        source.seek(0);
        source.release(mark);
        System.out.println("successful tokenization");
    }

    public void showTokens(TokenStream source) throws IOException {
        source.seek(0);
        int mark = source.mark();
        System.out.println("================================");
        System.out.println("         TOKEN STREAM");
        System.out.println("================================");
        System.out.println("line\tcolumn\ttype\tspelling");
        System.out.println("------- ------- ------- -------");
        while (true) {
            Token tok = source.LT(1);
            System.out.format("%d\t%d\t%s\t%s\n",
                tok.getLine(),
                tok.getCharPositionInLine(),
                tokenizer.getTokName(tok.getType()),
                tok.getText()
            );
            if (tok.getType() == -1) break;
            source.consume();
        }
        source.seek(0);
        source.release(mark);
    }

    private void printNode(ParseTree curNode, String prefix) {
        if (curNode instanceof TerminalNode tnode) {
            Token tok = tnode.getSymbol();
            System.out.format("%s '%s'\n",
                tokenizer.getTokName(tok.getType()),
                tok.getText());
        }
        else if (curNode instanceof ParserRuleContext inode) {
            String nonterminal = ruleNames[inode.getRuleIndex()];
            //String context = inode.getClass().getSimpleName();
            //System.out.format("%s %s\n", nonterminal, context);
            System.out.format("%s\n", nonterminal);
        }
        else { assert false; }
        int nchildren = curNode.getChildCount();
        for (int i = 0; i < nchildren; ++i) {
            boolean last = (i == nchildren - 1);
            System.out.print(prefix + (last ? "└── " : "├── "));
            printNode(curNode.getChild(i), prefix + (last ? "    " : "│   "));
        }
    }

    public void checkParseTree(TokenStream source) throws IOException, ReflectiveOperationException {
        source.seek(0);
        int mark = source.mark();
        Parser parser = (Parser) parserClass.getConstructor(TokenStream.class).newInstance(source);
        Errors.register(parser);
        String startRuleName = parser.getRuleNames()[0];
        ParseTree root = null;
        try {
            Method startRuleMethod = parser.getClass().getMethod(startRuleName);
            root = (ParseTree)startRuleMethod.invoke(parser);
        }
        catch (Exception e) { throw new RuntimeException(e); }
        source.seek(0);
        source.release(mark);
        System.out.println("successful parse");
    }

    public void showParseTree(TokenStream source) throws IOException, ReflectiveOperationException {
        source.seek(0);
        int mark = source.mark();
        System.out.println("================================");
        System.out.println("         PARSE TREE");
        System.out.println("================================");
        Parser parser = (Parser) parserClass.getConstructor(TokenStream.class).newInstance(source);
        Errors.register(parser);
        String startRuleName = parser.getRuleNames()[0];
        ParseTree root = null;
        try {
            Method startRuleMethod = parser.getClass().getMethod(startRuleName);
            root = (ParseTree)startRuleMethod.invoke(parser);
        }
        catch (Exception e) { throw new RuntimeException(e); }
        printNode(root, "");
        source.seek(0);
        source.release(mark);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Arguments: [-q] <tokenSpec.txt> [<Grammar.g4>] <sourcecode.txt>");
            System.exit(1);
        }

        int argind = 0;

        boolean quiet = false;
        if (args[argind].equals("-q")) {
            quiet = true;
            ++argind;
        }

        File specFile = new File(args[argind]);
        ++argind;

        File grammarFile;
        File inputFile;

        if (args.length == argind + 1) {
            grammarFile = null;
            inputFile = new File(args[argind]);
        }
        else {
            grammarFile = new File(args[argind]);
            inputFile = new File(args[argind+1]);
        }

        Path tempDir = Files.createTempDirectory("antlr_build_");
        tempDir.toFile().deleteOnExit();

        ParseTreeGen ptgen;
        if (grammarFile == null) {
            ptgen = new ParseTreeGen(specFile);
        }
        else {
            // 1. Programmatically run ANTLR
            String[] antlrArgs = new String[] {
                "-o", tempDir.toAbsolutePath().toString(),
                "-no-listener",
                "-no-visitor",
                grammarFile.getAbsolutePath()
            };
            org.antlr.v4.Tool antlr = new org.antlr.v4.Tool(antlrArgs);
            antlr.processGrammarsOnCommandLine();
            if (antlr.getNumErrors() > 0) {
                throw new RuntimeException("ANTLR compilation failed.");
            }

            // 2. Compile generated .java files via JavaCompiler API
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("JDK required at runtime (getSystemJavaCompiler returned null). Run with JDK, not JRE.");
            }

            List<File> javaFiles = Files.walk(tempDir)
                .filter(p -> p.toString().endsWith(".java"))
                .map(Path::toFile)
                .toList();

            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles);

            // Pass current classpath to the in-memory compiler
            List<String> optionList = List.of("-d", tempDir.toAbsolutePath().toString());

            boolean success = compiler.getTask(null, fileManager, null, optionList, null, compilationUnits).call();
            fileManager.close();

            if (!success) {
                throw new RuntimeException("javac compilation of generated parser failed.");
            }

            // 3. Dynamic Class Loading
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ tempDir.toUri().toURL() },
                ParseTreeGen.class.getClassLoader()
            );

            String grammarName = grammarFile.getName().replace(".g4", "");
            String parserClassName = grammarName;// + "Parser";

            // 1. Load compiled parser class
            Class<?> parserClass = Class.forName(parserClassName, true, classLoader);

            // create parse tree generator
            ptgen = new ParseTreeGen(parserClass, specFile);
        }

        TokenStream toks = ptgen.getStream(inputFile.toPath());
        if (quiet) ptgen.checkTokens(toks);
        else ptgen.showTokens(toks);
        if (grammarFile != null) {
            if (quiet) ptgen.checkParseTree(toks);
            else ptgen.showParseTree(toks);
        }
        if (!quiet) System.out.println("================================");
    }
}
