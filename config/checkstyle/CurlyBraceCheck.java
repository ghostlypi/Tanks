import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Checks that curly braces are either both on the same line (inline format)
 * or each on their own line (expanded format). All other arrangements are violations.
 *
 * <p>Allowed:
 * <pre>
 * // Inline - both braces on the same line:
 * enum Color { RED, GREEN, BLUE }
 *
 * // Expanded - each brace alone on its own line:
 * enum Color
 * {
 *     RED,
 *     GREEN,
 *     BLUE
 * }
 * </pre>
 *
 * <p>Not allowed (opening brace shares a line with other code):
 * <pre>
 * enum Color {
 *     RED,
 *     GREEN
 * }
 * </pre>
 */
public class CurlyBraceCheck extends AbstractCheck
{
    @Override
    public int[] getDefaultTokens()
    {
        return new int[]{TokenTypes.LCURLY};
    }

    @Override
    public int[] getAcceptableTokens()
    {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens()
    {
        return getDefaultTokens();
    }

    @Override
    public void visitToken(DetailAST lcurly)
    {
        DetailAST parent = lcurly.getParent();
        if (parent == null)
        {
            return;
        }

        // The matching RCURLY is always the last child of the same parent block node
        DetailAST lastChild = parent.getLastChild();
        if (lastChild == null || lastChild.getType() != TokenTypes.RCURLY)
        {
            return;
        }
        DetailAST rcurly = lastChild;

        int lcurlyLine = lcurly.getLineNo();
        int rcurlyLine = rcurly.getLineNo();

        // Same line: always OK (inline format)
        if (lcurlyLine == rcurlyLine)
        {
            return;
        }

        // Different lines: each brace must be alone on its line
        String[] lines = getLines();

        String lcurlyLineText = lines[lcurlyLine - 1].trim();
        if (!lcurlyLineText.equals("{"))
        {
            log(lcurly,
                "Opening curly brace must be alone on its own line,"
                    + " or on the same line as the closing brace.");
        }

        String rcurlyLineText = lines[rcurlyLine - 1].trim();
        if (!rcurlyLineText.equals("}") && !rcurlyLineText.equals("};"))
        {
            log(rcurly,
                "Closing curly brace must be alone on its own line,"
                    + " or on the same line as the opening brace.");
        }
    }
}