package si413;

import java.util.function.Consumer;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;

/** Contains static methods for error handling.
 * Be sure to call Errors.error() from your code whenever something goes
 * wrong, rather than letting an exception propagate or just crashing.
 */
public class Errors {
    /** Call this method when an error is noticed by your interpreter or compiler.
     * The default behavior is to print the message to standard error
     * and then abort the program with exit code 7.
     * This function will never actually return; the return type T is just
     * for programmer convenience if you want to call an error from a function
     * that is supposed to return something.
     */
    public static <T> T error(String message) {
        errorAction.accept(message);
        throw new AssertionError("error action should have thrown or aborted execution");
    }

    /** Call this from the scanner or parser to indicate a synax error. */
    public static <T> T syntax(String parent, String source, int line, int col, String msg) {
        return error(String.format("SYNTAX ERROR in %s from line %d column %d of %s: %s",
                                   parent, line, col, source, msg));
    }

    /** Static method to make an ANTLR lexer or parser abort on failure.
     * By default ANTLR will continue trying to parse even after a syntax
     * error is found, but we want it to crash immediately.
     */
    public static void register(Recognizer<?,?> parser) {
        parser.removeErrorListeners();
        parser.addErrorListener(new Listener());
    }

    /** This is what calling error() does by default. */
    private static void defaultErrorAction(String message) {
        System.err.format("ERROR: %s\n", message);
        System.err.println("Aborting the interpreter");
        System.exit(7);
    }

    /** Storing the function that will be actually called for error(). */
    private static Consumer<String> errorAction = Errors::defaultErrorAction;

    /** Changes what happens when someone calls error().
     * This is mostly useful for running test cases, so the JVM doesn't
     * crash when we are testing for errors.
     */
    public static void setErrorAction(Consumer<String> action) {
        assert action != null;
        errorAction = action;
    }

    /** ANTLR Error listener class to intercept syntax errors during parsing. */
    private static class Listener extends BaseErrorListener {
        @Override
        public void syntaxError(
                Recognizer<?,?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e)
        {
            Errors.syntax(
                recognizer.getClass().getSimpleName(),
                recognizer.getInputStream().getSourceName(),
                line,
                charPositionInLine,
                msg);
        }
    }
}
