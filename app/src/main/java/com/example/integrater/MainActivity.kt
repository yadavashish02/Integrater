package com.example.integrater

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlin.math.*
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val calculateButton:Button = findViewById(R.id.button_calculate)
        calculateButton.setOnClickListener { integrate() }
    }

    private fun integrate() {
        val limitAText:EditText = findViewById(R.id.text_limit_a)
        val limitBText:EditText = findViewById(R.id.text_limit_b)
        val accuracyNText:EditText = findViewById(R.id.text_accuracy_n)
        val answerText:TextView = findViewById(R.id.textView_answer)

        val a = limitAText.text.toString().toDouble()
        val b = limitBText.text.toString().toDouble()
        val n = accuracyNText.text.toString().toDouble()
        val answer = calculate(a,b,n)
        answerText.text = "Approximate answer is: $answer"
    }

    fun calculate(a:Double, b:Double, n:Double):Double {
        val h:Double = (b-a)/n
        var p:Double = a
        var z:Double
        var sum = 0.0
        do {
            p += h
            z = fx(p)
            sum +=z
        } while (p<(b-h))

        return (h/2)*(fx(a)+2*sum + fx(b))
    }

    fun fx(x:Double):Double {
        val functionText:EditText = findViewById(R.id.text_function)
        return ExpressionParser().evaluate(functionText.text.toString().replace("x", x.toString()))
    }
}

class DeprecateParser {

    private enum class Operators(val sign: Char) {
        PLUS('+'),
        MINUS('-'),
        MULTIPLY('*'),
        DIVISION('/'),
        POWER('^'),
        EXPONENTIAL('E');
    }

    private fun String.split(position: Int) =
        listOf(
            this.substring(0, position),
            this.substring(position + 1, this.length)
        )

    private fun extractNumber(numString: String) = numString.toDoubleOrNull()

    private fun isValue(expression: String): Boolean {
        val validChars = "1234567890.-"

        for (i in expression.indices) {
            val char = expression[i]
            if (char !in validChars) return false
            if (expression.count { it == '.' } > 1) return false
            if (char == '-' && i != 0) return false
        }
        return true
    }

    private fun String.lastIndexOf(char: Char): Int {
        var bOpen = 0
        var bClose = 0
        for (i in this.indices) {
            val currChar = this[i]

            when {
                currChar == char && bOpen == bClose -> return this.length - i - 1
                currChar == '(' -> bOpen++
                currChar == ')' -> bClose++
            }
        }
        return -1
    }

    private fun isOperator(operator: Operators, expression: String, position: Int): Boolean {
        if (operator == Operators.PLUS) {
            if (expression[position - 1] == 'E') {
                if (position >= 2) {
                    return false
                }
            } else {
                return true
            }
        } else if (operator == Operators.MINUS) {
            if (position == 0) {
                return false
            } else if (expression[position - 1] == 'E' && position >= 2) {
                return false
            } else {
                val prevOperator = expression[position - 1]
                for (legalOp in Operators.values()) {
                    if (prevOperator == legalOp.sign)
                        return false
                }
                println("returning operator minus")
                return true
            }
        }
        return true
    }

    private fun evaluateFunction(funString: String, value: Double): Double {
        return when (funString) {
            // Trigonometric
            "SIN", "sin", "Sin" -> sin(value)
            "COS", "cos", "Cos" -> cos(value)
            "TAN", "tan", "Tan" -> tan(value)
            "ASIN", "asin" -> asin(value)
            "ACOS", "acos" -> acos(value)
            "ATAN", "atan" -> atan(value)

            //arithmetic
            "LOG10", "log10", "Log10" -> log10(value)
            "LN", "Ln", "ln" -> ln(value)
            "SQRT", "sqrt", "Sqrt" -> sqrt(value)
            "EXP", "exp", "Exp" -> exp(value)

            //hyperbolic
            "SINH", "sinh", "Sinh" -> sinh(value)
            "COSH", "cosh", "Cosh" -> cosh(value)
            "TANH", "tanh", "Tanh" -> tanh(value)


            else -> throw
            ArithmeticException("Function cannot be determined $funString")
        }
    }

