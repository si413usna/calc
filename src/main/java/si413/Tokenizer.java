package si413;

/* This file contains a generic tokenizer based on the tokenSpec.txt file.
 * You should NOT need to change anything here - just change the
 * tokenSpec.txt file for your language as needed.
 * This is version 2.0 of the tokenizer. It reads in the entire input into
 * a single string for matching, rather than one character at a time.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Pair;

/** Generic Tokenizer (aka Scanner or Lexer) based on Java regex library.
 * Reads token specifications from a plain text file, by default in
 * main/resources/si413/tokenSpec.txt
 *
 * That file make have comment lines starting with #, or token spec
 * lines of the form
 *   TOKENNAME: regex
 *
 * The regular expressions cannot start or end with whitespace - use
 * character classes like [ ] if you want to start or end your regex
 * with a space.
 *
 * Designed to be compatible with ANTLRv4 parsers. Use the streamFrom()
 * method to get a TokenStream which can be passed to a parser.
 * Be sure to use a tokens { NAME, NAME, ... } block in your parser grammar
 * to match with the names of tokens in tokenSpec.txt.
 */
public class Tokenizer {
    /** Token type used internally to indicate a regex should be skipped. */
    public static final int IGNORE_TYPE = Integer.MAX_VALUE;

    /** The internal representation of a single token spec, which has a type and a regex. */
    private static record TokenSpec(int type, Pattern pat) { }

    /** Regex used to parse each line of the tokenSpec.txt file.
     * Distinguishes between blank or comment lines, and valid token spec lines.
     */
    private static Pattern specFileLine = Pattern.compile(
        "^\\s*(?:#.*|(?:(?<name>[A-Z]\\w*)|ignore)\\s*:\\s*(?<pat>.*?)\\s*)$");

    private static class NamedTokens {
        private Map<String, Integer> name2type = new HashMap<>();
        private Map<Integer, String> type2name = new HashMap<>();
        private int highestType = 0;

        public NamedTokens() {
            add("EOF", Token.EOF);
        }

        public void add(String name, int type) {
            if (name2type.containsKey(name) || type2name.containsKey(type)) {
                throw new IllegalArgumentException(String.format(
                            "token name '%s' or type %d duplicated", name, type));
            }
            name2type.put(name, type);
            type2name.put(type, name);
            if (type > highestType) highestType = type;
        }

        public int size() {
            return name2type.size();
        }

        public int lookup(String name, boolean addIfMissing) {
            Integer type = name2type.get(name);
            if (type == null) {
                if (addIfMissing) {
                    type = highestType + 1;
                    add(name, type);
                }
                else return Errors.error(String.format("Token %s not found in vocab", name));
            }
            return type;
        }

        public int lookup(String name) {
            return lookup(name, false);
        }

        public String lookup(int type) {
            String name = type2name.get(type);
            if (name == null)
                return Errors.error(String.format("No token with type %d", type));
            else return name;
        }
    }

    private List<TokenSpec> specs;
    private Set<Integer> initialDisabledTokens;
    private NamedTokens tokNames = new NamedTokens();

    /** Loads a tokenizer from the given input spec file and vocabulary of token types.
     * No tokens will be disabled by default.
     */
    public Tokenizer(InputStream specFile, Vocabulary vocab) throws IOException {
        this(specFile, vocab, Set.of());
    }

