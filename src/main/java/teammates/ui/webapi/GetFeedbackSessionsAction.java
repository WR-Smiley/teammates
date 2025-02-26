package teammates.ui.webapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import teammates.common.datatransfer.InstructorPermissionSet;
import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.Const;
import teammates.storage.sqlentity.Course;
import teammates.ui.output.FeedbackSessionData;
import teammates.ui.output.FeedbackSessionsData;

/**
 * Get a list of feedback sessions.
 */
public class GetFeedbackSessionsAction extends Action {

    @Override
    AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    void checkSpecificAccessControl() throws UnauthorizedAccessException {
        if (userInfo.isAdmin) {
            return;
        }

        String entityType = getNonNullRequestParamValue(Const.ParamsNames.ENTITY_TYPE);

        if (!(entityType.equals(Const.EntityType.STUDENT) || entityType.equals(Const.EntityType.INSTRUCTOR))) {
            throw new UnauthorizedAccessException("entity type not supported.");
        }

        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);

        if (entityType.equals(Const.EntityType.STUDENT)) {
            if (!userInfo.isStudent) {
                throw new UnauthorizedAccessException("User " + userInfo.getId()
                        + " does not have student privileges");
            }

            if (courseId != null) {
                if (isCourseMigrated(courseId)) {
                    Course course = sqlLogic.getCourse(courseId);
                    gateKeeper.verifyAccessible(sqlLogic.getStudentByGoogleId(courseId, userInfo.getId()), course);
                } else {
                    CourseAttributes courseAttributes = logic.getCourse(courseId);
                    gateKeeper.verifyAccessible(logic.getStudentForGoogleId(courseId, userInfo.getId()), courseAttributes);
                }
            }
        } else {
            if (!userInfo.isInstructor) {
                throw new UnauthorizedAccessException("User " + userInfo.getId()
                        + " does not have instructor privileges");
            }

            if (courseId != null) {
                if (isCourseMigrated(courseId)) {
                    Course course = sqlLogic.getCourse(courseId);
                    gateKeeper.verifyAccessible(sqlLogic.getInstructorByGoogleId(courseId, userInfo.getId()), course);
                } else {
                    CourseAttributes courseAttributes = logic.getCourse(courseId);
                    gateKeeper.verifyAccessible(
                            logic.getInstructorForGoogleId(courseId, userInfo.getId()), courseAttributes);
                }
            }
        }
    }

    @Override
    public JsonResult execute() {
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        String entityType = getNonNullRequestParamValue(Const.ParamsNames.ENTITY_TYPE);

        // TODO: revisit this for when courses are migrated, this check is not needed as all accounts are migrated
        // if (isAccountMigrated(userInfo.getId())) {
        //     List<FeedbackSession> feedbackSessions = new ArrayList<>();
        //     List<Instructor> instructors = new ArrayList<>();
        //     List<FeedbackSessionAttributes> feedbackSessionAttributes = new ArrayList<>();
        //     List<String> studentEmails = new ArrayList<>();

        //     if (courseId == null) {
        //         if (entityType.equals(Const.EntityType.STUDENT)) {
        //             List<Student> students = sqlLogic.getStudentsByGoogleId(userInfo.getId());
        //             feedbackSessions = new ArrayList<>();
        //             for (Student student : students) {
        //                 String studentCourseId = student.getCourse().getId();
        //                 String emailAddress = student.getEmail();

        //                 studentEmails.add(emailAddress);
        //                 if (isCourseMigrated(studentCourseId)) {
        //                     List<FeedbackSession> sessions = sqlLogic.getFeedbackSessionsForCourse(studentCourseId);

        //                     feedbackSessions.addAll(sessions);
        //                 } else {
        //     List<FeedbackSessionAttributes> sessions = logic.getFeedbackSessionsForCourse(studentCourseId);

        //                     feedbackSessionAttributes.addAll(sessions);
        //                 }
        //             }
        //         } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
        //             boolean isInRecycleBin = getBooleanRequestParamValue(Const.ParamsNames.IS_IN_RECYCLE_BIN);

        //             instructors = sqlLogic.getInstructorsForGoogleId(userInfo.getId());

        //             if (isInRecycleBin) {
        //                 feedbackSessions = sqlLogic.getSoftDeletedFeedbackSessionsForInstructors(instructors);
        //             } else {
        //                 feedbackSessions = sqlLogic.getFeedbackSessionsForInstructors(instructors);
        //             }
        //         }
        //     } else {
        //         if (isCourseMigrated(courseId)) {
        //             feedbackSessions = sqlLogic.getFeedbackSessionsForCourse(courseId);
        //             if (entityType.equals(Const.EntityType.STUDENT) && !feedbackSessions.isEmpty()) {
        //                 Student student = sqlLogic.getStudentByGoogleId(courseId, userInfo.getId());
        //                 assert student != null;
        //                 String emailAddress = student.getEmail();

        //                 studentEmails.add(emailAddress);
        //             } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
        //                 instructors = Collections.singletonList(
        //                         sqlLogic.getInstructorByGoogleId(courseId, userInfo.getId()));
        //             }
        //         } else {
        //             feedbackSessionAttributes = logic.getFeedbackSessionsForCourse(courseId);
        //             if (entityType.equals(Const.EntityType.STUDENT) && !feedbackSessionAttributes.isEmpty()) {
        //                 Student student = sqlLogic.getStudentByGoogleId(courseId, userInfo.getId());
        //                 assert student != null;
        //                 String emailAddress = student.getEmail();
        //                 feedbackSessionAttributes = feedbackSessionAttributes.stream()
        //                         .map(instructorSession -> instructorSession.getCopyForStudent(emailAddress))
        //                         .collect(Collectors.toList());
        //             } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
        //                 instructors = Collections.singletonList(
        //                         sqlLogic.getInstructorByGoogleId(courseId, userInfo.getId()));
        //             }
        //         }
        //     }

        //     if (entityType.equals(Const.EntityType.STUDENT)) {
        //         // hide session not visible to student
        //         feedbackSessions = feedbackSessions.stream()
        //                 .filter(FeedbackSession::isVisible).collect(Collectors.toList());
        //         feedbackSessionAttributes = feedbackSessionAttributes.stream()
        //                 .filter(FeedbackSessionAttributes::isVisible).collect(Collectors.toList());
        //     }

        //     Map<String, Instructor> courseIdToInstructor = new HashMap<>();
        //     instructors.forEach(instructor -> courseIdToInstructor.put(instructor.getCourseId(), instructor));

        //     FeedbackSessionsData responseData =
        //             new FeedbackSessionsData(feedbackSessions, feedbackSessionAttributes);

        //     for (String studentEmail : studentEmails) {
        //         responseData.hideInformationForStudent(studentEmail);
        //     }

        //     if (entityType.equals(Const.EntityType.STUDENT)) {
        //         responseData.getFeedbackSessions().forEach(FeedbackSessionData::hideInformationForStudent);
        //     } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
        //         responseData.getFeedbackSessions().forEach(session -> {
        //             Instructor instructor = courseIdToInstructor.get(session.getCourseId());
        //             if (instructor == null) {
        //                 return;
        //             }

        //             InstructorPermissionSet privilege =
        //                     constructInstructorPrivileges(instructor, session.getFeedbackSessionName());
        //             session.setPrivileges(privilege);
        //         });
        //     }
        //     return new JsonResult(responseData);
        // } else {
        return executeOldFeedbackSession(courseId, entityType);
        // }
    }

    private JsonResult executeOldFeedbackSession(String courseId, String entityType) {
        List<FeedbackSessionAttributes> feedbackSessionAttributes;
        List<InstructorAttributes> instructors = new ArrayList<>();

        if (courseId == null) {
            if (entityType.equals(Const.EntityType.STUDENT)) {
                List<StudentAttributes> students = logic.getStudentsForGoogleId(userInfo.getId());
                feedbackSessionAttributes = new ArrayList<>();
                for (StudentAttributes student : students) {
                    String studentCourseId = student.getCourse();
                    String emailAddress = student.getEmail();
                    List<FeedbackSessionAttributes> sessions = logic.getFeedbackSessionsForCourse(studentCourseId);

                    sessions = sessions.stream()
                            .map(session -> session.getCopyForStudent(emailAddress))
                            .collect(Collectors.toList());

                    feedbackSessionAttributes.addAll(sessions);
                }
            } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
                boolean isInRecycleBin = getBooleanRequestParamValue(Const.ParamsNames.IS_IN_RECYCLE_BIN);

                instructors = logic.getInstructorsForGoogleId(userInfo.getId(), true);

                if (isInRecycleBin) {
                    feedbackSessionAttributes = logic.getSoftDeletedFeedbackSessionsListForInstructors(instructors);
                } else {
                    feedbackSessionAttributes = logic.getFeedbackSessionsListForInstructor(instructors);
                }
            } else {
                feedbackSessionAttributes = new ArrayList<>();
            }
        } else {
            feedbackSessionAttributes = logic.getFeedbackSessionsForCourse(courseId);
            if (entityType.equals(Const.EntityType.STUDENT) && !feedbackSessionAttributes.isEmpty()) {
                StudentAttributes student = logic.getStudentForGoogleId(courseId, userInfo.getId());
                assert student != null;
                String emailAddress = student.getEmail();
                feedbackSessionAttributes = feedbackSessionAttributes.stream()
                        .map(instructorSession -> instructorSession.getCopyForStudent(emailAddress))
                        .collect(Collectors.toList());
            } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
                instructors = Collections.singletonList(logic.getInstructorForGoogleId(courseId, userInfo.getId()));
            }
        }

        if (entityType.equals(Const.EntityType.STUDENT)) {
            // hide session not visible to student
            feedbackSessionAttributes = feedbackSessionAttributes.stream()
                    .filter(FeedbackSessionAttributes::isVisible).collect(Collectors.toList());
        }

        Map<String, InstructorAttributes> courseIdToInstructor = new HashMap<>();
        instructors.forEach(instructor -> courseIdToInstructor.put(instructor.getCourseId(), instructor));

        FeedbackSessionsData responseData = new FeedbackSessionsData(feedbackSessionAttributes);
        if (entityType.equals(Const.EntityType.STUDENT)) {
            responseData.getFeedbackSessions().forEach(FeedbackSessionData::hideInformationForStudent);
        } else if (entityType.equals(Const.EntityType.INSTRUCTOR)) {
            responseData.getFeedbackSessions().forEach(session -> {
                InstructorAttributes instructor = courseIdToInstructor.get(session.getCourseId());
                if (instructor == null) {
                    return;
                }

                InstructorPermissionSet privilege =
                        constructInstructorPrivileges(instructor, session.getFeedbackSessionName());
                session.setPrivileges(privilege);
            });
        }
        return new JsonResult(responseData);
    }

}
