package desktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.GroupLayout.Group;

import org.drools.compiler.rule.builder.dialect.java.parser.JavaParser.type_return;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;

public class Application {
    ScheduleSolverFactoryConfig m_scheduleSolverFactoryConfig;
    SolverConfig m_solverConfig;
    String[] instrumentList;
    HashMap<Integer, Student> studentData; // TODO: is this the right type?
    HashMap<String, Integer> instrumentMap;
    List<String> instructorList;
    List<Student> studentList;

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public Application() {
        JsonFileHandler handler = new JsonFileHandler();
        studentData = generateStudentData();
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("CLASSPATH: " + System.getProperty("java.class.path"));
        Logging logging = LoggingOptions.getDefaultInstance().getService();

        LogEntry entry = LogEntry.newBuilder(Payload.StringPayload.of("Testing Google Cloud Logging from Java"))
                .setSeverity(Severity.INFO)
                .setLogName("my-spreadsheet-log")
                .build();

        // logging.write(java.util.Collections.singleton(entry));
        System.out.println("Log entry written to Google Cloud!");
        System.out.println("print");

        SolverConfig solverConfig = SolverConfig.createFromXmlResource(
                "app/src/main/resources/solverConfig.xml",
                getClass().getClassLoader());
        HashMap<String, Integer> instrumentMap = new HashMap<>();
        instrumentMap.put("Drums", 0);
        instrumentMap.put("Bass", 0);
        instrumentMap.put("Guitar", 0);
        instrumentMap.put("Voice", 0);
        instrumentMap.put("Keys", 0);
        // groupList.put(new PerformanceGroup())

        GroupSchedule finalSchedule = runOptimizer(studentData);
        handler.writeGroupScheduleToJson(finalSchedule.getGroupList(), "travelling_data.json");
    }

    public void runApp() {

    }

    public GroupSchedule runOptimizer(HashMap<Integer, Student> studentData) {
        // Step 1: Initialize the GroupSchedule with studentData
        GroupSchedule initialSchedule = new GroupSchedule(studentData); // Pass student data here
        LOGGER.info("Initial schedule created with student data: " + initialSchedule);

        // Step 2: Create an initial SolverConfig for Branch and Bound
        SolverConfig branchAndBoundSolverConfig = ScheduleSolverFactoryConfig.createBranchAndBoundSolverConfig();
        SolverFactory<GroupSchedule> branchAndBoundSolverFactory = SolverFactory.create(branchAndBoundSolverConfig);
        Solver<GroupSchedule> branchAndBoundSolver = branchAndBoundSolverFactory.buildSolver();

        // Step 3: Run the solver briefly with Branch and Bound to get an initial
        // solution
        GroupSchedule solvedSchedule = branchAndBoundSolver.solve(initialSchedule);
        LOGGER.info("Initial Branch and Bound solution: " + solvedSchedule);

        // Step 4: Apply Local Search (Tabu Search) for further optimization
        SolverConfig localSearchSolverConfig = ScheduleSolverFactoryConfig.createTabuSearchSolverConfig();
        SolverFactory<GroupSchedule> localSearchSolverFactory = SolverFactory.create(localSearchSolverConfig);
        Solver<GroupSchedule> localSearchSolver = localSearchSolverFactory.buildSolver();

        // Use the result from Branch and Bound as the starting point for Local Search
        GroupSchedule localSearchSchedule = localSearchSolver.solve(solvedSchedule);
        LOGGER.info("Local Search (Tabu Search) result: " + localSearchSchedule);

        // Step 5: Apply Simulated Annealing for further refinement
        SolverConfig simulatedAnnealingSolverConfig = ScheduleSolverFactoryConfig
                .createSimulatedAnnealingSolverConfig();
        SolverFactory<GroupSchedule> simulatedAnnealingSolverFactory = SolverFactory
                .create(simulatedAnnealingSolverConfig);
        Solver<GroupSchedule> simulatedAnnealingSolver = simulatedAnnealingSolverFactory.buildSolver();

        // Use the result from Local Search as the starting point for Simulated
        // Annealing
        GroupSchedule finalSchedule = simulatedAnnealingSolver.solve(localSearchSchedule);
        LOGGER.info("Simulated Annealing result: " + finalSchedule);

        // Step 6: Return the final optimized schedule
        return finalSchedule;
    }
    // TODO: import instrumentList
    // instrumentMap.put("Drums", 1);
    // groupList.add(new PerformanceGroup(id++, "Alternative Rock", instrumentList,
    // 9));
    // TODO: Add all performance groups