    /** Loads a tokenizer from a spec file and token type vocabular, with initially disabled tokens. */
    public Tokenizer(InputStream specFile,
                     Vocabulary vocab,
                     Set<Integer> initialDisabledTokens)
            throws IOException
    {
        this.initialDisabledTokens = initialDisabledTokens;

        // extract token types from parser vocabulary
        boolean hasVocab = (vocab != null);
        if (hasVocab) {
            for (int type = 0; type <= vocab.getMaxTokenType(); ++type) {
                String name = vocab.getSymbolicName(type);
                if (name != null) tokNames.add(name, type);
            }
        }

        // read specs from spec file
        specs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(specFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher mat = specFileLine.matcher(line);
                if (!mat.matches()) {
                    Errors.error(String.format("Improper token spec line: '%s'", line));
                }
                String pat = mat.group("pat");
                if (pat == null) continue;
                String tokName = mat.group("name");
                int tokType;
                if (tokName == null) {
                    tokType = IGNORE_TYPE;
                } else {
                    tokType = tokNames.lookup(tokName, !hasVocab);
                }
                specs.add(new TokenSpec(tokType, Pattern.compile(pat, Pattern.MULTILINE)));
            }
        }
    }

    /** Returns the name associated to the given token type. */
    public String getTokName(int type) {
        return tokNames.lookup(type);
    }

    /** Produces a stream of tokens from the given input file. */
    public TokenStream streamFrom(Path sourceFile) throws IOException {
        return new CommonTokenStream(new Tokens(sourceFile));
    }

    /** TokenSource implementation to get tokens from a source file.
     * The most important method is nextToken().
     */
    public class Tokens implements TokenSource {
        private static record TokMatch(int tokType, Matcher matcher) { }

        private String text;
        private Pair<TokenSource,CharStream> sourcePair;
        private Set<Integer> disabledTokens;
        private List<TokMatch> matchers;
        private int pos = 0;
        private int line = 1;
        private int col = 1;
        private boolean hitEOF = false;
        private TokenFactory<?> tokenFactory = CommonTokenFactory.DEFAULT;

        public Tokens(Path sourceFile) throws IOException{
            this(Files.readString(sourceFile), sourceFile.toString());
        }

        public Tokens(String sourceText, String sourceName) {
            this.text = sourceText;
            this.sourcePair = new Pair<>(this, CharStreams.fromString(text, sourceName));
            this.disabledTokens = new HashSet<>(initialDisabledTokens);
            this.matchers = specs.stream()
                .map(tspec -> new TokMatch(tspec.type, tspec.pat.matcher(text)))
                .collect(Collectors.toUnmodifiableList());
        }

        @Override
        public Token nextToken() {
            // check for end of file
            if (pos == text.length()) {
                return tokenFactory.create(
                    sourcePair,
                    Token.EOF,
                    "",
                    Token.DEFAULT_CHANNEL,
                    0,
                    0,
                    line,
                    col
                );
            }

            // find the longest match starting at pos - maximal munch!
            int bestEnd = -1;
            int bestType = -1;
            for (TokMatch tm : matchers) {
                if (!disabledTokens.contains(tm.tokType)
                    && tm.matcher.find(pos)
                    && tm.matcher.start() == pos
                    && tm.matcher.end() > bestEnd
                ) {
                    bestEnd = tm.matcher.end();
                    bestType = tm.tokType;
                }
            }

            if (bestEnd <= pos) {
                // no (non-empty) match found
                Errors.syntax(
                    "Tokenizer",
                    getSourceName(),
                    line,
                    col,
                    String.format("invalid token starting with '%s'",
                        text.substring(pos, Math.min(text.length(), pos+20)))
                );
            }

            // create the actual token that will be returned
            Token result = tokenFactory.create(
                sourcePair,
                bestType,
                text.substring(pos, bestEnd),
                (bestType == IGNORE_TYPE ? Token.HIDDEN_CHANNEL : Token.DEFAULT_CHANNEL),
                0,
                bestEnd-pos-1,
                line,
                col
            );

            // advance the position and keep track of line numbers
            while (pos < bestEnd) {
                if (text.charAt(pos) == '\n') {
                    ++line;
                    col = 1;
                }
                else ++col;
                ++pos;
            }

            return result;
        }

        public void disableToken(int type) {
            disabledTokens.add(type);
        }

        public void enableToken(int type) {
            disabledTokens.remove(type);
        }

        @Override
        public String getSourceName() {
            return sourcePair.b.getSourceName();
        }

        @Override
        public CharStream getInputStream() {
            return sourcePair.b;
        }

        @Override
        public int getLine() {
            return line;
        }

        @Override
        public int getCharPositionInLine() {
            return col;
        }

        @Override
        public TokenFactory<?> getTokenFactory() {
            return this.tokenFactory;
        }

        @Override
        public void setTokenFactory(TokenFactory<?> factory) {
            this.tokenFactory = factory;
        }
    }
}
