/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.test.impl.score;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Before;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirectorFactory;
import org.optaplanner.test.impl.score.buildin.hardsoft.HardSoftScoreConstraintTest;

import static org.junit.Assert.assertEquals;

/**
 * Do not extend this class directly, instead extend a specific subclass if your {@link Score} type,
 * such as {@link HardSoftScoreConstraintTest}.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see HardSoftScoreConstraintTest
 */
public abstract class AbstractScoreConstraintTest<Solution_> {

    protected ScoreDirectorFactory<Solution_> scoreDirectorFactory;

    @Before
    public void init() {
        SolverFactory<Solution_> solverFactory = createSolverFactory();
        if (solverFactory == null) {
            throw new IllegalStateException("The solverFactory (" + solverFactory + ") cannot be null.");
        }
        scoreDirectorFactory = solverFactory.buildSolver().getScoreDirectorFactory();
        Class<? extends Score> expectedScoreClass = getExpectedScoreClass();
        SolutionDescriptor<Solution_> solutionDescriptor = ((InnerScoreDirectorFactory<Solution_>) scoreDirectorFactory).getSolutionDescriptor();
        Class<? extends Score> scoreClass = solutionDescriptor.getScoreDefinition().getScoreClass();
        if (expectedScoreClass != scoreClass) {
            throw new IllegalStateException("The solution's scoreClass (" + scoreClass
                    + ") differs from the test's expectedScoreClass (" + expectedScoreClass + ").");
        }
    }

    /**
     * This method is implemented by the specific subclass of the {@link Score} type,
     * to fail fast if a {@link SolverFactory} with another {@link Score} type is used.
     * @return never null
     */
    protected abstract Class<? extends Score> getExpectedScoreClass();

    /**
     * Return the {@link SolverFactory} of which you want to test the constraints.
     * @return never null
     */
    protected abstract SolverFactory<Solution_> createSolverFactory();

    /**
     * Assert that the constraint (which is usually a score rule) of {@link PlanningSolution}
     * has the expected weight for that score level.
     * @param constraintPackage sometimes null.
     * When null, {@code constraintName} for the {@code scoreLevel} must be unique.
     * @param scoreLevel at least 0
     * @param constraintName never null, the name of the constraint, which is usually the name of the score rule
     * @param expectedWeight sometimes null, the total weight for all matches of that 1 constraint
     * @param solution never null
     */
    protected void assertConstraintWeight(
            String constraintPackage, String constraintName, int scoreLevel, Number expectedWeight,
            Solution_ solution) {
        ScoreDirector<Solution_> scoreDirector = scoreDirectorFactory.buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        ConstraintMatchTotal matchTotal = findConstraintMatchTotal(constraintPackage, constraintName, scoreLevel, scoreDirector);
        if (matchTotal == null) {
            if (expectedWeight != null) {
                if (expectedWeight instanceof Byte) {
                    assertEquals(expectedWeight, (byte) 0);
                } else if (expectedWeight instanceof Short) {
                    assertEquals(expectedWeight, (short) 0);
                } else if (expectedWeight instanceof Integer) {
                    assertEquals(expectedWeight, 0);
                } else if (expectedWeight instanceof Long) {
                    assertEquals(expectedWeight, 0L);
                } else if (expectedWeight instanceof Float) {
                    assertEquals(expectedWeight, 0F);
                } else if (expectedWeight instanceof Double) {
                    assertEquals(expectedWeight, 0D);
                } else if (expectedWeight instanceof BigInteger) {
                    assertEquals(expectedWeight, BigInteger.ZERO);
                } else if (expectedWeight instanceof BigDecimal) {
                    assertEquals(expectedWeight, BigDecimal.ZERO);
                } else {
                    throw new IllegalStateException("Unsupported " + Number.class.getSimpleName()
                            + " type (" + expectedWeight.getClass() + ") for expectedWeight (" + expectedWeight + ").");
                }
            }
        } else {
            assertEquals(expectedWeight, matchTotal.getWeightTotalAsNumber());
        }
    }

    /**
     * @param constraintPackage sometimes null.
     * When null, {@code constraintName} for the {@code scoreLevel} must be unique.
     * @param constraintName never null, the name of the constraint, which is usually the name of the score rule
     * @param scoreLevel at least 0
     * @param scoreDirector never null
     * @return null if there is no constraint matched or the constraint doesn't exist
     */
    private ConstraintMatchTotal findConstraintMatchTotal(
            String constraintPackage, String constraintName, int scoreLevel, ScoreDirector<Solution_> scoreDirector) {
        ConstraintMatchTotal matchTotal = null;
        for (ConstraintMatchTotal selectedMatchTotal : scoreDirector.getConstraintMatchTotals()) {
            if (selectedMatchTotal.getScoreLevel() == scoreLevel
                    && selectedMatchTotal.getConstraintName().equals(constraintName)
                    && (constraintPackage == null || selectedMatchTotal.getConstraintPackage().equals(constraintPackage))) {
                if (matchTotal != null) {
                    throw new IllegalArgumentException("The constraintName (" + constraintName
                            + ") is used by 2 different constraintMatches (" + matchTotal.getIdentificationString()
                            + " and " + selectedMatchTotal.getIdentificationString() + ").");
                }
                matchTotal = selectedMatchTotal;
            }
        }
        return matchTotal;
    }

}
