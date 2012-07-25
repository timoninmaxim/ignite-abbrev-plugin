// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */
package org.gridgain.inspection.abbrev;

import com.intellij.codeInsight.daemon.*;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.refactoring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Inspection that checks variable names for usage of restricted words that
 * need to be abbreviated.
 *
 * @author @java.author
 * @version @java.version
 */
public class AbbreviationInspection extends BaseJavaLocalInspectionTool {
    /** Abbreviation rules. */
    private AbbreviationRules abbreviationRules;

    /** User message for options panel. */
    private String userMsg;

    /**
     * Constructor.
     */
    public AbbreviationInspection() {
        String ggHome = System.getenv("GRIDGAIN_HOME");

        if (ggHome == null) {
            userMsg = "GRIDGAIN_HOME environment variable was not found. Using hard-coded abbreviation table.";

            abbreviationRules = AbbreviationRules.getInstance(null);
        }
        else {
            File abbrevFile = new File(new File(ggHome), "idea" + File.separatorChar + "abbreviation.properties");

            if (!abbrevFile.exists() || !abbrevFile.isFile()) {
                userMsg = "${GRIDGAIN_HOME}/idea/abbreviation.properties was not found. Using hard-coded " +
                    "abbreviation table.";

                abbreviationRules = AbbreviationRules.getInstance(null);
            }
            else {
                userMsg = "Using " + abbrevFile.getAbsolutePath() + " as abbreviation rules file.";

                abbreviationRules = AbbreviationRules.getInstance(abbrevFile);
            }
        }
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getGroupDisplayName() {
        return GroupNames.STYLE_GROUP_NAME;
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getDisplayName() {
        return "Incorrect abbreviation usage";
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getShortName() {
        return "AbbreviationUsage";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
        final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            /** {@inheritDoc} */
            @Override public void visitField(PsiField field) {
                boolean isFinal = false;

                boolean isStatic = false;

                if (field instanceof PsiEnumConstant)
                    return;

                for (PsiElement el : field.getChildren()) {
                    if (el instanceof PsiModifierList) {
                        PsiModifierList modifiers = (PsiModifierList)el;

                        isFinal = modifiers.hasExplicitModifier("final");

                        isStatic = modifiers.hasExplicitModifier("static");
                    }

                    if (el instanceof PsiIdentifier && !(isFinal && isStatic))
                        checkShouldAbbreviate(field, el);
                }
            }

            /** {@inheritDoc} */
            @Override public void visitLocalVariable(PsiLocalVariable variable) {
                for (PsiElement el : variable.getChildren()) {
                    if (el instanceof PsiIdentifier)
                        checkShouldAbbreviate(variable, el);
                }
            }

            /** {@inheritDoc} */
            @Override public void visitMethod(PsiMethod mtd) {
                for (PsiParameter par : mtd.getParameterList().getParameters()) {
                    for (PsiElement el : par.getChildren()) {
                        if (el instanceof PsiIdentifier)
                            checkShouldAbbreviate(par, el);
                    }
                }
            }

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param toCheck Element to check and rename.
             * @param el Element to highlight the problem.
             */
            private void checkShouldAbbreviate(PsiNamedElement toCheck, PsiElement el) {
                List<String> nameParts = nameParts(toCheck.getName());

                for (String part : nameParts) {
                    if (getAbbreviation(part) != null) {
                        holder.registerProblem(el, "Abbreviation should be used",
                            new RenameToFix(replaceWithAbbreviations(nameParts)));

                        break;
                    }
                }
            }
        };
    }

    /**
     * Creates panel with user message.
     *
     * @return Panel instance.
     */
    @Override public javax.swing.JComponent createOptionsPanel() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        javax.swing.JLabel msg = new javax.swing.JLabel(userMsg);

        panel.add(msg);

        return panel;
    }

    /**
     * Renames variable to a given name.
     */
    private class RenameToFix implements LocalQuickFix {
        /** New proposed variable name. */
        private String name;

        /**
         * Creates rename to fix.
         *
         * @param name New variable name.
         */
        private RenameToFix(@NotNull String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        @NotNull public String getName() {
            return "Rename to " + name;
        }

        /** {@inheritDoc} */
        @NotNull public String getFamilyName() {
            return "";
        }

        /** {@inheritDoc} */
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descr) {
            PsiElement element = descr.getPsiElement().getParent();

            rename(project, element, name);
        }

        /**
         * Renames given element to a given name.
         *
         * @param project Project in which variable is located.
         * @param element Element to rename.
         * @param name New element name.
         */
        private void rename(@NotNull Project project, @NotNull PsiElement element, @NotNull String name) {
            JavaRefactoringFactory factory = JavaRefactoringFactory.getInstance(project);

            RenameRefactoring renRefactoring = factory.createRename(element, name, false, false);

            renRefactoring.run();
        }
    }

    /**
     * Constructs abbreviated name from parts of wrong name.
     *
     * @param oldNameParts Split of variable name.
     * @return Abbreviated variable name.
     */
    private String replaceWithAbbreviations(List<String> oldNameParts) {
        StringBuilder sb = new StringBuilder();

        for (String part : oldNameParts) {
            String abbrev = getAbbreviation(part);

            if (abbrev == null)
                 sb.append(part);
            else {
                // Only the following cases are possible: count, Count and COUNT since
                // parser splits tokens based on this rule.
                int pos = sb.length();

                sb.append(abbrev);

                if (Character.isUpperCase(part.charAt(0))) {
                    sb.setCharAt(pos, Character.toUpperCase(sb.charAt(pos)));

                    pos++;

                    if (Character.isUpperCase(part.charAt(part.length() - 1))) {
                        // Full abbreviation, like COUNT
                        while (pos < sb.length()) {
                            sb.setCharAt(pos, Character.toUpperCase(sb.charAt(pos)));

                            pos++;
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * Performs lookup of name part in abbreviation table.
     *
     * @param namePart Name part to lookup.
     * @return Abbreviation for given name or {@code null} if there is no such abbreviation.
     */
    @Nullable private String getAbbreviation(String namePart) {
        return abbreviationRules.getAbbreviation(namePart);
    }

    /**
     * Enum represents state of variable name parser.
     */
    private enum ParserState {
        /** State when no input symbols parsed yet. */
        START,

        /** First symbol parsed was capital. */
        CAPITAL,

        /** Parser is inside word token. */
        WORD,

        /** Parser is inside number. */
        NUM,

        /** Parser is inside abbreviation in capital letters. */
        ABBREVIATION
    }

    /**
     * Splits variable name into parts according to java naming conventions.
     *
     * @param name Variable or field name.
     * @return List containing variable name parts.
     */
    List<String> nameParts(String name) {
        List<String> res = new LinkedList<String>();

        StringBuilder sb = new StringBuilder();

        ParserState state = ParserState.START;

        char pending = 0;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            switch (state) {
                case START:
                    sb.append(c);

                    if (Character.isLowerCase(c))
                        state = ParserState.WORD;
                    else if (Character.isUpperCase(c))
                        state = ParserState.CAPITAL;
                    else if (Character.isDigit(c))
                        state = ParserState.NUM;
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        // Remain in start state.
                    }
                    break;

                case CAPITAL:
                    if (Character.isLowerCase(c)) {
                        sb.append(c);

                        state = ParserState.WORD;
                    }
                    else if (Character.isUpperCase(c)) {
                        pending = c;

                        state = ParserState.ABBREVIATION;
                    }
                    else if (Character.isDigit(c)) {
                        res.add(sb.toString());

                        sb.setLength(0);

                        sb.append(c);

                        state = ParserState.NUM;
                    }
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        res.add(String.valueOf(c));

                        state = ParserState.START;
                    }
                    break;

                case WORD:
                    if (Character.isLowerCase(c))
                        sb.append(c);
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        state = ParserState.START;

                        // Unread.
                        i--;
                    }
                    break;

                case ABBREVIATION:
                    if (Character.isUpperCase(c)) {
                        sb.append(pending);

                        pending = c;
                    }
                    else if (Character.isLowerCase(c)) {
                        res.add(sb.toString());

                        sb.setLength(0);

                        sb.append(pending).append(c);

                        state = ParserState.WORD;
                    }
                    else {
                        sb.append(pending);

                        res.add(sb.toString());

                        sb.setLength(0);

                        state = ParserState.START;

                        // Unread.
                        i--;
                    }
                    break;

                case NUM:
                    if (Character.isDigit(c))
                        sb.append(c);
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        state = ParserState.START;

                        // Unread.
                        i--;
                    }
                    break;
            }
        }

        if (state == ParserState.ABBREVIATION)
            sb.append(pending);

        if (sb.length() > 0)
            res.add(sb.toString());

        return res;
    }

    public static void main(String[] args) {
        AbbreviationInspection i = new AbbreviationInspection();

        assert listsEqual(Arrays.asList("count"), i.nameParts("count"));
        assert listsEqual(Arrays.asList("Count"), i.nameParts("Count"));
        assert listsEqual(Arrays.asList("Count", "1"), i.nameParts("Count1"));
        assert listsEqual(Arrays.asList("my", "Count"), i.nameParts("myCount"));
        assert listsEqual(Arrays.asList("my", "Count"), i.nameParts("myCount"));
        assert listsEqual(Arrays.asList("MY", "_", "COUNT"), i.nameParts("MY_COUNT"));
        assert listsEqual(Arrays.asList("MY", "_", "COUNT", "1"), i.nameParts("MY_COUNT1"));
        assert listsEqual(Arrays.asList("_", "_", "my", "_", "Count"), i.nameParts("__my_Count"));
        assert listsEqual(Arrays.asList("my", "123", "Count"), i.nameParts("my123Count"));
        assert listsEqual(Arrays.asList("my", "_","123", "_", "Count"), i.nameParts("my_123_Count"));
        assert listsEqual(Arrays.asList("my","BIG", "Count"), i.nameParts("myBIGCount"));
        assert listsEqual(Arrays.asList("my","BIG", "_", "count"), i.nameParts("myBIG_count"));
        assert listsEqual(Arrays.asList("my","1", "BIG", "2", "count"), i.nameParts("my1BIG2count"));
        assert listsEqual(Arrays.asList("my","1", "BIG", "2", "Count"), i.nameParts("my1BIG2Count"));


        assert "cnt".equals(i.replaceWithAbbreviations(i.nameParts("count")));
        assert "Cnt".equals(i.replaceWithAbbreviations(i.nameParts("Count")));
        assert "myCnt".equals(i.replaceWithAbbreviations(i.nameParts("myCount")));
        assert "MY_CNT".equals(i.replaceWithAbbreviations(i.nameParts("MY_COUNT")));
    }

    private static boolean listsEqual(List<String> one, List<String> two) {
        if (one.size() != two.size())
            return false;

        for (int i = 0; i < one.size(); i++) {
            if (!one.get(i).equals(two.get(i)))
                return false;
        }

        return true;
    }
}
