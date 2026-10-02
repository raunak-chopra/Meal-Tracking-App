package com.kalotracker.app.core.data.backup

import com.kalotracker.app.core.database.entity.*
import com.kalotracker.app.core.data.food.PersonalFood
import com.kalotracker.app.feature.workout.WorkoutExercise
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.zip.*

class ArchiveCodecTest {
    @Test fun archiveRoundTripPreservesPhotosAndLibrary() {
        val photo=byteArrayOf(1,2,3,4)
        val meal=BackupMeal("m","Rice",200,4f,45f,0.5f,timestamp=1,photoEntry=ArchiveCodec.photoName("m"))
        val backup=BackupFile(meals=listOf(meal),savedFoods=listOf(com.kalotracker.app.core.database.entity.SavedFoodEntity(name="Rice",calories=200,protein=4f,carbs=45f,fat=0.5f)))
        val out=ByteArrayOutputStream();ArchiveCodec.write(out,backup){photo}
        val restored=ArchiveCodec.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(backup,restored.backup);assertArrayEquals(photo,restored.photos.getValue(meal.photoEntry!!))
    }
    @Test(expected=BackupFormatException::class) fun rejectsPathTraversalBeforeExtracting() {
        val out=ByteArrayOutputStream();ZipOutputStream(out).use { it.putNextEntry(ZipEntry("../escape.jpg"));it.write(1);it.closeEntry() }
        ArchiveCodec.read(ByteArrayInputStream(out.toByteArray()))
    }
    @Test(expected=BackupFormatException::class) fun rejectsMissingManifestPhoto() {
        val b=BackupFile(meals=listOf(BackupMeal("m","Rice",200,4f,45f,0.5f,timestamp=1,photoEntry=ArchiveCodec.photoName("m"))))
        val out=ByteArrayOutputStream();ZipOutputStream(out).use { it.putNextEntry(ZipEntry("backup.json"));it.write(BackupCodec.encode(b).toByteArray());it.closeEntry() }
        ArchiveCodec.read(ByteArrayInputStream(out.toByteArray()))
    }
    @Test fun allNewRecordsAndPreferencesRoundTrip() {
        val payload = PersonalFood.json.encodeToString(listOf(WorkoutExercise("Run", true, durationMinutes=20, calories=100)))
        val product = com.kalotracker.app.core.network.ScannedFoodProduct("123", "Label", caloriesPer100g=120)
        val b = BackupFile(dayStatus=listOf(DayStatusEntity("2026-10-01",true)),
            goalHistory=listOf(GoalHistoryEntity("2026-10-01",2200,160,220,70,2500,"MAINTAIN")),
            savedFoods=listOf(SavedFoodEntity(name="Rice",calories=200,protein=4f,carbs=45f,fat=1f,favorite=true)),
            barcodes=listOf(BarcodeCacheEntity("123",PersonalFood.json.encodeToString(product),1)),
            routines=listOf(WorkoutRoutineEntity(name="Run",payload=payload)),
            workouts=listOf(BackupWorkout("w","Run","CARDIO",20,100,1,exercisesJson=payload)),
            preferences=BackupPreferences("gemini-2.0-flash",true,21,30))
        assertEquals(b,BackupCodec.decode(BackupCodec.encode(b)))
        assertFalse(BackupCodec.encode(b).contains("apiKey"))
    }
    @Test(expected=BackupFormatException::class) fun invalidRoutineRejected() {
        BackupCodec.decode(BackupCodec.encode(BackupFile(routines=listOf(WorkoutRoutineEntity(name="Bad",payload="[]")))))
    }
    @Test fun versionOneBackupsRemainReadable() {
        assertEquals(1,BackupCodec.decode("""{"app":"kalo","version":1,"meals":[]}""").version)
    }
}
