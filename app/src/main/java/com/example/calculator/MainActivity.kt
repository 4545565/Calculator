package com.example.calculator

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var inputText: TextView
    private lateinit var historyText: TextView
    private var isNewInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputText = findViewById(R.id.inputText)
        historyText = findViewById(R.id.historyText)
        
        setupNumberPad()
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun setupNumberPad() {
        val buttons = arrayOf(
            "C", "DEL", "(", ")",
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", ".", "=", "+"
        )

        findViewById<GridLayout>(R.id.buttonGrid).apply {
            forEachChild { view ->
                (view as? Button)?.apply {
                    background = ContextCompat.getDrawable(context, R.drawable.button_background)
                    setTextColor(Color.WHITE)
                    
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_scale))
                                (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(50)
                            }
                            MotionEvent.ACTION_UP -> {
                                v.clearAnimation()
                                handleButtonClick(text.toString())
                            }
                        }
                        true
                    }
                }
            }
        }
    }

    private fun handleButtonClick(value: String) {
        when {
            value == "C" -> clearAll()
            value == "DEL" -> deleteLastChar()
            value == "=" -> calculateResult()
            else -> appendInput(value)
        }
    }

    private fun appendInput(value: String) {
        if (isNewInput) clearAll()
        
        val current = inputText.text.toString()
        val newText = when {
            value == "." && current.contains(".") -> current
            value == "0" && current == "0" -> current
            current == "0" && value in "123456789" -> value
            else -> current + value
        }
        inputText.text = newText
    }

    private fun calculateResult() {
        try {
            val expression = inputText.text.toString()
            val result = evaluateExpression(expression)
            
            historyText.apply {
                text = expression
                animate().translationY(-50f).alpha(0.6f).start()
            }
            
            inputText.text = result.toString()
            isNewInput = true
        } catch (e: Exception) {
            inputText.text = "Error"
        }
    }

    private fun evaluateExpression(expr: String): Double {
        // 这里需要实现表达式计算逻辑（注意安全）
        // 建议使用第三方数学库或自己实现安全的表达式解析
        return 0.0
    }

    private fun GridLayout.forEachChild(action: (View) -> Unit) {
        for (i in 0 until childCount) action(getChildAt(i))
    }
}