    private fun roundToPrecision(value: Double, precision: Int = 3): Double {
        val corrector = 10.0.pow(precision).toInt()
        return round(value * corrector) / corrector
    }

    fun evaluateExpression(expression: String, precision: Int = 3): Double {
        val res = evaluate(expression)
        return roundToPrecision(res, precision)
    }

    private fun evaluate(expression: String): Double {
        for (operator in Operators.values()) {
            /*
                find the operator from right side (last)
                for cases : 20/10/2
            */
            var position = expression.reversed().lastIndexOf(operator.sign)
            println("op ${operator.sign} pos $position")

            while (position > 0) {
                if (isOperator(operator, expression, position)) {
                    val partialExpressions = expression.split(position)
                    val left = partialExpressions[0]
                    val right = partialExpressions[1]

                    val value0 = evaluate(left)
                    val value1 = evaluate(right)

                    println(
                        """
                        left $left
                        right $right
                        valueLeft $value0
                        valueRight $value1
                    """.trimIndent()
                    )

                    val res = when (operator) {
                        Operators.PLUS -> value0 + value1
                        Operators.MINUS -> value0 - value1
                        Operators.DIVISION -> {
                            if (value1 == 0.0)
                                throw ArithmeticException("Divide By Zero")
                            value0 / value1
                        }
                        Operators.MULTIPLY -> value0 * value1
                        Operators.POWER -> value0.pow(value1)
                        Operators.EXPONENTIAL -> value0 * (10.0.pow(value1))
                    }
                    return res
                }
                if (position > 0) {
                    position =
                        expression.substring(0, position).reversed().lastIndexOf(operator.sign)
                }
            }
        }

        // Checking for function in expression
        val position = expression.lastIndexOf('(')
        println("Expression $expression pos $position ${expression.lastIndex}")

        if (position > 0 && expression.last() == ')') {
            val funString = expression.substring(0, position)
            val value = evaluate(expression.substring(position + 1, expression.lastIndex))
            val res = evaluateFunction(funString, value)
            return res
        }

        if (expression.startsWith('(') && expression.endsWith(')')) {
            return evaluate(expression.substring(1, expression.lastIndex))
        }
        println("Expression : $expression")
        return when {
            isValue(expression) -> extractNumber(expression) ?: Double.MIN_VALUE
            expression == "PI" -> PI
            expression == "E" || expression == "e" -> E
            else -> throw NumberFormatException()
        }
    }
}

class BadSyntaxException(msg: String = "Bad Syntax") : Exception(msg)

class DomainException(msg: String = "Domain Error") : Exception(msg)

class ImaginaryException(msg:String = "Imaginary Number not supported"):Exception(msg)

class BaseNotFoundException(msg: String = "Base Not Found"):Exception(msg)

class ExpressionParser {

    private val numStack = Stack<Double>()
    private val opStack = Stack<String>()

    var isDegrees = false
    private var logEnabled = false


    fun enableLog(status: Boolean) {
        logEnabled = status
    }

    fun evaluate(expression: String, precision: Int = 3): Double {
        val uExpression = convertToUExpression(expression)
        val res = evaluateExpression(uExpression)
        return roundToPrecision(res, precision)
    }

    private fun convertToUExpression(expression: String): String {
        val sb = StringBuilder()
        for (i in expression.indices) {
            val currChar = expression[i]
            if (currChar.toString() == NormalOperators.MINUS.sign) {
                if (i == 0) {
                    sb.append('u')
                } else {
                    val prevChar = expression[i - 1]
                    if (prevChar in "+*/^E(") {
                        sb.append('u')
                    } else {
                        sb.append(currChar)
                    }
                }
            } else {
                sb.append(currChar)
            }
        }
        return sb.toString()
    }

    private fun roundToPrecision(value: Double, precision: Int = 3): Double {
        val corrector = 10.0.pow(precision).toInt()
        var result = round(value * corrector) / corrector
        if (result == -0.0) {
            result = 0.0
        }
        return result
    }


