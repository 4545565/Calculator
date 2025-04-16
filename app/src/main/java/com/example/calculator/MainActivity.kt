package com.example.calculator

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
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
import android.os.VibratorManager
import android.text.method.ScrollingMovementMethod
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private lateinit var inputText: TextView
    private lateinit var historyText: TextView
    private var isNewInput = false
    private var isResultDisplayed = false // 仅用于控制输入框是否显示结果

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 检测屏幕比例并控制旋转
        checkScreenRatioAndLockRotation()

        inputText = findViewById(R.id.inputText)
        historyText = findViewById(R.id.historyText)
        historyText.movementMethod = ScrollingMovementMethod()
        
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
                                
                                // 使用 Android 12+ 的触觉反馈 API
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                    val vibrator = vibratorManager.defaultVibrator
                                    
                                    // 使用预定义的高品质触觉效果（推荐）
                                    if (vibrator.areEffectsSupported(VibrationEffect.EFFECT_CLICK)[0] == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
                                        vibrator.vibrate(
                                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                        )
                                    }
                                    // 或使用自定义波形（精细控制振幅和频率）
                                    else {
                                        val waveform = VibrationEffect.createWaveform(
                                            longArrayOf(0, 30),  // 延迟 0ms → 震动 30ms
                                            intArrayOf(0, 255),  // 初始振幅 0 → 最大强度 255
                                            -1                   // 不重复
                                        )
                                        vibrator.vibrate(waveform)
                                    }
                                } else {
                                    // 旧版本兼容方案（简单短震动）
                                    @Suppress("DEPRECATION")
                                    (context.getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(50)
                                }
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
        // 如果当前显示的是计算结果，则清空输入框开始新表达式
        if (isResultDisplayed) {
            inputText.text = ""
            isResultDisplayed = false
        }
        
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
            
            // 将完整表达式和结果追加到历史记录
            historyText.append("\n$expression=$result") 
            
            // 自动滚动到历史记录底部
            historyText.post {
                val scrollY = historyText.layout.getLineTop(historyText.lineCount) - historyText.height
                historyText.scrollTo(0, max(scrollY, 0))
            }
            
            // 显示结果并标记状态
            inputText.text = result.toString()
            isResultDisplayed = true
        } catch (e: Exception) {
            inputText.text = "Error"
            isResultDisplayed = false
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
        historyText.text = "" // 只有明确按清除键才会清空历史
        isResultDisplayed = false
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
