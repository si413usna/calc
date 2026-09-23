package si413;

import org.antlr.v4.runtime.tree.ParseTree;

/** Interpreter for basic calculator language.
 * The tokens and grammar come from src/main/resource/si413/tokenSpec.txt
 * and src/main/antlr4/si413/Grammar.g4 respectively.
 */
public class Interpreter {
    private StatementVisitor stmtVis = new StatementVisitor();
    private ExpressionVisitor exprVis = new ExpressionVisitor();
    private Integer savedValue = null;

    /** Executes the complete parse tree (should have a statement at the root). */
    public void execute(ParseTree ptree) {
        stmtVis.visit(ptree);
    }

    /** Methods in this class will execute statements.
     * Return type is Void because statements do not return anything.
     * Note that this is Void and not void, so we still have to return null
     * in each function. (This is a consequence of Java generics.)
     */
    private class StatementVisitor extends Visitor<Void> {
        @Override
        public Void visitRegularProg(Grammar.RegularProgContext ctx) {
            visit(ctx.stmt());
            visit(ctx.prog());
            return null;
        }

        @Override
        public Void visitEmptyProg(Grammar.EmptyProgContext ctx) {
            return null;
        }

        @Override
        public Void visitPrintStmt(Grammar.PrintStmtContext ctx) {
            int value = exprVis.visit(ctx.expr());
            System.out.println(value);
            return null;
        }

        @Override
        public Void visitSaveStmt(Grammar.SaveStmtContext ctx) {
            savedValue = exprVis.visit(ctx.expr());
            return null;
        }

        @Override
        public Void visitEmptyStmt(Grammar.EmptyStmtContext ctx) {
            return null;
        }
    }

    /** Methods in this class will execute expressions and return the result.
     */
    private class ExpressionVisitor extends Visitor<Integer> {
        @Override
        public Integer visitLiteralExpr(Grammar.LiteralExprContext ctx) {
            return Integer.valueOf(ctx.INT().getText());
        }

        @Override
        public Integer visitVarExpr(Grammar.VarExprContext ctx) {
            if (savedValue == null)
                return Errors.error("x is referenced before being saved");
            else return savedValue;
        }

        @Override
        public Integer visitSignExpr(Grammar.SignExprContext ctx) {
            int rhs = visit(ctx.expr());
            if (ctx.ADDOP().getText().equals("-")) return -rhs;
            else return rhs;
        }

        @Override
        public Integer visitMulExpr(Grammar.MulExprContext ctx) {
            int lhs = visit(ctx.expr(0));
            int rhs = visit(ctx.expr(1));
            if (ctx.MULOP().getText().equals("*")) return lhs * rhs;
            else {
                if (rhs == 0) return Errors.error("divide by zero");
                else return lhs / rhs;
            }
        }

        @Override
        public Integer visitAddExpr(Grammar.AddExprContext ctx) {
            int lhs = visit(ctx.expr(0));
            int rhs = visit(ctx.expr(1));
            if (ctx.ADDOP().getText().equals("+")) return lhs + rhs;
            else return lhs - rhs;
        }

        @Override
        public Integer visitParenExpr(Grammar.ParenExprContext ctx) {
            return visit(ctx.expr());
        }
    }


    /** Use this as the base class any visitor classes.
     * It warns you if one of the visit methods is missing at parse-time.
     */
    private static class Visitor<T> extends GrammarBaseVisitor<T> {
        @Override
        public T visitChildren(org.antlr.v4.runtime.tree.RuleNode node) {
            return Errors.error(String.format(
                "class %s has no visit method for %s",
                getClass().getSimpleName(),
                node.getClass().getSimpleName()));
        }
    }

}
