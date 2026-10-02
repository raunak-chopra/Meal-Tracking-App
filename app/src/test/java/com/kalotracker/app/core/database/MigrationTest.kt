package com.kalotracker.app.core.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Validates MIGRATION_2_3 on the JVM against the exported Room schemas (2.json / 3.json):
 * data survives, and the migrated structure equals a freshly created v3 database.
 */
class MigrationTest {

    private fun schemaSql(version: Int): List<String> {
        val path = "/com.kalotracker.app.core.database.KaloDatabase/$version.json"
        val text = javaClass.getResourceAsStream(path)!!.bufferedReader().readText()
        val entities = Json.parseToJsonElement(text).jsonObject["database"]!!
            .jsonObject["entities"]!!.jsonArray
        return entities.flatMap { e ->
            val table = e.jsonObject["tableName"]!!.jsonPrimitive.content
            val create = e.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)
            val indices = e.jsonObject["indices"]?.jsonArray.orEmpty().map {
                it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)
            }
            listOf(create) + indices
        }
    }

    private fun open(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun Connection.exec(sql: String) = createStatement().use { it.execute(sql) }

    private fun Connection.count(table: String): Int =
        createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM `$table`").use { it.next(); it.getInt(1) } }

    private fun Connection.columns(table: String): List<String> =
        createStatement().use { st ->
            st.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                buildList { while (rs.next()) add("${rs.getString("name")}:${rs.getString("type")}:${rs.getInt("notnull")}:${rs.getInt("pk")}") }
            }
        }

    private fun Connection.foreignKeys(table: String): List<String> =
        createStatement().use { st ->
            st.executeQuery("PRAGMA foreign_key_list(`$table`)").use { rs ->
                buildList { while (rs.next()) add("${rs.getString("table")}:${rs.getString("from")}:${rs.getString("to")}:${rs.getString("on_delete")}") }
            }
        }

    private fun Connection.indexes(table: String): List<String> =
        createStatement().use { st ->
            st.executeQuery("PRAGMA index_list(`$table`)").use { rs ->
                buildList { while (rs.next()) if (!rs.getString("name").startsWith("sqlite_")) add(rs.getString("name")) }
            }.sorted()
        }

    private fun Connection.tables(): List<String> =
        createStatement().use { st ->
            st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { rs ->
                buildList { while (rs.next()) add(rs.getString(1)) }
            }.sorted()
        }

    @Test
    fun migration2to3_preservesDataAndMatchesSchema() {
        val old = open()
        // Worst case: enforce foreign keys during the migration.
        old.exec("PRAGMA foreign_keys=ON")
        schemaSql(2).forEach { old.exec(it) }

        old.exec("INSERT INTO meals VALUES ('m1','Oats',350,12.5,60.0,7.0,'/img/a.jpg','http://x','n',1000,'SYNCED')")
        old.exec("INSERT INTO meals VALUES ('m2','Rice',200,4.0,45.0,0.5,NULL,NULL,NULL,2000,'PENDING')")
        old.exec("INSERT INTO food_items VALUES ('f1','m1','Oats',80,300,10,50,6,0.9)")
        old.exec("INSERT INTO food_items VALUES ('f2','m1','Milk',100,50,2.5,10,1,0.8)")
        old.exec("INSERT INTO food_items VALUES ('f3','m2','Rice',150,200,4,45,0.5,1.0)")
        old.exec("INSERT INTO workouts VALUES ('w1','Push','STRENGTH',45,300,3000,'PENDING')")
        old.exec("INSERT INTO exercise_sets VALUES ('s1','w1','Bench',1,60.0,8,1)")
        old.exec("INSERT INTO exercise_sets VALUES ('s2','w1','Bench',2,60.0,7,1)")
        old.exec("INSERT INTO water_logs VALUES ('h1',250,4000)")

        Migrations.MIGRATION_2_3_SQL.forEach { old.exec(it) }

        assertEquals(2, old.count("meals"))
        assertEquals(3, old.count("food_items"))
        assertEquals(1, old.count("workouts"))
        assertEquals(2, old.count("exercise_sets"))
        assertEquals(1, old.count("water_logs"))
        assertEquals(0, old.count("weight_logs"))

        old.createStatement().use { st ->
            st.executeQuery("SELECT title, totalProteinGrams, imageLocalUri, notes, timestamp FROM meals WHERE id='m1'").use {
                assertTrue(it.next())
                assertEquals("Oats", it.getString(1))
                assertEquals(12.5, it.getDouble(2), 0.001)
                assertEquals("/img/a.jpg", it.getString(3))
                assertEquals("n", it.getString(4))
                assertEquals(1000L, it.getLong(5))
            }
        }

        // Structure must equal a database created directly at v3.
        val fresh = open()
        schemaSql(3).forEach { fresh.exec(it) }
        assertEquals(fresh.tables(), old.tables())
        for (t in fresh.tables()) {
            assertEquals("columns of $t", fresh.columns(t), old.columns(t))
            assertEquals("foreign keys of $t", fresh.foreignKeys(t), old.foreignKeys(t))
            assertEquals("indexes of $t", fresh.indexes(t), old.indexes(t))
        }

        // Cascade still works after the rebuild.
        old.exec("DELETE FROM meals WHERE id='m1'")
        assertEquals(1, old.count("food_items"))
    }
}
