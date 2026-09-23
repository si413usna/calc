package si413;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/** Helpful methods to visualize a token stream or parse tree. */
public class SyntaxViz {
    private Tokenizer tokenizer;

    public SyntaxViz(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public void showTokens(TokenStream source) {
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
        System.out.println("================================");
    }

    private void printNode(ParseTree curNode, String prefix) {
        if (curNode instanceof TerminalNode tnode) {
            Token tok = tnode.getSymbol();
            System.out.format("%s '%s'\n",
                tokenizer.getTokName(tok.getType()),
                tok.getText());
        }
        else if (curNode instanceof ParserRuleContext inode) {
            String nonterminal = Grammar.ruleNames[inode.getRuleIndex()];
            String context = inode.getClass().getSimpleName();
            System.out.format("%s %s\n", nonterminal, context);
            //System.out.format("%s\n", nonterminal);
        }
        else { assert false; }
        int nchildren = curNode.getChildCount();
        for (int i = 0; i < nchildren; ++i) {
            boolean last = (i == nchildren - 1);
            System.out.print(prefix + (last ? "└── " : "├── "));
            printNode(curNode.getChild(i), prefix + (last ? "    " : "│   "));
        }
    }

    public void showParseTree(ParseTree ptree) {
        System.out.println("================================");
        System.out.println("         PARSE TREE");
        System.out.println("================================");
        printNode(ptree, "");
        System.out.println("================================");
    }
}
