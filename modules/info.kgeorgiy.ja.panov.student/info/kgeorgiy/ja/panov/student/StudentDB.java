package info.kgeorgiy.ja.panov.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {
    private <T> List<T> getFields(List<Student> students, Function<Student, T> mapper) {
        return students.stream().map(mapper).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getFields(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getFields(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getFields(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getFields(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFirstNames(students).stream().collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBy(students, Student::compareTo);
    }

    private final static Comparator<Student> COMPARE_BY_NAME =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .reversed()
                    .thenComparing(Student::getId);

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBy(students, COMPARE_BY_NAME);
    }

    private <T> List<Student> findStudentBy(Collection<Student> students, T field, Function<Student, T> getField) {
        return students.stream()
                .filter(student -> getField.apply(student).equals(field))
                .sorted(COMPARE_BY_NAME).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentBy(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentBy(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentBy(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group)
                .stream()
                .collect(
                        Collectors.toMap(
                                Student::getLastName,
                                Student::getFirstName,
                                BinaryOperator.minBy(String::compareTo)
                        ));
    }

    private List<Group> getGroupsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream()
                .map(entry -> new Group(entry.getKey(), sortStudentsBy(entry.getValue(), comparator)))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsBy(students, COMPARE_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsBy(students, Comparator.comparing(Student::getId));
    }

    private GroupName getLargestGroupBy(Collection<Student> students, Comparator<Group> comparator) {
        return getGroupsByName(students).stream().max(comparator).map(Group::getName).orElse(null);
    }

    private <T, V> T getLargestGroupBy(Collection<Student> students,
                                       Comparator<Map.Entry<T, Set<V>>> comparator,
                                       Function<Student, T> classifier,
                                       Function<Student, V> mapper, T defaultResult) {
        return students.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.mapping(mapper, Collectors.toSet())))
                .entrySet().stream()
                .max(Comparator.comparing(
                        (Function<Map.Entry<T, Set<V>>, Integer>) entry -> entry.getValue().size())
                        .thenComparing(comparator))
                .map(Map.Entry::getKey).orElse(defaultResult);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(students, Map.Entry.comparingByKey(), Student::getGroup, a -> a, null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(students, Map.Entry.<GroupName, Set<String>>comparingByKey().reversed(), Student::getGroup, Student::getFirstName, null);
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getLargestGroupBy(students, Map.Entry.comparingByKey(), Student::getFirstName, Student::getGroup, "");
    }

    private <T> List<T> getWithIndices(Collection<Student> students, int[] indices, Function<List<Student>, List<T>> getter) {
        return Arrays.stream(indices)
                .mapToObj(ind -> getter.apply(students.stream().collect(Collectors.toList())).get(ind))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getWithIndices(students, indices, this::getFirstNames);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getWithIndices(students, indices, this::getLastNames);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return getWithIndices(students, indices, this::getGroups);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getWithIndices(students, indices, this::getFullNames);
    }
}
