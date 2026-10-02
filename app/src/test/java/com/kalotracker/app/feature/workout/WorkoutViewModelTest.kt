package com.kalotracker.app.feature.workout

import com.kalotracker.app.core.data.repository.WorkoutRepository
import com.kalotracker.app.core.database.dao.WorkoutDao
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private fun past(name: String, kg: Float) = listOf(ExerciseSetEntity(
        workoutId = "past", exerciseName = name, setNumber = 1, weightKg = kg, reps = 8
    ))

    @Test fun oldHistoryCannotOverwriteNewExerciseOrEditedSets() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeWorkoutDao()
            val vm = WorkoutViewModel(WorkoutRepository(dao, dispatcher))
            runCurrent()
            vm.selectExercise("Deadlift")
            runCurrent()
            assertEquals("", vm.uiState.value.sets[0].weightKg)
            dao.histories.getValue("Bench Press").complete(past("Bench Press", 60f))
            runCurrent()
            assertEquals("Deadlift", vm.uiState.value.exerciseName)
            assertEquals("", vm.uiState.value.sets[0].weightKg)
            vm.updateSet(0, "30", "5")
            dao.histories.getValue("Deadlift").complete(past("Deadlift", 100f))
            runCurrent()
            assertEquals("30", vm.uiState.value.sets[0].weightKg)
            assertFalse(vm.uiState.value.previousSessionFound)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun selectedExerciseUsesItsOwnHistory() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeWorkoutDao()
            val vm = WorkoutViewModel(WorkoutRepository(dao, dispatcher))
            runCurrent()
            dao.histories.getValue("Bench Press").complete(past("Bench Press", 60f))
            runCurrent()
            vm.selectExercise("Deadlift")
            assertEquals("", vm.uiState.value.sets[0].weightKg)
            runCurrent()
            dao.histories.getValue("Deadlift").complete(past("Deadlift", 100f))
            runCurrent()
            assertEquals("100.0", vm.uiState.value.sets[0].weightKg)
            assertTrue(vm.uiState.value.previousSessionFound)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun invalidInputCannotWriteAndFailedSaveCanBeRetriedOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeWorkoutDao()
            val vm = WorkoutViewModel(WorkoutRepository(dao, dispatcher))
            runCurrent()
            dao.histories.getValue("Bench Press").complete(emptyList())
            runCurrent()
            var navigations = 0
            vm.saveWorkout { navigations++ }
            runCurrent()
            assertEquals(0, dao.writes)
            assertNotNull(vm.uiState.value.errorMessage)
            vm.toggleCardio(true)
            dao.failWrites = true
            vm.saveWorkout { navigations++ }
            runCurrent()
            assertEquals(0, navigations)
            assertFalse(vm.uiState.value.isSaving)
            assertFalse(vm.uiState.value.isSaved)
            dao.failWrites = false
            repeat(3) { vm.saveWorkout { navigations++ } }
            runCurrent()
            assertEquals(2, dao.writes) // failed attempt + successful retry
            assertEquals(1, navigations)
            assertTrue(dao.savedSets.isEmpty()) // cardio must not persist hidden strength defaults
        } finally { Dispatchers.resetMain() }
    }

    @Test fun multiExerciseSaveKeepsCompletionAndAggregatesOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeWorkoutDao()
            val vm = WorkoutViewModel(WorkoutRepository(dao, dispatcher))
            runCurrent()
            dao.histories.getValue("Bench Press").complete(emptyList())
            runCurrent()
            vm.updateSet(0, "40", "8")
            vm.toggleSetComplete(0, false)
            vm.updateDuration("20")
            vm.updateCalories("100")
            vm.addExerciseToSession()
            vm.updateExerciseName("Run")
            vm.toggleCardio(true)
            vm.updateDuration("15")
            vm.updateCalories("150")
            vm.setSessionTitle("Evening")
            vm.saveWorkout {}
            runCurrent()
            assertEquals(1, dao.writes)
            assertEquals(35, dao.savedWorkout!!.durationMinutes)
            assertEquals(250, dao.savedWorkout!!.estimatedCaloriesBurned)
            assertEquals("Evening", dao.savedWorkout!!.title)
            assertEquals(2, validateRoutinePayload(dao.savedWorkout!!.exercisesJson).size)
            assertEquals(1, dao.savedSets.size)
            assertFalse(dao.savedSets.single().isCompleted)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedEditLoadCannotOverwriteWithDefaultDraft() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = FakeWorkoutDao()
            val vm = WorkoutViewModel(WorkoutRepository(dao, dispatcher), workoutId = "missing")
            runCurrent()
            assertNotNull(vm.uiState.value.errorMessage)
            vm.toggleCardio(true)
            vm.saveWorkout {}
            runCurrent()
            assertEquals(0, dao.writes)
            assertFalse(vm.uiState.value.isSaved)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun quickFitnessPersistsAdjustedRepsAndTotalTimeOnceWithDuplicateTapGuard() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val dao = FakeWorkoutDao()
        val vm = WorkoutViewModel(WorkoutRepository(dao, dispatcher))
        val holder = androidx.lifecycle.ViewModelStore().apply { put("workout", vm) }
        try {
            runCurrent()
            var saved = 0
            repeat(3) { vm.saveDailyFitness("7", "70", FitnessEffort.EASY, listOf("20", "0", "15")) { saved++ } }
            runCurrent()
            assertEquals(1, saved)
            assertEquals(1, dao.writes)
            assertEquals(7, dao.savedWorkout!!.durationMinutes)
            assertEquals("Daily fitness", dao.savedWorkout!!.title)
            assertEquals(listOf(20, 15), dao.savedSets.map { it.reps })
            assertTrue(dao.savedSets.all { it.weightKg == 0f })
        } finally { holder.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    private class FakeWorkoutDao : WorkoutDao {
        val histories = mutableMapOf<String, CompletableDeferred<List<ExerciseSetEntity>>>()
        var savedWorkout: WorkoutEntity? = null
        var writes = 0
        var failWrites = false
        var savedSets = emptyList<ExerciseSetEntity>()
        override suspend fun getLastSessionSetsForExercise(exerciseName: String) =
            histories.getOrPut(exerciseName) { CompletableDeferred() }.await()
        override suspend fun insertWorkoutWithSets(workout: WorkoutEntity, sets: List<ExerciseSetEntity>) {
            writes++
            if (failWrites) throw java.io.IOException("storage unavailable")
            savedWorkout = workout
            savedSets = sets
        }
        override suspend fun getWorkoutById(id: String): WorkoutWithSets? = null
        override fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long) = flowOf(emptyList<WorkoutWithSets>())
        override suspend fun getWorkoutsBetween(start: Long, end: Long) = emptyList<WorkoutWithSets>()
        override suspend fun getAllWorkouts() = emptyList<WorkoutWithSets>()
        override suspend fun insertWorkout(workout: WorkoutEntity) = Unit
        override suspend fun insertWorkouts(workouts: List<WorkoutEntity>) = Unit
        override suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>) = Unit
        override suspend fun deleteWorkout(workout: WorkoutEntity) = Unit
        override suspend fun deleteExerciseSetsByWorkoutId(workoutId: String) = Unit
        override suspend fun deleteAllExerciseSets() = Unit
        override suspend fun deleteAllWorkouts() = Unit
    }
}