    private fun computeNormalOperation(op: String) {
        try {
            when (op) {
                NormalOperators.PLUS.sign -> {
                    val num0 = numStack.pop()
                    val num1 = numStack.pop()
                    numStack.push(num1 + num0)
                }
                NormalOperators.MINUS.sign -> {
                    val num0 = numStack.pop()
                    val num1 = numStack.pop()
                    numStack.push(num1 - num0)
                }
                NormalOperators.MULTIPLY.sign -> {
                    val num0 = numStack.pop()
                    val num1 = numStack.pop()
                    numStack.push(num1 * num0)
                }
                NormalOperators.DIVISION.sign -> {
                    val num0 = numStack.pop()
                    val num1 = numStack.pop()
                    numStack.push(num1 / num0)
                }
                NormalOperators.POWER.sign -> {
                    val num0 = numStack.pop()
                    val num1 = numStack.pop()
                    numStack.push(num1.pow(num0))
                }
                NormalOperators.EXPONENTIAL.sign -> {
                    val num0 = numStack.pop()
                    val num1 = numStack.pop()
                    numStack.push(num1 * (10.0.pow(num0)))
                }
                NormalOperators.UNARY.sign -> {
                    val num0 = numStack.pop()
                    numStack.push(-1.0 * num0)
                }
            }
        } catch (es: IndexOutOfBoundsException) {
            clearStacks()
            throw BadSyntaxException()
        } catch (ae: ArithmeticException) {
            // division by zero
            clearStacks()
            throw Exception("Division by zero not possible")
        }
    }

    private fun evaluateExpression(expression: String): Double {
        var i = 0;
        val numString = StringBuilder()
        while (i < expression.length) {
            val currChar = expression[i]
            if (currChar in "0123456789.") {
                // check for implicit multiply
                if (i != 0 &&
                    (expression[i - 1] == ')' || expression[i - 1] == 'e' ||
                            (i >= 2 && expression.substring(i - 2, i) == "PI"))
                ) {
                    performSafePushToStack(numString, "*")
                }
                numString.append(currChar)
                i++

            } else if (currChar.toString() isIn NormalOperators.values() || currChar == '(') {

                if (currChar == '(') {
                    // check for implicit multiply
                    if (i != 0 && expression[i - 1].toString() notIn NormalOperators.values()) {
                        performSafePushToStack(numString, "*")
                    }
                    opStack.push("(")
                } else {
                    performSafePushToStack(numString, currChar.toString())
                }

                i++
            } else if (currChar == ')') {
                computeBracket(numString)
                i++
            } else if (currChar == '!') {
                performFactorial(numString)
                i++
            } else if (currChar == '%') {
                performPercentage(numString)
                i++
            } else if (i + 2 <= expression.length && expression.substring(i, i + 2) == "PI") {
                // check for implicit multiply
                if (i != 0 && expression[i - 1].toString() notIn NormalOperators.values()
                    && expression[i - 1] != '('
                ) {
                    performSafePushToStack(numString, "*")
                }
                numStack.push(PI)
                i += 2
            } else if (expression[i] == 'e' &&
                (i + 1 == expression.length || (i + 1) < expression.length && expression[i + 1] != 'x')
            ) {
                // check for implicit multiply
                if (i != 0 && expression[i - 1].toString() notIn NormalOperators.values()
                    && expression[i - 1] != '('
                ) {
                    performSafePushToStack(numString, "*")
                }
                numStack.push(E)
                i++
            } else {
                // check for implicit multiply
                if (i != 0 && expression[i - 1].toString() notIn NormalOperators.values()
                    && expression[i - 1] != '('
                ) {
                    performSafePushToStack(numString, "*")
                }
                val increment = pushFunctionalOperator(expression, i)
                i += increment
            }
        }

        if (numString.isNotEmpty()) {
            val number = numString.toString().toDouble()
            numStack.push(number)
            numString.clear()
        }
        while (!opStack.isEmpty()) {
            val op = opStack.pop()
            if (op isIn FunctionalOperators.values()) {
                clearStacks()
                throw BadSyntaxException()
            }
            computeNormalOperation(op)
        }
        if (logEnabled) {
            opStack.display()
            numStack.display()
        }
        return try {
            numStack.pop()
        } catch (ie: IndexOutOfBoundsException) {
            clearStacks()
            throw BadSyntaxException()
        }
    }