    public static HashMap<String, PerformanceGroup> generateGroupData() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper
                    .readTree(new File("C:\\Users\\anika\\Desktop\\CSIBSL\\ScheduleOptimizer\\travelling_data.json"));
            JsonNode groupsNode = rootNode.get("groups");
            HashMap<String, PerformanceGroup> groupMap = new HashMap<>();
            for (JsonNode groupNode : groupsNode) {
                List<String> instruments = new ArrayList<>();
                for (JsonNode instrumentSlot : groupNode.get("instrumentSlots")) {
                    String instrument = groupNode.get("instrumentSlots").asText();
                    instruments.add(instrument);
                }
                groupMap.put(groupNode.get("groupName").asText(), new PerformanceGroup(
                        0,
                        groupNode.get("groupName").asText(),
                        instruments,
                        groupNode.get("instrumentSlots").size()));
            }
            return groupMap;
        } catch (IOException e) {
            System.out.println("whatever 2");
            return null;
        }
    }

    public static HashMap<Integer, Student> generateStudentData() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<TimeSlot> timeSlotList = new ArrayList<TimeSlot>();
        timeSlotList.add(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(4, 30), LocalTime.of(6, 0)));
        // TODO: Add all time slots

        List<Room> roomList = new ArrayList<Room>(2);
        roomList.add(new Room(8));
        roomList.add(new Room(9));

        ArrayList<PerformanceGroup> groupList = new ArrayList<PerformanceGroup>();
        try {
            JsonNode rootNode = objectMapper
                    .readTree(new File("C:\\Users\\anika\\Desktop\\CSIBSL\\ScheduleOptimizer\\travelling_data.json"));

            JsonNode studentsNode = rootNode.get("students");
            HashMap<Integer, Student> studentMap = new HashMap<>();
            for (JsonNode studentNode : studentsNode) {
                int id = studentNode.get("id").asInt();
                String studentName = studentNode.get("studentName").asText();
                String instrument = studentNode.get("instrument").asText();
                int tenure = studentNode.get("tenure").asInt();

                HashMap<String, PerformanceGroup> groupMap = generateGroupData();
                ArrayList<PerformanceGroup> topChoices = new ArrayList<>();
                topChoices.add(groupMap.get(studentNode.get("firstChoice").asText()));
                topChoices.add(groupMap.get(studentNode.get("secondChoice").asText()));
                topChoices.add(groupMap.get(rootNode.get("thirdChoice").asText()));
                topChoices.add(groupMap.get(rootNode.get("fourthChoice").asText()));
                studentMap.put(id, new Student(
                        id,
                        rootNode.get("studentName").asText(),
                        rootNode.get("instrument").asText(),
                        rootNode.get("tenure").asInt(),
                        topChoices));
            }

            return studentMap;
        } catch (IOException e) {
            System.out.println("whatever");
            return null;
        }
    }

    public ArrayList<Student> chooseGroupByTenure(List<Student> studentList) {
        ArrayList<Student> assignedStudentList = new ArrayList<>();
        for (int i = 0; i < studentList.size(); i++) {
            Student student = studentList.get(i);
            if (!assignedStudentList.contains(student)) {
                for (int j = 0; j < 4; j++) { // iterate through choices
                    PerformanceGroup groupChoice = student.getChoices()[j];
                    // if the choice and timeslot are available or the choice and another
                    // overlapping timeslot are available
                    if (groupChoice.getSlotsRemaining(student.getInstrument()) > 0
                            && (findTimeSlotIntersection(groupChoice.getTimeSlots(),
                                    student.getTimeSlotChoices()).size() >= 1)) {
                        student.assignGroup(student.getChoices()[j]);
                        TimeSlot assignedTimeSlot = findTimeSlotIntersection(groupChoice.getTimeSlots(),
                                student.getTimeSlotChoices()).get(0);
                        groupChoice.setTimeSlot(assignedTimeSlot);
                        assignedStudentList.add(student);
                    }
                }
            }
        }
        return assignedStudentList;
    }

    // check for overlap between performanceGroup potential TimeSlots and Student
    // chosen TimeSlots
    public List<TimeSlot> findTimeSlotIntersection(List<TimeSlot> timeSlots1, List<TimeSlot> timeSlots2) {
        List<TimeSlot> intersection = new ArrayList<>();

        for (TimeSlot timeSlot1 : timeSlots1) {
            for (TimeSlot timeSlot2 : timeSlots2) {
                if (timeSlot1.equals(timeSlot2)) { // Use equals for comparison
                    if (!intersection.contains(timeSlot1)) {
                        intersection.add(timeSlot1);
                    }
                }
            }
        }
        return intersection;
    }

    // get overlapping timeSlot choices between students
    public HashMap<StudentPair, List<TimeSlot>> getOverlappingTimeSlots(List<Student> studentList) {
        HashMap<StudentPair, List<TimeSlot>> map = new HashMap<StudentPair, List<TimeSlot>>();
        for (int i = 0; i < studentList.size(); i++) {
            Student student1 = studentList.get(i);
            Student student2 = studentList.get(i + 1);
            List<TimeSlot> intersection = findTimeSlotIntersection(student1.getTimeSlotChoices(),
                    student2.getTimeSlotChoices());
            if (intersection.size() > 1) {
                map.put(new StudentPair(student1, student2), intersection);
            } else {
                map.put(new StudentPair(student1, student2), Collections.emptyList());
            }
        }
        return map;
    }

    public List<Student> mergeSort(List<Student> studentList) {
        int length = studentList.size();
        if (length == 1) {
            return studentList;
        }
        int mid = studentList.get(length / 2).getTenureScore();
        List<Student> leftHalf = mergeSort(studentList.subList(0, mid));
        List<Student> rightHalf = mergeSort(studentList.subList(mid, length));
        return merge(leftHalf, rightHalf);
    }
    // Note: recursion for complexity

    public List<Student> merge(List<Student> left, List<Student> right) {
        List<Student> output = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getTenureScore() > right.get(j).getTenureScore()) {
                output.add(left.get(i));
                i++;
            } else {
                output.add(right.get(j));
                j++;
            }
        }
        output.addAll(left.subList(i, left.size()));
        output.addAll(right.subList(j, right.size()));

        return output;
    }
}
