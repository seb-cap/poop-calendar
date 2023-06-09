package com.example.poopcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poopcalendar.ui.theme.PoopCalendarTheme
import java.io.File
import java.io.FileWriter
import java.util.*


val all_poops: MutableList<MutableList<MutableList<Int>>> = mutableStateListOf()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val poop = File(this.filesDir, "main.poop")
        poop.createNewFile()
        val poopScan = Scanner(poop)
        if (!poopScan.hasNext()) {
            initializePoops(poop)
        }
        setupPoopList(poopScan)
        setContent {
            PoopCalendarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.DarkGray
                ) {
                    Calendar { save() }
                }
            }
        }
    }

    private fun save() {
        val poops = File(this.filesDir, "main.poop")
        var str = ""

        all_poops.forEach { year ->
            year.forEach { month ->
                month.forEach { day ->
                    str += if (day < 0 || day > 9) "n " else "$day "
                }
            }
        }
        poops.writeText(str)
    }
}

private fun initializePoops(poops: File) {
    val wr = FileWriter(poops)
    for (i in 0..29219) {
        wr.write("n ")
    }
}

private fun setupPoopList(poop: Scanner) {
    for (yr in 2000..2099) {
        all_poops.add(mutableListOf())
        for (mo in 1..12) {
            all_poops[yr-2000].add(mutableListOf())
            for (d in 1..datesInMonth(mo, yr)) {
                if (!poop.hasNext()) return
                val next = poop.next()
                val num = if (next == "n") -1 else Integer.parseInt(next)

                all_poops[yr-2000][mo-1].add(num)
            }
        }
    }
}

@Composable
fun Calendar(save: () -> Unit) {
    var month by rememberSaveable {mutableStateOf(1)}
    var year by rememberSaveable {mutableStateOf(2020)}
    var day by rememberSaveable {mutableStateOf(1)}
    var jump by rememberSaveable {mutableStateOf(0)}
    var ping by remember {mutableStateOf(true)}
    Column (
        modifier = Modifier.fillMaxSize()
    ) {
        Row (
            modifier = Modifier.weight(0.2f).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                when (jump) {
                    0 -> {
                        month--
                        day = 1
                        if (month < 1) {
                            if (year > 2020) {
                                month = 12
                                year--
                            } else {
                                month++
                            }
                        }
                    }
                    1 -> {
                        year--
                        day = 1
                        if (year < 2020) year = 2020
                    }
                }
            } ) {Text(text = "Previous")}

            Text(text = monthFromInt(month) + " " + year.toString(), textAlign = TextAlign.Center, color = Color.White,
                modifier = Modifier.clickable {
                    jump++
                    if (jump > 1) jump = 0
            })
            Button(onClick = {
                when (jump) {
                    0 -> {
                        month++
                        day = 1
                        if (month > 12) {
                            if (year < 2099) {
                                month = 1
                                year++
                            } else {
                                month--
                            }
                        }
                    }
                    1 -> {
                        year++
                        if (year > 2099) year = 2099
                    }
                }
            } ) {Text(text = "Next")}
        }

        Column (Modifier.weight(0.6f)) {
            val startDay = 1 - computeStartingDay(month, year)
            for (wk in 0..5) {
                Week(startDay + wk * 7, month, year, ping,
                    {
                        if (day > 0 && day < datesInMonth(month, year)) day = it
                    },modifier = Modifier.weight(0.2f))
            }
        }

        EditData(day, month, year, {ping = !ping}, save, modifier = Modifier.weight(0.2f))
    }
}

@Composable
fun EditData(day: Int, month: Int, year: Int, ping: () -> Unit, save: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize())
    {
        Row (modifier = modifier.weight(0.5f).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            var daysPoops by remember {mutableStateOf(-1)}
            daysPoops = all_poops[year-2020][month-1][day-1]

            Text(text = monthFromInt(month) + " " + day + ", " + year, color = Color.White)

            Button(onClick = {
                daysPoops--
                if (daysPoops < -1) daysPoops = -1
                all_poops[year-2020][month-1][day-1] = daysPoops
                ping()
            }) { Text(text = " - ")}

            Text(text = if (daysPoops == -1) "N/A" else daysPoops.toString(), color = Color.White)

            Button(onClick = {
                daysPoops++
                if (daysPoops > 9) daysPoops = 9
                all_poops[year-2020][month-1][day-1] = daysPoops
                ping()
            }) { Text(text = " + ")}
        }

        Button(onClick = save, modifier = modifier.weight(0.5f).fillMaxSize()) {
            Text(text = "Save Calendar")
        }
    }
}

fun computeStartingDay(month: Int, year: Int): Int {
    return (findYearCode(year) + findMonthCode(month, year) + findCenturyCode(year) + 1) % 7
}

fun findYearCode(year: Int): Int {
    val lastTwoDigits = year % 100
    return (lastTwoDigits / 4 + lastTwoDigits) % 7
}

fun findCenturyCode(year: Int): Int {
    return when (year / 100) {
        17 -> 0
        18 -> 5
        19 -> 3
        20 -> 0
        21 -> 5
        22 -> 3
        23 -> 1
        24 -> 6
        else -> throw IllegalStateException("Time travel is not okay.")
    }
}

fun findMonthCode(month: Int, year: Int): Int {
    return when (month) {
        1 -> if (isLeapYear(year)) 5 else 6
        2 -> if (isLeapYear(year)) 1 else 2
        3 -> 2
        4 -> 5
        5 -> 0
        6 -> 3
        7 -> 5
        8 -> 1
        9 -> 4
        10 -> 6
        11 -> 2
        12 -> 4
        else -> throw IllegalArgumentException("Invalid month")
    }
}

fun monthFromInt(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Invalid"
    }
}

@Composable
fun Week(startDay: Int, month: Int, year: Int, ping: Boolean, setDay: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row (
        modifier = modifier.fillMaxWidth()
    ) {
        for (i in startDay..startDay + 6) {
            Day(i, getPoops(i, month, year), month, year, ping, setDay, modifier.fillMaxSize())
        }
    }
}

private fun getPoops(day: Int, month: Int, year: Int): Int {
    if (day < 1 || day > datesInMonth(month, year)) return 0
    return all_poops[year - 2020][month - 1][day - 1]
}

@Composable
fun Day(number: Int, poops: Int, month: Int, year: Int, ping: Boolean, setDay: (Int) -> Unit, modifier: Modifier = Modifier) {
    val poopsColor = when (poops) {
        -1 -> Color.White
        0 -> Color(105, 240, 245)
        1 -> Color(255, 165, 0)
        2 -> Color(255, 120, 0)
        3 -> Color.Red
        else -> Color.Magenta
    }
    val date = if (number > 0 && number <= datesInMonth(month, year)) number.toString() else ""
    Box (
        modifier = modifier
            .background(color = poopsColor)
            .border(BorderStroke(1.dp, Color.Black))
            .clickable {setDay(number)}
    ) {
        Text(text = date, color = Color.Black, modifier = Modifier.padding(3.dp))
    }
}

fun datesInMonth(month: Int, year: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }
}

fun isLeapYear(year: Int): Boolean {
    return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
}