    private fun pushFunctionalOperator(
        expression: String,
        index: Int
    ): Int {
        for (func in FunctionalOperators.values()) {
            val funLength = func.func.length
            if ((index + funLength < expression.length) &&
                expression.substring(index, index + funLength) == func.func
            ) {
                if (func != FunctionalOperators.logx) {
                    opStack.push(func.func)
                    return funLength
                } else {
                    val logRegex = Regex("log[0123456789.]+\\(")
                    val found = logRegex.find(expression.substring(index, expression.length))
                    try {
                        val logxString = found!!.value
                        opStack.push(logxString)
                        return logxString.length
                    }catch (e: NullPointerException){
                        throw BaseNotFoundException()
                    }
                }
            }
        }
        clearStacks()
        throw Exception("Unsupported Operation at ${expression.substring(index, expression.length)}")
    }

    private fun performSafePushToStack(
        numString: StringBuilder,
        currOp: String
    ) {
        if (numString.isNotEmpty()) {
            val number = numString.toString().toDouble()
            numStack.push(number)
            numString.clear()

            if (opStack.isEmpty()) {
                opStack.push(currOp)
            } else {
                var prevOpPrecedence = getBinaryOperatorPrecedence(opStack.peek())
                val currOpPrecedence = getBinaryOperatorPrecedence(currOp)
                if (currOpPrecedence > prevOpPrecedence) {
                    opStack.push(currOp)
                } else {
                    while (currOpPrecedence <= prevOpPrecedence) {
                        val op = opStack.pop()
                        computeNormalOperation(op)
                        if (!opStack.isEmpty())
                            prevOpPrecedence = getBinaryOperatorPrecedence(opStack.peek())
                        else
                            break
                    }
                    opStack.push(currOp)
                }
            }
        } else if (!numStack.isEmpty() || currOp == NormalOperators.UNARY.sign) {
            opStack.push(currOp)
        }

    }

    private fun getBinaryOperatorPrecedence(currOp: String): Int {
        return when (currOp) {
            NormalOperators.PLUS.sign -> NormalOperators.PLUS.precedence
            NormalOperators.MINUS.sign -> NormalOperators.MINUS.precedence
            NormalOperators.MULTIPLY.sign -> NormalOperators.MULTIPLY.precedence
            NormalOperators.DIVISION.sign -> NormalOperators.DIVISION.precedence
            NormalOperators.POWER.sign -> NormalOperators.POWER.precedence
            NormalOperators.EXPONENTIAL.sign -> NormalOperators.EXPONENTIAL.precedence
            NormalOperators.UNARY.sign -> NormalOperators.UNARY.precedence
            else -> -1
        }
    }

    private fun computeBracket(numString: StringBuilder) {
        if (numString.isNotEmpty()) {
            val number = numString.toString().toDouble()
            numStack.push(number)
            numString.clear()
        }
        var operator = opStack.pop()
        while (operator != "(" && operator notIn FunctionalOperators.values()) {
            computeNormalOperation(operator)
            operator = opStack.pop()
        }
        if (operator isIn FunctionalOperators.values()) {
            computeFunction(operator)
        }
    }

