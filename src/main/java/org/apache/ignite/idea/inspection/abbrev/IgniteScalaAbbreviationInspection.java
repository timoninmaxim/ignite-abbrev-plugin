package org.apache.ignite.idea.inspection.abbrev;

import com.intellij.codeInspection.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValue;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueDeclaration;
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariableDefinition;
import org.apache.ignite.idea.util.IgniteUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Inspects usages of Ignite abbreviations. */
public class IgniteScalaAbbreviationInspection extends LocalInspectionTool {
    /** Abbreviation exceptions. */
    private final Set<String> abbrExceptions = Set.of("value");

    /** {@inheritDoc} */
    @Override public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession ses) {
        return new ScalaElementVisitor() {
            /** {@inheritDoc} */
            @Override public void visitValueDeclaration(ScValueDeclaration v) {
                Arrays.stream(v.declaredElementsArray()).forEach(this::checkShouldAbbreviate);
            }

            /** {@inheritDoc} */
            @Override public void visitValue(ScValue v) {
                Arrays.stream(v.declaredElementsArray()).forEach(this::checkShouldAbbreviate);
            }

            /** {@inheritDoc} */
            @Override public void visitParameter(ScParameter p) {
                checkShouldAbbreviate(p, p.getNameIdentifier());
            }

            /** {@inheritDoc} */
            @Override public void visitVariableDefinition(ScVariableDefinition v) {
                Arrays.stream(v.declaredElementsArray()).forEach(this::checkShouldAbbreviate);
            }

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private void checkShouldAbbreviate(PsiNamedElement elem) {
                check0(IgniteUtils.camelCaseParts(elem.getName()), elem);
            }

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private void checkShouldAbbreviate(PsiElement elem, PsiIdentifier id) {
                check0(IgniteUtils.camelCaseParts(id.getText()), elem);
            }

            /**
             * Checks that all identifier parts are correctly abbreviated. Registers problem if needed.
             *
             * @param parts Identifier parts.
             * @param elem Checked identifier element.
             */
            private void check0(List<String> parts, PsiElement elem) {
                for (String part : parts) {
                    IgniteAbbreviationConfig config = ServiceManager.getService(elem.getProject(), IgniteAbbreviationConfig.class);

                    if (!abbrExceptions.contains(part) && config.getAbbreviation(part) != null) {
                        holder.registerProblem(elem, "Abbreviation should be used",
                            new RenameToFix(config.replaceWithAbbreviations(parts)));

                        return;
                    }
                }
            }
        };
    }

    /** Rename quick fix. */
    private static class RenameToFix implements LocalQuickFix {
        /** */
        private final String name;

        /**
         * @param name New name for the identifier.
         */
        public RenameToFix(String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        @Override public String getName() {
            return "Rename to " + name;
        }

        /** {@inheritDoc} */
        @Override public String getFamilyName() {
            return "";
        }

        /** {@inheritDoc} */
        @Override public void applyFix(Project proj, ProblemDescriptor descr) {
            PsiElement element = descr.getPsiElement();

            JavaRefactoringFactory factory = JavaRefactoringFactory.getInstance(proj);
            RenameRefactoring renRefactoring = factory.createRename(element, name, false, false);
            renRefactoring.run();
        }
    }
}
