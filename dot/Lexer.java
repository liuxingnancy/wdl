import java.util.regex.*;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;

class SourceCode {
  private File source;
  private String resource;
  private String contents;
  private int line;
  private int col;

  SourceCode(String source, String resource) {
    this.contents = source;
    this.resource = resource;
    this.line = 1;
    this.col = 1;
  }

  SourceCode(File source) throws IOException {
    this(source, "utf-8", source.getCanonicalPath());
  }

  SourceCode(File source, String resource) throws IOException {
    this(source, "utf-8", resource);
  }

  SourceCode(File source, String encoding, String resource) throws IOException, FileNotFoundException {
    FileChannel channel = new FileInputStream(source).getChannel();
    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    Charset cs = Charset.forName(encoding);
    CharsetDecoder cd = cs.newDecoder();
    CharBuffer cb = cd.decode(buffer);
    this.contents = cb.toString();
    this.resource = resource;
    this.line = 1;
    this.col = 1;
  }

  public void advance(int amount) {
    String str = this.contents.substring(0, amount);
    for ( byte b : str.getBytes() ) {
      if ( b == (byte) '\n' || b == (byte) '\r' ) {
        this.line++;
        this.col = 1;
      } else {
        this.col++;
      }
    }
    this.contents = this.contents.substring(amount);
  }

  public String getString() {
    return this.contents;
  }

  public String getResource() {
    return this.resource;
  }

  public int getLine() {
    return this.line;
  }

  public int getCol() {
    return this.col;
  }
}

class LexerMatch {
  private Terminal terminal;
  LexerMatch() { this.terminal = null; }
  LexerMatch(Terminal terminal) { this.terminal = terminal; }
  public Terminal getTerminal() { return this.terminal; }
}

class TokenLexer {
  private Pattern regex;
  private DotParser.TerminalId terminal;

  TokenLexer(Pattern regex, DotParser.TerminalId terminal) {
    this.regex = regex;
    this.terminal = terminal;
  }

  LexerMatch match(SourceCode source) {
    Matcher m = this.regex.matcher(source.getString());
    System.out.println("[");
    //System.out.println("[\n  code='"+source.getString().trim()+"'");
    System.out.println("  pattern="+m.pattern());
    LexerMatch rval = null;
    if ( m.find() ) {
      System.out.println("  match");
      String sourceString = m.group();

      if (this.terminal != null)
        rval = new LexerMatch(new Terminal(this.terminal.id(), this.terminal.string(), Utility.base64_encode(sourceString.getBytes()), source.getResource(), source.getLine(), source.getCol()));
      else
        rval = new LexerMatch();

      source.advance(sourceString.length());
    }
    System.out.println("]");
    return rval;
  }
}

public class Lexer {
  public static void main(String[] args) {
    ArrayList<TokenLexer> regex = new ArrayList<TokenLexer>();
    regex.add( new TokenLexer(Pattern.compile("^digraph(?=[^a-zA-Z_]|$)"), DotParser.TerminalId.TERMINAL_DIGRAPH) );
    regex.add( new TokenLexer(Pattern.compile("^graph(?=[^a-zA-Z_]|$)"), DotParser.TerminalId.TERMINAL_GRAPH) );
    regex.add( new TokenLexer(Pattern.compile("^subgraph(?=[^a-zA-Z_]|$)"), DotParser.TerminalId.TERMINAL_SUBGRAPH) );
    regex.add( new TokenLexer(Pattern.compile("^strict(?=[^a-zA-Z_]|$)"), DotParser.TerminalId.TERMINAL_STRICT) );
    regex.add( new TokenLexer(Pattern.compile("^edge(?=[^a-zA-Z_]|$)"), DotParser.TerminalId.TERMINAL_EDGE) );
    regex.add( new TokenLexer(Pattern.compile("^node(?=[^a-zA-Z_]|$)"), DotParser.TerminalId.TERMINAL_NODE) );
    regex.add( new TokenLexer(Pattern.compile("^;"), DotParser.TerminalId.TERMINAL_SEMI) );
    regex.add( new TokenLexer(Pattern.compile("^\\}"), DotParser.TerminalId.TERMINAL_RBRACE) );
    regex.add( new TokenLexer(Pattern.compile("^\\{"), DotParser.TerminalId.TERMINAL_LBRACE) );
    regex.add( new TokenLexer(Pattern.compile("^\\["), DotParser.TerminalId.TERMINAL_LSQUARE) );
    regex.add( new TokenLexer(Pattern.compile("^\\]"), DotParser.TerminalId.TERMINAL_RSQUARE) );
    regex.add( new TokenLexer(Pattern.compile("^\u002d\u002d"), DotParser.TerminalId.TERMINAL_DASHDASH) );
    regex.add( new TokenLexer(Pattern.compile("^\u002d\u003e"), DotParser.TerminalId.TERMINAL_ARROW) );
    regex.add( new TokenLexer(Pattern.compile("^,"), DotParser.TerminalId.TERMINAL_COMMA) );
    regex.add( new TokenLexer(Pattern.compile("^:"), DotParser.TerminalId.TERMINAL_COLON) );
    regex.add( new TokenLexer(Pattern.compile("^="), DotParser.TerminalId.TERMINAL_ASSIGN) );
    regex.add( new TokenLexer(Pattern.compile("^([a-zA-Z\u0200-\u0377_]([0-9a-zA-Z\u0200-\u0377_])*|\"(\\\"|[^\"])*?\")|[-]?(\\.[0-9]+|[0-9]+(.[0-9]*)?)"), DotParser.TerminalId.TERMINAL_IDENTIFIER) );
    regex.add( new TokenLexer(Pattern.compile("^\\s+"), null) );

    if ( args.length < 1 ) {
      System.err.println("Usage: Lexer <input file>");
      System.exit(-1);
    }

    try {
      SourceCode code = new SourceCode(new File(args[0]));
      ArrayList<String> terminal_strings = new ArrayList<String>();
      boolean progress = true;

      while (progress) {
        progress = false;
        for ( TokenLexer lexer : regex ) {
          LexerMatch match = lexer.match(code);
          if (match != null) {
            progress = true;
            if (match.getTerminal() != null) {
              terminal_strings.add( "  " + match.getTerminal() );
            }
            break;
          }
        }
      }

      System.out.println("[");
      System.out.println( Utility.join(terminal_strings, ",\n") );
      System.out.println("]");
      System.out.flush();
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-1);
    }
  }
}
