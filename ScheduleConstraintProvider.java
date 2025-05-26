package desktop;

import org.glassfish.jaxb.runtime.v2.runtime.reflect.opt.Const;
import org.kie.api.command.Command;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

public class ScheduleConstraintProvider implements ConstraintProvider {
        ConstraintFactory m_constraintFactory;

        public ScheduleConstraintProvider(ConstraintFactory constraintFactory) {
                m_constraintFactory = constraintFactory;
                defineConstraints(constraintFactory);
        }

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
                return new Constraint[] {
                                // Hard constraints
                                roomConflict(constraintFactory),
                                sameStudentConflict(constraintFactory),
                                // instructorConflict(constraintFactory),
                                studentTenureConstraint(constraintFactory),
                                dailyGroupSurplusConstraint(constraintFactory),
                                instrumentCapacityConstraint(constraintFactory, null),
                                // Soft constraints
                                maximizeFirstChoice(constraintFactory),
                                minimizeLastChoice(constraintFactory)
                };
        }

        private Constraint roomConflict(ConstraintFactory constraintFactory) {
                // Rooms can only accomodate one group rehearsal at a time

                return constraintFactory.forEach(PerformanceGroup.class).join(PerformanceGroup.class,
                                Joiners.equal(PerformanceGroup::getTimeSlot), // if both groups have the same timeslot
                                Joiners.equal(PerformanceGroup::getRoom), // if both groups have the same room
                                Joiners.lessThan(PerformanceGroup::getId)) // prevents duplicate checks
                                .penalize(HardSoftScore.ONE_HARD) // penalizes score with a hard conflict
                                .asConstraint("Room conflict"); // makes this a constraint
        }

        // THE SRC CODE OF THE .JOIN METHOD
        /*
         * default <B> BiConstraintStream<A, B> join(Class<B> otherClass, BiJoiner<A, B>
         * joiner1, BiJoiner<A, B> joiner2,
         * BiJoiner<A, B> joiner3) {
         * return join(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
         */
        private Constraint sameStudentConflict(ConstraintFactory constraintFactory) {

                return constraintFactory.forEach(PerformanceGroup.class)
                                // pairs every A type with a B type under the specified conditions
                                .join(PerformanceGroup.class,
                                                Joiners.equal(PerformanceGroup::getTimeSlot),
                                                Joiners.equal(PerformanceGroup::getStudents),
                                                Joiners.lessThan(PerformanceGroup::getId))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Same student conflict");
        }


        private Constraint studentTenureConstraint(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Student.class)
                                // if one student has less tenure and gets the performance group that both have
                                // as first choice (assuming same instrument and choice for the group)
                                .join(Student.class,
                                                Joiners.equal(Student::getChoices), // check only the choice of the
                                                                                    // group
                                                Joiners.equal(Student::getInstrument),
                                                Joiners.lessThan(Student::getTenureScore),
                                                Joiners.lessThan(Student::getAchievedChoice))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Student tenure constraint");
        }

        private Constraint dailyGroupSurplusConstraint(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(PerformanceGroup.class)
                                .join(PerformanceGroup.class,
                                                Joiners.equal(PerformanceGroup::extractDayOfWeek))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Daily rehearsal number surplus");
        }

        private Constraint maximizeFirstChoice(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Student.class)
                                .filter(Student::hasTopChoice)
                                .reward(HardSoftScore.ONE_SOFT)
                                .asConstraint("Maximize first choice");
        }

        private Constraint minimizeLastChoice(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Student.class)
                                .filter(Student::hasLastChoice)
                                .penalize(HardSoftScore.ONE_SOFT)
                                .asConstraint("Minimize last choice");
        }

        private Constraint instrumentCapacityConstraint(ConstraintFactory constraintFactory, Student student) {
                return constraintFactory.forEach(PerformanceGroup.class)
                                .filter(performanceGroup -> performanceGroup
                                                .getTakenSlots(student.getInstrument()) > performanceGroup
                                                                .getInstrumentSlots(student.getInstrument()))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Instrument capacity");
        }

        // private Constraint
}