    private fun computeFunction(func: String) {
        var num = numStack.pop()

        when (func) {
            FunctionalOperators.sin.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(sin(num))
            }
            FunctionalOperators.cos.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(cos(num))
            }
            FunctionalOperators.tan.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(tan(num))
            }
            FunctionalOperators.asin.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(asin(num))
            }
            FunctionalOperators.acos.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(acos(num))
            }
            FunctionalOperators.atan.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(atan(num))
            }
            FunctionalOperators.sinh.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(sinh(num))
            }
            FunctionalOperators.cosh.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(cosh(num))
            }
            FunctionalOperators.tanh.func -> {
                if (isDegrees) {
                    num = (num * PI) / 180
                }
                numStack.push(tanh(num))
            }
            FunctionalOperators.sqrt.func -> {
                if (num < 0) {
                    clearStacks()
                    throw ImaginaryException()
                }
                numStack.push(sqrt(num))
            }
            FunctionalOperators.exp.func -> numStack.push(exp(num))
            FunctionalOperators.ln.func -> numStack.push(ln(num))
            FunctionalOperators.log2.func -> numStack.push(log2(num))
            FunctionalOperators.log10.func -> numStack.push(log10(num))
            else -> {
                if (func.contains(FunctionalOperators.logx.func)) {
                    val base = func.substring(3, func.lastIndex).toDouble()
                    numStack.push(log(num, base))
                }
            }
        }
    }

    private fun performFactorial(numString: StringBuilder) {
        if (numString.isNotEmpty()) {
            val number = numString.toString().toDouble()
            numString.clear()
            if (number.isInt()) {
                val result = factorial(number)
                numStack.push(result)
                return
            } else {
                clearStacks()
                throw DomainException()
            }
        } else if (!numStack.isEmpty()) {
            val number = numStack.pop()
            if (number.isInt()) {
                var result = factorial(number.absoluteValue)
                if (number < 0) {
                    result = 0 - result
                }
                numStack.push(result)
                return
            } else {
                clearStacks()
                throw DomainException()
            }
        }
        clearStacks()
        throw DomainException()
    }


    private fun performPercentage(numString: StringBuilder) {
        if (numString.isNotEmpty()) {
            val number = numString.toString().toDouble()
            numString.clear()
            val result = number / 100
            numStack.push(result)
            return

        } else if (!numStack.isEmpty()) {
            val number = numStack.pop()
            val result = number / 100.0
            numStack.push(result)
            return
        }
        clearStacks()
        throw BadSyntaxException()
    }

    private fun Double.isInt() = this == floor(this)

    private fun clearStacks() {
        numStack.clear()
        opStack.clear()
    }

    private fun factorial(num: Double, output: Double = 1.0): Double {
        return if (num == 0.0) output
        else factorial(num - 1, output * num)
    }

}

enum class NormalOperators(val sign: String, val precedence: Int) {
    PLUS("+", 2),
    MINUS("-", 2),
    MULTIPLY("*", 3),
    DIVISION("/", 4),
    POWER("^", 5),
    EXPONENTIAL("E", 5),
    UNARY("u", 6);
}

enum class FunctionalOperators(val func: String) {
    sin("sin("),
    cos("cos("),
    tan("tan("),
    asin("asin("),
    acos("acos("),
    atan("atan("),
    sinh("sinh("),
    cosh("cosh("),
    tanh("tanh("),
    log2("log2("),
    log10("log10("),
    ln("ln("),
    logx("log"),
    sqrt("sqrt("),
    exp("exp(")

}

infix fun <T> String.isIn(operators: Array<T>): Boolean {

    for (operator in operators) {
        if (operator is NormalOperators) {
            if (this == operator.sign) {
                return true
            }
        } else if (operator is FunctionalOperators) {
            if (this.contains(operator.func)) {
                return true
            } else if (this.contains(FunctionalOperators.logx.func)) {
                return true
            }
        }
    }
    return false
}

infix fun <T> String.notIn(operators: Array<T>): Boolean {
    return !(this isIn operators)
}

class Stack<T> {
    private val stack = arrayListOf<T>()
    private var top = -1
    fun push(item: T) {
        stack.add(item)
        top++
    }

    fun pop(): T = stack.removeAt(top--)

    fun peek(): T = stack[top]

    fun isEmpty() = stack.isEmpty()

    fun size() = top + 1

    fun display() = println(stack)

    fun clear(){
        stack.clear()
        top = -1
    }
}
