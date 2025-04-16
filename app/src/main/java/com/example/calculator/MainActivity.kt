package com.example.calculator

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.TextView
import net.objecthunter.exp4j.ExpressionBuilder
import java.util.EmptyStackException
import androidx.gridlayout.widget.GridLayout
import android.view.WindowManager

class MainActivity : AppCompatActivity() {
    private lateinit var inputText: TextView
    private lateinit var historyText: TextView
    private var isNewInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 检测屏幕比例并控制旋转
        checkScreenRatioAndLockRotation()

        inputText = findViewById(R.id.inputText)
        historyText = findViewById(R.id.historyText)
        
        setupNumberPad()
        window.statusBarColor = Color.TRANSPARENT
    }

    // 检测屏幕比例并锁定旋转
    private fun checkScreenRatioAndLockRotation() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display
            display?.getRealMetrics(metrics)
        } else {
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        val screenWidth = metrics.widthPixels.coerceAtLeast(metrics.heightPixels)
        val screenHeight = metrics.widthPixels.coerceAtMost(metrics.heightPixels)
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

        // 如果屏幕比例 > 16:9（即 1.777...），则锁定为当前方向
        requestedOrientation = if (aspectRatio > 16.1f / 9f) {
            when (resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            // 恢复自动旋转
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 监听屏幕旋转事件，重新检测比例
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        checkScreenRatioAndLockRotation()
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
        when (value) {
            "C" -> clearAll()
            "DEL" -> deleteLastChar()
            "=" -> calculateResult()
            else -> appendInput(value)  // 确保此时inputText已初始化
        }
    }
    private fun appendInput(value: String) {
        runOnUiThread {
            // 空安全检查
            if (!::inputText.isInitialized) return@runOnUiThread

            val current = inputText.text?.toString() ?: ""  // 处理可能的空文本
            val newText = when {
                value == "." && current.contains(".") -> current
                value == "0" && current == "0" -> current
                current == "0" && value in "123456789" -> value
                else -> current + value
            }
            inputText.text = newText
        }
    }

    private fun calculateResult() {
        try {
            val expression = inputText.text.toString()
            val result = evaluateExpression(expression)
            
            // 显示历史记录动画
            historyText.apply {
                text = "$expression="
                animate().translationY(-50f).alpha(0.6f).setDuration(300).start()
            }
            
            // 处理结果显示
            inputText.text = when {
                result % 1 == 0.0 -> result.toLong().toString()  // 整数显示
                result.isInfinite() -> "无穷大"
                result.isNaN() -> "非法数字"
                else -> "%.8f".format(result).trimEnd('0').trimEnd('.')  // 去除多余小数位
            }
            isNewInput = true
        } catch (e: CalculationException) {
            inputText.text = when (e.message) {
                "除以零错误" -> "无法除以零"
                "括号不匹配" -> "括号不完整"
                else -> "输入错误"
            }
            isNewInput = true
        } catch (e: Exception) {
            inputText.text = "计算错误"
            isNewInput = true
        }
    }

    private fun evaluateExpression(expr: String): Double {
        return try {
            // 预处理输入表达式
            val processedExpr = expr
                .replace("÷", "/")   // 统一除号
                .replace("×", "*")   // 统一乘号
                .replace(" ", "")    // 移除空格
                .replace("()", "")   // 处理空括号

            // 使用exp4j构建表达式
            ExpressionBuilder(processedExpr)
                .build()
                .evaluate()
        } catch (e: ArithmeticException) {
            // 处理数学异常（如除以零）
            when (e.message) {
                "Division by zero!" -> throw CalculationException("除以零错误")
                else -> throw CalculationException("算术错误")
            }
        } catch (e: IllegalArgumentException) {
            // 处理无效表达式
            throw CalculationException("无效表达式")
        } catch (e: EmptyStackException) {
            // 处理括号不匹配等栈异常
            throw CalculationException("括号不匹配")
        }
    }
    private fun clearAll() {
        inputText.text = ""
        historyText.apply {
            text = ""
            translationY = 0f
            alpha = 1f
        }
        isNewInput = false
    }
    private fun deleteLastChar() {
        val currentText = inputText.text.toString()
        if (currentText.isNotEmpty()) {
            // 使用代码点处理Unicode字符
            val codePointCount = currentText.codePointCount(0, currentText.length)
            if (codePointCount > 0) {
                val newLength = currentText.offsetByCodePoints(0, codePointCount - 1)
                inputText.text = currentText.substring(0, newLength)
            }
        }
        
        // 当输入被清空时重置状态
        if (inputText.text.isEmpty()) resetCalculatorState()
    }

    // 单独的状态重置方法
    private fun resetCalculatorState() {
        isNewInput = false
        historyText.apply {
            text = ""
            animate().translationY(0f).alpha(1f).setDuration(150).start()
        }
    }
    // 自定义异常类
    class CalculationException(message: String) : Exception(message)

    private fun GridLayout.forEachChild(action: (View) -> Unit) {
        for (i in 0 until childCount) action(getChildAt(i))
    }
}
