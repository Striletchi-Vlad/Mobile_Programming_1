package com.example.myapplication4

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

const val serverURL = "http://192.168.100.132:5000"

fun sendGetRequest(urlString: String): String {
    // send get request to server
    val url = URL(urlString)
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = "GET"
    val responseCode = con.responseCode
    // println("GET Response Code :: $responseCode")
    if (responseCode == HttpURLConnection.HTTP_OK) { // success
        val response = StringBuffer()
        val inReader = BufferedReader(InputStreamReader(con.inputStream))
        var inputLine: String?
        while (inReader.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        inReader.close()
        // print result
        // println(response.toString())
        return response.toString()
    } else {
        // println("GET request failed")
    }
    return ""
}

fun sendPostRequest(urlString: String, json_payload: String): String {
    // send post request to server
    val url = URL(urlString)
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = "POST"
    con.setRequestProperty("Content-Type", "application/json; utf-8")
    con.setRequestProperty("Accept", "application/json")
    con.doOutput = true
    val os = con.outputStream
    val input = json_payload.toByteArray(Charsets.UTF_8)
    os.write(input, 0, input.size)
    os.flush()
    os.close()
    val responseCode = con.responseCode
    // println("POST Response Code :: $responseCode")
    if (responseCode == HttpURLConnection.HTTP_OK) { //success
        val response = StringBuffer()
        val inReader = BufferedReader(InputStreamReader(con.inputStream))
        var inputLine: String?
        while (inReader.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        inReader.close()
        // print result
        // println(response.toString())
        return response.toString()
    } else {
        // println("POST request failed")
    }
    return ""
}

fun isInternetAvailable(context: Context): Boolean {
    // return true if internet is available
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        ?: return false
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

fun sendCreateRequest(recording: RecordingEntity): Boolean {
    val json_payload = """
             {
                 "id": "${recording.id}",
                 "name": "${recording.name}",
                 "description": "${recording.desc}"
             }
         """.trimIndent()

    val response = sendPostRequest("$serverURL/recordings", json_payload)
    if (response == "") {
        return false
    }
    return true
}
