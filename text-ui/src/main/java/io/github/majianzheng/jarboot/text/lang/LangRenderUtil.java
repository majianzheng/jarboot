package io.github.majianzheng.jarboot.text.lang;

import io.github.majianzheng.jarboot.text.Style;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.*;

import javax.swing.text.Segment;
import java.util.*;

/**
 * 
 * @author duanling 2015年12月3日 上午11:38:55
 *
 */
public class LangRenderUtil {
    public static final String plain = "plain";
    public static final String actionscript = "actionscript";
    public static final String asm = "asm";
    public static final String bbcode = "bbcode";
    public static final String c = "c";
    public static final String clojure = "clojure";
    public static final String cpp = "cpp";
    public static final String cs = "cs";
    public static final String css = "css";
    public static final String d = "d";
    public static final String dart = "dart";
    public static final String delphi = "delphi";
    public static final String dtd = "dtd";
    public static final String fortran = "fortran";
    public static final String groovy = "groovy";
    public static final String htaccess = "htaccess";
    public static final String html = "html";
    public static final String java = "java";
    public static final String javascript = "javascript";
    public static final String jshintrc = "jshintrc";
    public static final String json = "json";
    public static final String jsp = "jsp";
    public static final String latex = "latex";
    public static final String less = "less";
    public static final String lisp = "lisp";
    public static final String lua = "lua";
    public static final String makefile = "makefile";
    public static final String mxml = "mxml";
    public static final String nsis = "nsis";
    public static final String perl = "perl";
    public static final String php = "php";
    public static final String properties = "properties";
    public static final String python = "python";
    public static final String ruby = "ruby";
    public static final String sas = "sas";
    public static final String scala = "scala";
    public static final String sql = "sql";
    public static final String tcl = "tcl";
    public static final String unix = "unix";
    public static final String vb = "vb";
    public static final String bat = "bat";
    public static final String xml = "xml";

    private static Map<String, Class<?>> tokenMakerMap = new HashMap<String, Class<?>>();

    static {
        tokenMakerMap.put(plain, PlainTextTokenMaker.class);
        tokenMakerMap.put(actionscript, ActionScriptTokenMaker.class);
        tokenMakerMap.put(asm, AssemblerX86TokenMaker.class);
        tokenMakerMap.put(bbcode, BBCodeTokenMaker.class);
        tokenMakerMap.put(c, CTokenMaker.class);
        tokenMakerMap.put(clojure, ClojureTokenMaker.class);
        tokenMakerMap.put(cpp, CPlusPlusTokenMaker.class);
        tokenMakerMap.put(cs, CSharpTokenMaker.class);
        tokenMakerMap.put(css, CSSTokenMaker.class);
        tokenMakerMap.put(d, DTokenMaker.class);
        tokenMakerMap.put(dart, DartTokenMaker.class);
        tokenMakerMap.put(delphi, DelphiTokenMaker.class);
        tokenMakerMap.put(dtd, DtdTokenMaker.class);
        tokenMakerMap.put(fortran, FortranTokenMaker.class);
        tokenMakerMap.put(groovy, GroovyTokenMaker.class);
        tokenMakerMap.put(htaccess, HtaccessTokenMaker.class);
        tokenMakerMap.put(html, HTMLTokenMaker.class);
        tokenMakerMap.put(java, JavaTokenMaker.class);
        tokenMakerMap.put(javascript, JavaScriptTokenMaker.class);
        tokenMakerMap.put(jshintrc, JshintrcTokenMaker.class);
        tokenMakerMap.put(json, JsonTokenMaker.class);
        tokenMakerMap.put(jsp, JSPTokenMaker.class);
        tokenMakerMap.put(latex, LatexTokenMaker.class);
        tokenMakerMap.put(less, LessTokenMaker.class);
        tokenMakerMap.put(lisp, LispTokenMaker.class);
        tokenMakerMap.put(lua, LuaTokenMaker.class);
        tokenMakerMap.put(makefile, MakefileTokenMaker.class);
        tokenMakerMap.put(mxml, MxmlTokenMaker.class);
        tokenMakerMap.put(nsis, NSISTokenMaker.class);
        tokenMakerMap.put(perl, PerlTokenMaker.class);
        tokenMakerMap.put(php, PHPTokenMaker.class);
        tokenMakerMap.put(properties, PropertiesFileTokenMaker.class);
        tokenMakerMap.put(python, PythonTokenMaker.class);
        tokenMakerMap.put(ruby, RubyTokenMaker.class);
        tokenMakerMap.put(sas, SASTokenMaker.class);
        tokenMakerMap.put(scala, ScalaTokenMaker.class);
        tokenMakerMap.put(sql, SQLTokenMaker.class);
        tokenMakerMap.put(tcl, TclTokenMaker.class);
        tokenMakerMap.put(unix, UnixShellTokenMaker.class);
        tokenMakerMap.put(vb, VisualBasicTokenMaker.class);
        tokenMakerMap.put(bat, WindowsBatchTokenMaker.class);
        tokenMakerMap.put(xml, XMLTokenMaker.class);
    }

    static TokenMaker createTokenMaker(String lang) {
        Class<?> clazz = tokenMakerMap.get(lang);
        if (clazz == null) {
            clazz = JavaTokenMaker.class;
        }
        try {
            return (TokenMaker) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("construct TokenMaker error!, lang:" + lang, e);
        }
    }

    static List<String> lines(String multilines) {
        List<String> result = new ArrayList<String>();
        Scanner scanner = new Scanner(multilines);
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.add(line);
            }
        } finally {
            scanner.close();
        }
        return result;
    }

    static public String render(String code, String lang, HighLightTheme theme) {
        StringBuilder sb = new StringBuilder(8192);

        TokenMaker tm = createTokenMaker(lang);

        int previousLineTokenType = TokenTypes.NULL;
        for (String line : lines(code)) {
            Segment segment = new Segment(line.toCharArray(), 0, line.length());

            Token token = tm.getTokenList(segment, previousLineTokenType, 0);

            while (token != null) {

                int type = token.getType();
                if (type < 0) {
                    break;
                }
                previousLineTokenType = type;

                Style style = theme.getStyle(type);
                if (style != null) {
                    sb.append(style.toAnsiSequence());
                }
                String lexeme = token.getLexeme();
                if (lexeme != null) {
                    sb.append(token.getLexeme());
                }

                if (style != null) {
                    sb.append(Style.reset.toAnsiSequence());
                }

                token = token.getNextToken();
            }

            sb.append('\n');
        }

        return sb.toString();
    }

    static public String render(String code) {
        return render(code, "java");
    }

    static public String render(String code, String lang) {
        return render(code, lang, HighLightTheme.defaultTheme());
    }
}
