package com.rockchip.llm.translator

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rockchip.llm.translator.ui.theme.LLM_TranslatorTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val TAG = "LLMTranslate"
    private var mLLmTranslator : LLmTranslator? = null
    private val mHandler = Handler(Looper.getMainLooper())

    companion object {
        var inputLang = mutableStateOf("英文")
        var inputText = mutableStateOf("Toybrick是完全开源的嵌入式开发平台")
        var outputThinkText = mutableStateOf("")
        var outputResultText = mutableStateOf("")
        var isOutputing = mutableStateOf(false)
        val scrollState1 = mutableStateOf(ScrollState(0))
        val scrollState2 = mutableStateOf(ScrollState(0))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modelpath = copyFileIfNeeded(this, "deepseek-r1.rkllm")

        mLLmTranslator = LLmTranslator(modelpath, object : LLmTranslatorCallback {
            override fun onThinking(msg: String, finished: Boolean) {
                mHandler.post({
                    outputThinkText.value += msg
                })
                isOutputing.value = !finished
                scoreToEnd(true)
            }

            override fun onResult(msg: String, finished: Boolean) {
                mHandler.post({
                    outputResultText.value += msg
                })
                isOutputing.value = !finished
                scoreToEnd(false)
            }
        })

        enableEdgeToEdge()
        setContent {
            LLM_TranslatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(onclick = ::onClick, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLLmTranslator?.destroy()
    }

    fun copyFileIfNeeded(context: Context, fileName: String) : String {
        val modelpath = "${context.filesDir}/$fileName"
        if (File(modelpath).exists()) {
            return modelpath
        }

        val inputStream: InputStream = context.assets.open(fileName)
        val outputStream: OutputStream = FileOutputStream(modelpath)
        Log.d(TAG, "Copying model to file ...")
        inputStream.copyTo(outputStream)
        Log.d(TAG, "Copying model to file ... finished")
        inputStream.close()
        outputStream.close()
        return modelpath
    }

    fun onClick(input: String)  {
        outputThinkText.value = ""
        outputResultText.value = ""

        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit {
            isOutputing.value = true
            mLLmTranslator!!.translate(inputText.value, inputLang.value)
        }
    }

    fun scoreToEnd(isThinkText : Boolean) {
        runBlocking {
            launch {
                if (isThinkText)
                    scrollState1.value.scrollTo(scrollState1.value.maxValue)
                else
                    scrollState2.value.scrollTo(scrollState2.value.maxValue)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(onclick: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("英文", "日语", "韩语", "西班牙语", "法语", "德语", "俄罗斯语")
    Column(modifier = modifier) {
        OutlinedTextField(
            value = MainActivity.inputText.value,
            onValueChange = {MainActivity.inputText.value = it},
            label = { Text("请输入") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onclick(MainActivity.inputText.value) },
                enabled = !MainActivity.isOutputing.value
            ) {
                Text("翻译")
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(text = MainActivity.inputLang.value)
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        onClick = {
                            MainActivity.inputLang.value = options[index]
                        },
                        text = { Text(label) }
                    )
                }
            }
        }
        OutlinedTextField(
            value = MainActivity.outputThinkText.value,
            onValueChange = {},
            label = { Text("思考") },
            modifier = Modifier.fillMaxWidth().weight(3f).verticalScroll(MainActivity.scrollState1.value),
            readOnly = true,
        )
        OutlinedTextField(
            value = MainActivity.outputResultText.value,
            onValueChange = {},
            label = { Text("结果") },
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(MainActivity.scrollState2.value),
            readOnly = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LLM_TranslatorTheme {
        Greeting(onclick = {})
    }
}