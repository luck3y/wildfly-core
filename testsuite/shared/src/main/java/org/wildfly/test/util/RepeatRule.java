package org.wildfly.test.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule to repeat a series of tests X times. The primary use of this rule is for re-running flaky tests
 * in CI, with -Dtest=foo.bar.baz.SomeTest
 *
 * To use: annotate test class with:
 * <code>
 *   @Rule
 *   public RepeatRule rule = new RepeatRule(X);
 * </code>
 * Where X is the number of times to run the test
 *
 * @author Ken Wills <kwills@redhat.com>
 */
public class RepeatRule implements TestRule {
    private int count;

    public RepeatRule(int count) {
        this.count = count;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for (int i = 0; i < count; i++) {
                    base.evaluate();
                }
            }
        };
    }

}