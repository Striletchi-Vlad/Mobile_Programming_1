package com.example.myapplication4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val desc: String,
    var syncd: Boolean = false
)


@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings")
    fun getAll(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getById(id: String): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recording: RecordingEntity)

    @Delete
    fun delete(recording: RecordingEntity)
}

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}


class RecordingListViewModel(private val recordingDao: RecordingDao) : ViewModel() {
    var selectedid: String? = null

    fun pushLocalRecordings() {
        val unsyncdRecordings = recordingDao.getAll().filter { !it.syncd }
        unsyncdRecordings.forEach {
            if (sendCreateRequest(it)) {
                it.syncd = true;
            }
        }
    }

    fun pullRemoteRecordings() {
        // delete all local recordings
        recordingDao.getAll().forEach {
            recordingDao.delete(it)
        }
        val response = sendGetRequest("$serverURL/recordings")
        if (response == "") {
            return
        }
        // interpret response as json
        val jsonArray = JSONArray(response)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("id")
            val name = jsonObject.getString("name")
            val desc = jsonObject.getString("description")
            val recordingEntity = RecordingEntity(id, name, desc, true)
            recordingDao.insert(recordingEntity)
        }
    }

    fun addRecording(name: String, desc: String) {
        val id = UUID.randomUUID().toString()
        val recording = RecordingEntity(id, name, desc)
        viewModelScope.launch(Dispatchers.IO) {
            recordingDao.insert(recording)
        }
    }

    fun deleteRecording(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordingDao.getById(id)?.let {
                recordingDao.delete(it)
            }
        }
    }

    fun editRecording(id: String, name: String, desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordingDao.getById(id)?.let {
                val updatedRecording = it.copy(name = name, desc = desc)
                recordingDao.insert(updatedRecording)
            }
        }
    }

    fun getRecording(id: String): RecordingEntity? {
        return recordingDao.getById(id)
    }

    fun getAllRecordings(): List<RecordingEntity> {
        return recordingDao.getAll()
    }
}

