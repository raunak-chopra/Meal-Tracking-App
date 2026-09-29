package com.kalotracker.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Runs the real "last session" query from WorkoutDao (extracted from the source so it cannot drift)
 * against SQLite. The old version ordered by the random UUID and returned arbitrary sets.
 */
class WorkoutQueryTest {

    private fun lastSessionSql(): String {
        val source = File("src/main/java/com/kalotracker/app/core/database/dao/WorkoutDao.kt").readText()
        val marker = "suspend fun getLastSessionSetsForExercise"
        val before = source.substring(0, source.indexOf(marker))
        val start = before.lastIndexOf("\"\"\"", before.lastIndexOf("\"\"\"") - 1) + 3
        val end = before.lastIndexOf("\"\"\"")
        return source.substring(start, end).replace(":exerciseName", "?")
    }

    @Test
    fun returnsSetsFromTheMostRecentWorkoutNotTheLargestId() {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { st ->
            st.execute("CREATE TABLE workouts (id TEXT PRIMARY KEY, title TEXT, type TEXT, durationMinutes INTEGER, estimatedCaloriesBurned INTEGER, timestamp INTEGER)")
            st.execute("CREATE TABLE exercise_sets (id TEXT PRIMARY KEY, workoutId TEXT, exerciseName TEXT, setNumber INTEGER, weightKg REAL, reps INTEGER, isCompleted INTEGER)")
            // The OLDER workout gets the lexicographically LARGER ids: the old query would have picked it.
            st.execute("INSERT INTO workouts VALUES ('zzz-old','Bench','STRENGTH',30,100,1000)")
            st.execute("INSERT INTO workouts VALUES ('aaa-new','Bench','STRENGTH',30,100,2000)")
            st.execute("INSERT INTO workouts VALUES ('mmm-other','Squat','STRENGTH',30,100,3000)")
            st.execute("INSERT INTO exercise_sets VALUES ('zzz-2','zzz-old','Bench',2,50,8,1)")
            st.execute("INSERT INTO exercise_sets VALUES ('zzz-1','zzz-old','Bench',1,50,10,1)")
            st.execute("INSERT INTO exercise_sets VALUES ('aaa-1','aaa-new','bench',1,60,10,1)")
            st.execute("INSERT INTO exercise_sets VALUES ('aaa-2','aaa-new','bench',2,60,9,1)")
            st.execute("INSERT INTO exercise_sets VALUES ('mmm-1','mmm-other','Squat',1,100,5,1)")
        }

        conn.prepareStatement(lastSessionSql()).use { ps ->
            ps.setString(1, "Bench") // also matches "bench" (case-insensitive)
            ps.setString(2, "Bench")
            val reps = ps.executeQuery().use { rs ->
                buildList { while (rs.next()) add("${rs.getDouble("weightKg")}x${rs.getInt("reps")}#${rs.getInt("setNumber")}") }
            }
            assertEquals(listOf("60.0x10#1", "60.0x9#2"), reps) // newest session, in set order
        }
    }
}
