package com.example.myapplication4

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.*
import android.os.Bundle
import android.os.StrictMode
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.myapplication4.ui.theme.MyApplicationTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


import java.util.UUID;
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import org.json.JSONArray


class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mydb"
        ).allowMainThreadQueries().build()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // allow network access on main thread
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                val navController = rememberNavController()
                val viewModel = viewModel { RecordingListViewModel(db.recordingDao()) }
                if (!isInternetAvailable(this)) {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                }
                else{
                    viewModel.pushLocalRecordings()
                    viewModel.pullRemoteRecordings()
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "home", builder = {
                        composable("home"){
                            RecordingList(navController, viewModel, this@MainActivity)
                        }
                        composable("edit"){
                            EditRecording(navController, viewModel)
                        }
                        composable("create"){
                            CreateRecording(navController, viewModel)
                        }
                        composable("delete"){
                            DeleteRecording(navController, viewModel)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun SingleRecording(id: String, name: String, navController: NavController, viewModel: RecordingListViewModel , context: Context){
    Row() {
        Text( text = name)
        Button(onClick = {}) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
        }
        Button(onClick = {
            if (!isInternetAvailable(context)) {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            else{
                viewModel.selectedid = id
                navController.navigate("edit")
            }
        }) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
        }
        Button(onClick = {
            if (!isInternetAvailable(context)) {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            else{
                viewModel.selectedid = id
                navController.navigate("delete")
            }
        }) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
        }
    }
}
// Define your composable component
@Composable
fun RecordingList(navController: NavController, viewModel: RecordingListViewModel, context: Context){
    if (!isInternetAvailable(context)) {
        //Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
    }
    else{
        viewModel.pushLocalRecordings()
        viewModel.pullRemoteRecordings()
    }
    Column() {
        Text(text = "Recordings")
        viewModel.getAllRecordings().forEach {
            SingleRecording(it.id, it.name, navController, viewModel, context)
        }
        Button(onClick = {
            navController.navigate("create")
        }) {
            Icon(Icons.Rounded.Add, contentDescription = "Add")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecording(navController: NavController, viewModel: RecordingListViewModel){
    val recording = viewModel.getRecording(viewModel.selectedid!!)
    var name by remember { mutableStateOf(recording!!.name) }
    var desc by remember { mutableStateOf(recording!!.desc) }
    Column() {
        Text(text = "Edit Recording")
        Row {
            Text(text = "Name")
            TextField(value = name, onValueChange = { name = it })
        }
        Row {
            Text(text = "Description")
            TextField(value = desc, onValueChange = { desc = it })
        }
        Row {
            Button(onClick = {navController.navigate("home")}) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Cancel")
            }
            Button(onClick = {
                navController.navigate("home")
                // viewModel.editRecording(viewModel.selectedid!!, name, desc)
                val json_payload = """
                    {
                        "id": "${viewModel.selectedid!!}",
                        "name": "$name",
                        "description": "$desc"
                    }
                """.trimIndent()
                sendPostRequest("$serverURL/recordings/update/${viewModel.selectedid!!}", json_payload)
            }) {
                Icon(Icons.Rounded.Check, contentDescription = "Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecording(navController: NavController, viewModel: RecordingListViewModel){
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    Column() {
        Text(text = "Create Recording")
        Row {
            Text(text = "Name")
            TextField(value = name, onValueChange = { name = it })
        }
        Row {
            Text(text = "Description")
            TextField(value = desc, onValueChange = { desc = it })
        }
        Row{
            Button(onClick = {navController.navigate("home")}) {
                Text(text = "Record")
            }
            Button(onClick = {
                navController.navigate("home")
            }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Cancel")
            }
            Button(onClick = {
                viewModel.addRecording(name, desc)
                navController.navigate("home")
            }) {
                Icon(Icons.Rounded.Check, contentDescription = "Save")
            }
        }
    }
}

@Composable
fun DeleteRecording(navController: NavController, viewModel: RecordingListViewModel){
    Column() {
        Text(text = "Delete Recording")
        Row {
            Button(onClick = {navController.navigate("home")}) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Cancel")
            }
            Button(onClick = {
                navController.navigate("home")
                // viewModel.deleteRecording(viewModel.selectedid!!)
                sendPostRequest("$serverURL/recordings/delete/${viewModel.selectedid!!}", "")
            }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete")
            }
        }
    }
}