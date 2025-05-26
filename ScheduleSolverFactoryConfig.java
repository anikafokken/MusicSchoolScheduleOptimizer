package desktop;

import javax.swing.text.html.parser.Entity;

import org.optaplanner.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import org.optaplanner.core.config.exhaustivesearch.ExhaustiveSearchType;
import org.optaplanner.core.config.heuristic.selector.entity.EntitySorterManner;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchType;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

public class ScheduleSolverFactoryConfig {

        public static SolverConfig createBranchAndBoundSolverConfig() {
                return new SolverConfig().withSolutionClass(GroupSchedule.class)
                                .withEntityClasses(Student.class)
                                .withScoreDirectorFactory(
                                                new ScoreDirectorFactoryConfig().withConstraintProviderClass(
                                                                ScheduleConstraintProvider.class))
                                .withPhases(new ExhaustiveSearchPhaseConfig()
                                                .withExhaustiveSearchType(ExhaustiveSearchType.BRANCH_AND_BOUND)
                                                .withEntitySorterManner(EntitySorterManner.DECREASING_DIFFICULTY))
                                .withTerminationConfig(new TerminationConfig());
        }

        public static SolverConfig createTabuSearchSolverConfig() {
                return new SolverConfig().withSolutionClass(GroupSchedule.class)
                                .withEntityClasses(Student.class)
                                .withScoreDirectorFactory(
                                                new ScoreDirectorFactoryConfig().withConstraintProviderClass(
                                                                ScheduleConstraintProvider.class))
                                .withPhases(new LocalSearchPhaseConfig().withLocalSearchType(
                                                LocalSearchType.TABU_SEARCH))
                                .withTerminationConfig(new TerminationConfig());
        }

        public static SolverConfig createSimulatedAnnealingSolverConfig() {
                return new SolverConfig().withSolutionClass(GroupSchedule.class)
                                .withEntityClasses(Student.class)
                                .withScoreDirectorFactory(
                                                new ScoreDirectorFactoryConfig().withConstraintProviderClass(
                                                                ScheduleConstraintProvider.class))
                                .withPhases(new LocalSearchPhaseConfig().withLocalSearchType(
                                                LocalSearchType.SIMULATED_ANNEALING))
                                .withTerminationConfig(new TerminationConfig());
        }
}
