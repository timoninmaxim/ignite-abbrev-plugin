/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.idea.inspection;

import java.util.List;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.apache.ignite.idea.inspection.abbrev.IgniteAbbreviationInspection;
import org.apache.ignite.idea.inspection.abbrev.IgniteScalaAbbreviationInspection;
import org.apache.ignite.idea.inspection.comment.IgniteCommentInspection;

/** Tests Apache Ignite inspections. */
public class IgniteInspectionTest extends LightJavaCodeInsightFixtureTestCase {
    /** {@inheritDoc} */
    @Override protected void setUp() throws Exception {
        super.setUp();

        myFixture.enableInspections(
            new IgniteAbbreviationInspection(),
            new IgniteScalaAbbreviationInspection(),
            new IgniteAnnotationInspection(),
            new IgniteBracketInspection(),
            new IgniteCommentInspection(),
            new IgniteEmptyLineInspection(),
            new IgnitePlublicInterfaceMethodsInspection(),
            new IgniteWrongComparationInspection()
        );
    }

    /** {@inheritDoc} */
    @Override protected String getTestDataPath() {
        return "src/test/resources/inspection";
    }

    /** Tests {@link IgniteAbbreviationInspection}. */
    public void testAbbreviationInspection() {
        checkJavaQuickFix(generateFixAllIntentionNameByInspection(new IgniteAbbreviationInspection()), "Abbreviation");
    }

    /** Tests {@link IgniteAbbreviationInspection}. */
    public void testScalaAbbreviationInspection() {
        checkScalaQuickFix(generateFixAllIntentionNameByInspection(new IgniteScalaAbbreviationInspection()), "Abbreviation1");
        checkScalaQuickFix(generateFixAllIntentionNameByInspection(new IgniteScalaAbbreviationInspection()), "Abbreviation2");
    }

    /** Tests {@link IgniteBracketInspection}. */
    public void testBracketInspection() {
        checkJavaQuickFix(generateFixAllIntentionNameByInspection(new IgniteBracketInspection()), "Bracket");
    }

    /** Tests {@link IgniteCommentInspection}. */
    public void testCommentInspection() {
        checkJavaQuickFix("Add default comment", "Comment1");
        checkJavaQuickFix("Add default comment", "Comment2");
        checkJavaQuickFix("Add default comment", "Comment3");
        checkJavaQuickFix("Add /** {@inheritDoc} */", "Comment4");
        checkJavaQuickFix("Add default comment", "Comment5");
        checkJavaQuickFix("Add empty comment", "Comment6");
        checkJavaQuickFix("Add default comment", "Comment7");
    }

    /** Tests {@link IgnitePlublicInterfaceMethodsInspection}. */
    public void testPublicInterfaceMethodInspection() {
        checkJavaQuickFix(generateFixAllIntentionNameByInspection(new IgnitePlublicInterfaceMethodsInspection()), "PublicInterfaceMethod");
    }

    /** Tests {@link IgniteEmptyLineInspection}. */
    public void testEmptyLineInspection() {
        checkJavaQuickFix(generateFixAllIntentionNameByInspection(new IgniteEmptyLineInspection()), "EmptyLine1");
        checkJavaQuickFix(generateFixAllIntentionNameByInspection(new IgniteEmptyLineInspection()), "EmptyLine2");
    }

    /**
     * File name pattern 'foo.java' and 'foo.after.java' are matching before and after files
     * in the resources directory. See {@link #checkQuickFix}.
     *
     * @param intention Quick fix name.
     * @param testName  Base name of test file before quick fix.
     */
    public void checkJavaQuickFix(String intention, String testName) {
        checkQuickFix(intention, testName + ".java", testName + ".after.java");
    }

    /**
     * File name pattern 'foo.scala' and 'foo.after.scala' are matching before and after files
     * in the resources directory. See {@link #checkQuickFix}.
     *
     * @param intention Quick fix name.
     * @param testName  Base name of test file before quick fix.
     */
    public void checkScalaQuickFix(String intention, String testName) {
        checkQuickFix(intention, testName + ".scala", testName + ".after.scala");
    }

    /** Tests {@link IgniteAnnotationInspection}. */
    public void testAnnotationInspection() {
        myFixture.addClass("public class String {}");

        myFixture.addClass("package java.lang; public @interface Override {}");
        myFixture.addClass("package org.jetbrains.annotations; public @interface Nullable {}");

        checkInspection("Annotation1", "Annotation @Override must be on the same line with the method name");
        checkInspection("Annotation2", "Annotation @Nullable must be on the same line with the method name");
    }

    /** Tests {@link IgniteWrongComparationInspection}. */
    public void testWrongComparationInspection() {
        myFixture.addClass("package org.apache.ignite.internal.processors.affinity; public final class AffinityTopologyVersion {}");

        checkInspection("WrongComparation", new IgniteWrongComparationInspection().getDisplayName());
    }

    /**
     * Given the name of a test file, runs comparing references inspection quick fix and tests
     * the results against a reference outcome file.
     *
     * @param intention Quick fix name.
     * @param before Name of test file before quick fix.
     * @param after Name of test file after quick fix.
     */
    private void checkQuickFix(String intention, String before, String after) {
        myFixture.configureByFile(before);
        List<HighlightInfo> highlightInfos = myFixture.doHighlighting();
        assertFalse(highlightInfos.isEmpty());

        IntentionAction action = myFixture.getAvailableIntentions().stream()
            .filter(action1 -> intention.equals(action1.getText()))
            .findAny()
            .orElseThrow(() -> new AssertionError("Could not found intention " + intention));

        myFixture.launchAction(action);

        myFixture.checkResultByFile(after);
    }

    /**
     * Given the name of a test file, tests description of inspection at caret.
     *
     * @param testName    Test file name base
     * @param description Inspection description
     */
    private void checkInspection(String testName, String description) {
        myFixture.configureByFile(testName + ".java");

        List<HighlightInfo> highlightInfos = myFixture.doHighlighting();

        assertTrue(highlightInfos.stream().anyMatch(info -> description.equals(info.getDescription())));
    }

    /** */
    private String generateFixAllIntentionNameByInspection(InspectionProfileEntry inspection) {
        return "Fix all '" + inspection.getDisplayName() + "' problems in file";
    }
}
